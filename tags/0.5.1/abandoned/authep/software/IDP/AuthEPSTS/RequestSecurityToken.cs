using System;
using System.Xml;
using System.Xml.Serialization;
using System.Collections.Generic;
using System.Runtime.Serialization;
using System.IdentityModel.Selectors;
using System.IdentityModel.Tokens;
using System.Security.Cryptography;
using System.ServiceModel.Security;


namespace nl.telin.authep.sts
{
    /// <summary>
    /// Represents a security token request. 
    /// </summary>
    public class RequestSecurityToken
    {
        // RST attributes
        private string context;

        // RST properties
        private string requestType;
        private string keyType;
        private string keySize;
        private string encryptWith;
        private string signWith;
        private List<string> claimTypes = new List<string>();
        private SecurityKeyIdentifier useKey;

        // routines and state to help process the request
        private delegate void elementHandler(XmlReader reader);
        private Dictionary<string, elementHandler> wsTrustElements = new Dictionary<string, elementHandler>();
        private Dictionary<string, elementHandler> wsSecurityPolicyElements = new Dictionary<string, elementHandler>();
        private Dictionary<string, elementHandler> wsIdentityElements = new Dictionary<string, elementHandler>();
        private static Dictionary<string, string> expectedValues = new Dictionary<string, string>();

        static RequestSecurityToken()
        {
            expectedValues.Add(Constants.WSTrust.Elements.RequestType, Constants.WSTrust.RequestTypes.Issue);
            expectedValues.Add(Constants.WSTrust.Elements.TokenType, Constants.WSTrust.TokenTypes.Saml10Assertion);
            expectedValues.Add(Constants.WSTrust.Elements.CanonicalizationAlgorithm, Constants.XmlCanonicalization.Algorithms.ExcC14N);
            expectedValues.Add(Constants.WSTrust.Elements.KeySize, Constants.Numbers.TwoThousandFortyEight);
            expectedValues.Add(Constants.WSTrust.Elements.EncryptWith, Constants.XmlEnc.Algorithms.RsaOaepMgf1P);
            expectedValues.Add(Constants.WSTrust.Elements.SignWith, Constants.XmlDsig.Algorithms.RsaSha1);
        }

        /// <summary>
        /// Initializes a new instance of the <b>RequestSecurityToken</b> using the specified Xml Dictionary Reader
        /// </summary>
        /// <param name="reader">A <b>XmlDictionaryReader</b> that is used to freate the new <see cref="RequestSecurityToken"/></param>
        public RequestSecurityToken(XmlDictionaryReader reader)
        {
            // Parse the message
            ParseRST(reader);
            //string t = reader.ReadInnerXml();
            

            // Do intradependent parameter checks
            if (this.requestType == null)
            {
                //
                // This is a required parameter, per the WS-Trust February 2005
                // specification. However, CardSpace.v1 does not always emit this value.
                //
                // throw new InvalidRequestFaultException();
            }

            // This sample handles a very narrow profile of requests -- check for these.
            if (((this.KeyType == Constants.WSIdentity.KeyTypes.NoProofKey) &&
                  (this.keySize == null &&
                  this.encryptWith == null &&
                  this.signWith == null))

                ||

                 ((this.KeyType == Constants.WSTrust.KeyTypes.Asymmetric) &&
                  (this.keySize != null &&
                   this.encryptWith != null &&
                   this.signWith != null)))
            {
                // these are the accepted combinations for this sample
                ;
            }
            else
            {
                throw new NotSupportedException();
            }
        }

        /// <summary>
        /// Get the context of this RST
        /// </summary>
        public string Context
        {
            get { return context; }
        }

        /// <summary>
        /// Get the keytype of this RST
        /// </summary>
        public string KeyType
        {
            get { return keyType; }
        }

        /// <summary>
        /// Get the <b>SecurityKeyIdentifier</b> used by this RST
        /// </summary>
        public SecurityKeyIdentifier UseKey
        {
            get { return useKey; }
        }

        /// <summary>
        /// Get a list of all claims types that are requested by this RST
        /// </summary>
        public List<string> ClaimTypes 
        { 
            get { return claimTypes; } 
        }

        private static string ConsumeSimpleString(XmlReader reader)
        {
            // No attributes are handled
            ReadAttribute(reader, null, null);

            // Extract the element content and verify it is expected value, if applicable
            string presentedValue = reader.ReadString();
            string expectedValue;
            if (expectedValues.TryGetValue(reader.LocalName, out expectedValue))
            {
                if ((presentedValue == null) ||
                     (string.Compare(expectedValue, presentedValue) != 0))
                {
                    throw new NotSupportedException("Unexpected value");
                }
            }
            reader.ReadEndElement();
            return presentedValue;
        }

        private static void IgnoreElement(XmlReader reader)
        {
            reader.Skip();
        }

        /// <summary>
        /// Get a specific attribute by using a provided Xml Reader.
        /// </summary>
        /// <param name="reader"><b>XmlReader</b> used for reading a xml source</param>
        /// <param name="NamespaceUri">Namespace to look in</param>
        /// <param name="AttributeName">Name of the attribute to search fro</param>
        /// <returns></returns>
        public static string ReadAttribute(XmlReader reader, string NamespaceUri, string AttributeName)
        {
            string attributeString = null;

            if (null == reader)
                throw new ArgumentNullException("reader");

            if (!reader.IsStartElement())
            {
                throw new XmlException("ReadElement must be called with the reader positioned on a start element.");
            }

            if (reader.HasAttributes)
            {
                while (reader.MoveToNextAttribute())
                {
                    if (AttributeName != null &&
                        reader.LocalName == AttributeName &&
                        reader.NamespaceURI == NamespaceUri )
                    {
                        attributeString = reader.Value;
                    }
                    else
                    {
                        if (reader.Prefix != "xmlns" && reader.LocalName != "xmlns")
                            throw new XmlException("Unrecognized attribute: " + reader.LocalName + " namespace: " + reader.NamespaceURI);
                    }
                }

                reader.MoveToElement(); // Leave the reader on the start element
            }

            return attributeString;
        }

        private void CanonicalizationAlgorithmHandler(XmlReader reader)
        {
            // the value is not referenced later, no need to store, but still
            // read to validate expected value
            ConsumeSimpleString(reader);
        }

        private void EncryptWithHandler(XmlReader reader)
        {
            this.encryptWith = ConsumeSimpleString(reader);
        }

        private void InitializeHandlers()
        {
            // Elements that are accepted and handled
            wsTrustElements.Add(Constants.WSTrust.Elements.RequestType, this.RequestTypeHandler);
            wsTrustElements.Add(Constants.WSTrust.Elements.TokenType, this.TokenTypeHandler);
            wsTrustElements.Add(Constants.WSTrust.Elements.CanonicalizationAlgorithm, this.CanonicalizationAlgorithmHandler);
            wsTrustElements.Add(Constants.WSTrust.Elements.KeyType, this.KeyTypeHandler);
            wsTrustElements.Add(Constants.WSTrust.Elements.KeySize, this.KeySizeHandler);
            wsTrustElements.Add(Constants.WSTrust.Elements.EncryptWith, this.EncryptWithHandler);
            wsTrustElements.Add(Constants.WSTrust.Elements.SignWith, this.SignWithHandler);
            wsTrustElements.Add(Constants.WSTrust.Elements.UseKey, this.UseKeyHandler);
            wsTrustElements.Add(Constants.WSTrust.Elements.Claims, this.ClaimHandler);

            // Elements that are accepted, but ignored
            //wsTrustElements.Add(Constants.WSTrust.Elements.Claims, IgnoreElement);
            wsTrustElements.Add(Constants.WSTrust.Elements.EncryptionAlgorithm, IgnoreElement);
            wsIdentityElements.Add(Constants.WSIdentity.Elements.InformationCardReference, IgnoreElement);
            wsIdentityElements.Add(Constants.WSIdentity.Elements.RequestDisplayToken, IgnoreElement);
            wsIdentityElements.Add(Constants.WSIdentity.Elements.ClientPseudonym, IgnoreElement);
        }

        private void ClaimHandler(XmlReader reader)
        {
            reader.Read();
            while (reader.NodeType != XmlNodeType.EndElement && reader.LocalName == "ClaimType")
            {
                while (reader.MoveToNextAttribute())
                {
                    if (reader.LocalName == "Uri")
                        claimTypes.Add(reader.Value);
                }
                reader.Read();
            }
        }

        private void RequestTypeHandler(XmlReader reader)
        {
            this.requestType = ConsumeSimpleString(reader);
        }

        private void TokenTypeHandler(XmlReader reader)
        {
            // the value is not referenced later, no need to store, but still
            // read to validate expected value
            ConsumeSimpleString(reader);
        }

        private void KeySizeHandler(XmlReader reader)
        {
            this.keySize = ConsumeSimpleString(reader);
        }

        private void KeyTypeHandler(XmlReader reader)
        {
            this.keyType = ConsumeSimpleString(reader);
        }

        private void ParseRST(XmlReader reader)
        {
            // Initialize routines to help parse the message
            InitializeHandlers();

            // Verify the message type and extract the context, if any
            if (reader.LocalName != Constants.WSTrust.Elements.RequestSecurityToken || reader.NamespaceURI != Constants.WSTrust.NamespaceUri.Uri)
            {
                throw new XmlException("Incorrect Element");
            }
            context = ReadAttribute(reader, "", Constants.WSTrust.Attributes.Context);
            reader.Read();

            // Move from node to node until done
            while (!(reader.EOF))
            {
                switch (reader.NodeType)
                {
                    case XmlNodeType.Element:

                        // Handle the fixed values and verify values are as expected for
                        // the sample scenarios.
                        elementHandler handler;
                        if ((reader.NamespaceURI == Constants.WSTrust.NamespaceUri.Uri &&
                             wsTrustElements.TryGetValue(reader.LocalName, out handler)) ||

                            (reader.NamespaceURI == Constants.WSIdentity.NamespaceUri.Uri &&
                             wsIdentityElements.TryGetValue(reader.LocalName, out handler)))
                        {
                            // this is an element that we expect and handle
                            handler(reader);
                        }
                        else
                        {
                            throw new XmlException("Unexpected element");
                        }
                        break;

                    case XmlNodeType.Whitespace:
                    case XmlNodeType.SignificantWhitespace:
                    case XmlNodeType.EndElement:
                    case XmlNodeType.Comment:
                        reader.Read();
                        break;

                    case XmlNodeType.Text:
                    case XmlNodeType.CDATA:
                    case XmlNodeType.ProcessingInstruction:
                    case XmlNodeType.Document:
                        throw new XmlException("Unexpected element");
                }
            }
        }

        private void SignWithHandler(XmlReader reader)
        {
            this.signWith= ConsumeSimpleString(reader);
        }

        private void UseKeyHandler(XmlReader reader)
        {
            // Ensure there are no attributes, since this code doens't handle any
            ReadAttribute(reader, null, null);
            // Go to the next element
            reader.ReadStartElement();

            if ( !reader.IsEmptyElement )
            {
                if (reader.IsStartElement())
                {

                    this.useKey = WSSecurityTokenSerializer.DefaultInstance.ReadKeyIdentifier(reader);
                }
                reader.MoveToContent();
                    reader.ReadEndElement();
            }
            else
            {
                reader.Read();
            }
        }            
    }
}

