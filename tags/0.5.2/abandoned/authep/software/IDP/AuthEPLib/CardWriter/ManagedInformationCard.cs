using System;
using System.Collections.Generic;
using System.Text;
using System.Xml;
using System.Collections;
using System.Security.Cryptography.Xml;
using System.Security.Cryptography.X509Certificates;
using System.Security.Cryptography;
using System.IO;
using System.ServiceModel;
using System.ServiceModel.Channels;
using System.Runtime.Serialization;
using System.ServiceModel.Description;

namespace nl.telin.authep.managedcardwriter
{
    /// <summary>
    /// <b>ManagedInformationCard</b> represents a Microsoft Windows Cardspace Managed card.
    /// </summary>
    public class ManagedInformationCard
    {

        string m_cardId = string.Empty;
        string m_cardName = string.Empty;
        string m_issuerName = string.Empty;
        string m_issuerId = string.Empty;
        DateTime m_issuedOn;
        DateTime m_expiresOn;
        bool m_requireAppliesTo;
        string m_language = string.Empty;
        string m_cardVersion = string.Empty;
        string m_mimeType = string.Empty;
        byte[ ] m_logo;
        string[ ] m_tokenTypes;
        List<ClaimInfo> m_supportedClaims = new List<ClaimInfo>();
        string m_privacyNoticeAt;
        EndpointAddress m_epr;
        string m_hint = string.Empty;
        string m_credentialIdentifier = string.Empty;
        DefaultValues.CardType m_cardType = DefaultValues.CardType.None;
        string m_mexUri = string.Empty;

        /// <summary>
        /// Create a new instance of <b>ManagedInformationCard</b>.
        /// </summary>
        /// <param name="cardType">The type of this card.</param>
        public ManagedInformationCard( DefaultValues.CardType cardType )
        {
            m_cardId = DefaultValues.CardId;
            m_cardName = DefaultValues.CardName;
            m_issuerId = DefaultValues.Issuer;
            m_issuerName = DefaultValues.IssuerName;
            m_issuedOn = DateTime.Now;
            m_expiresOn = DateTime.MaxValue;
            m_requireAppliesTo = false;
            m_language = DefaultValues.Language;
            m_cardVersion = DefaultValues.CardVersion;
            m_cardType = cardType;
            m_mexUri = DefaultValues.MexUri;
        }

        /// <summary>
        /// This card's type.
        /// </summary>
        public DefaultValues.CardType CardType
        {
            get { return m_cardType; }
        }
        /// <summary>
        /// This card's id.
        /// </summary>
        public string CardId
        {
            get { return m_cardId; }
            set { m_cardId = value; }
        }
        /// <summary>
        /// Name of the card.
        /// </summary>
        public string CardName
        {
            get { return m_cardName; }
            set { m_cardName = value; }
        }
        /// <summary>
        /// Name of this card's issuer
        /// </summary>
        public string IssuerName
        {
            get { return m_issuerName; }
            set { m_issuerName = value; }
        }
        /// <summary>
        /// Id of this card's issuer
        /// </summary>
        public string IssuerId
        {
            get { return m_issuerId; }
            set { m_issuerId = value; }
        }
        /// <summary>
        /// Byte's of this card's logo (120x80 72dpi 24 bit PNG image)
        /// </summary>
        public byte[ ] CardLogo
        {
            get { return m_logo; }
            set { m_logo = value; }
        }
        /// <summary>
        /// MimeType of this card.
        /// </summary>
        public string MimeType
        {
            get { return m_mimeType; }
            set { m_mimeType = value; }
        }
        /// <summary>
        /// RequireAppliesTo
        /// </summary>
        public bool RequireAppliesTo
        {
            get { return m_requireAppliesTo; }
            set { m_requireAppliesTo = value; }
        }
        /// <summary>
        /// All token types in this card.
        /// </summary>
        public string[ ] TokenTypes
        {
            get { return m_tokenTypes; }
            set { m_tokenTypes = value; }
        }
        /// <summary>
        /// Uri of the mex service.
        /// </summary>
        public string MexUri
        {
            get { return m_mexUri; }
            set { m_mexUri = value; }
        }
        /// <summary>
        /// A list of all claims supported by this card.
        /// </summary>
        public List<ClaimInfo> SupportedClaims
        {
            get { return m_supportedClaims; }
            set { m_supportedClaims = value; }
        }
        /// <summary>
        /// Link to where the privacy notice is located.
        /// </summary>
        public string PrivacyNoticeAt
        {
            get { return m_privacyNoticeAt; }
            set { m_privacyNoticeAt = value; }
        }
        /// <summary>
        /// Information card version.
        /// </summary>
        public string CardVersion
        {
            get { return m_cardVersion; }
            set { m_cardVersion = value; }
        }
        /// <summary>
        /// Hint for the user to remember his credentials.
        /// </summary>
        public string CredentialHint
        {
            get { return m_hint; }
            set { m_hint = value; }
        }
        /// <summary>
        /// Credential identifier.
        /// </summary>
        public string CredentialIdentifier
        {
            get { return m_credentialIdentifier; }
            set { m_credentialIdentifier = value; }
        }
        /// <summary>
        /// Sign this information card and write it to a file.
        /// </summary>
        /// <param name="filename">Path to where this card should be stored.</param>
        /// <param name="cert">Certificate to use for signing this card.</param>
        public void SerializeAndSign( string filename, X509Certificate2 cert )
        {

            MemoryStream stream = new MemoryStream();
            XmlWriter writer = XmlWriter.Create( stream );

            writer.WriteStartElement( XmlNames.WSIdentity.InfoCardElement, XmlNames.WSIdentity.Namespace );

            //
            // write the InformationCardReference element
            //
            writer.WriteAttributeString( XmlNames.Xml.Language, XmlNames.Xml.Namespace, m_language );
            writer.WriteStartElement( XmlNames.WSIdentity.InfoCardRefElement, XmlNames.WSIdentity.Namespace );
            writer.WriteElementString( XmlNames.WSIdentity.CardIdElement, XmlNames.WSIdentity.Namespace, m_cardId );
            writer.WriteElementString( XmlNames.WSIdentity.CardVersionElement, XmlNames.WSIdentity.Namespace, m_cardVersion );
            writer.WriteEndElement();

            //
            // card name
            //
            if( !String.IsNullOrEmpty( m_cardName ) )
            {
                writer.WriteStartElement( XmlNames.WSIdentity.CardNameElement, XmlNames.WSIdentity.Namespace );
                writer.WriteString( m_cardName );
                writer.WriteEndElement();
            }


            //
            // card image
            //
            if( null != m_logo && 0 != m_logo.Length )
            {
                writer.WriteStartElement( XmlNames.WSIdentity.CardImageElement, XmlNames.WSIdentity.Namespace );
                if( !String.IsNullOrEmpty( m_mimeType ) )
                {
                    writer.WriteAttributeString( XmlNames.WSIdentity.MimeTypeAttribute, m_mimeType );
                }
                string val = Convert.ToBase64String( m_logo );
                writer.WriteString( val );
                writer.WriteEndElement();
            }

            //
            // card issuer uri
            //
            writer.WriteStartElement( XmlNames.WSIdentity.IssuerElement, XmlNames.WSIdentity.Namespace );
            writer.WriteString( m_issuerId );
            writer.WriteEndElement();


            //
            // issue time
            //
            writer.WriteStartElement( XmlNames.WSIdentity.TimeIssuedElement, XmlNames.WSIdentity.Namespace );
            writer.WriteString( XmlConvert.ToString( m_issuedOn, XmlDateTimeSerializationMode.Utc ) );
            writer.WriteEndElement();

            //
            // expiry time
            //
            writer.WriteStartElement( XmlNames.WSIdentity.TimeExpiresElement, XmlNames.WSIdentity.Namespace );
            writer.WriteString( XmlConvert.ToString( m_expiresOn, XmlDateTimeSerializationMode.Utc ) );
            writer.WriteEndElement();

            //
            // Start the tokenservice list
            //
            writer.WriteStartElement( XmlNames.WSIdentity.TokenServiceListElement, XmlNames.WSIdentity.Namespace );

            EndpointAddressBuilder eprBuilder = new EndpointAddressBuilder();

            eprBuilder.Uri = new Uri( m_issuerId );

            eprBuilder.Identity = new X509CertificateEndpointIdentity( cert );

            if( null != m_mexUri )
            {

                MetadataReference mexRef = new MetadataReference();
                mexRef.Address = new EndpointAddress( m_mexUri );
                mexRef.AddressVersion = AddressingVersion.WSAddressing10;

                MetadataSection mexSection = new MetadataSection();
                mexSection.Metadata = mexRef;

                MetadataSet mexSet = new MetadataSet();
                mexSet.MetadataSections.Add( mexSection );


                MemoryStream memStream = new MemoryStream();

                XmlTextWriter writer1 = new XmlTextWriter( memStream, System.Text.Encoding.UTF8 );

                mexSet.WriteTo( writer1 );

                writer1.Flush();

                memStream.Seek( 0, SeekOrigin.Begin );

                XmlDictionaryReader reader = XmlDictionaryReader.CreateTextReader( memStream, XmlDictionaryReaderQuotas.Max );

                eprBuilder.SetMetadataReader( reader );

            }

            m_epr = eprBuilder.ToEndpointAddress();



            writer.WriteStartElement( XmlNames.WSIdentity.TokenServiceElement, XmlNames.WSIdentity.Namespace );

            //
            // Write the EndPointReference
            //
            m_epr.WriteTo( AddressingVersion.WSAddressing10, writer );

            //
            // Write the UserCredential Element
            //
            writer.WriteStartElement( XmlNames.WSIdentity.UserCredentialElement, XmlNames.WSIdentity.Namespace );

            //
            // Write the hint
            //
            if( !String.IsNullOrEmpty( m_hint ) )
            {
                writer.WriteStartElement( XmlNames.WSIdentity.DisplayCredentialHintElement, XmlNames.WSIdentity.Namespace );
                writer.WriteString( m_hint );
                writer.WriteEndElement();
            }

            switch( m_cardType )
            {
                case DefaultValues.CardType.UserNamePassword:
                    writer.WriteStartElement( XmlNames.WSIdentity.UserNamePasswordCredentialElement, XmlNames.WSIdentity.Namespace );
                    if( !string.IsNullOrEmpty( m_credentialIdentifier ) )
                    {
                        writer.WriteStartElement( XmlNames.WSIdentity.UserNameElement, XmlNames.WSIdentity.Namespace );
                        writer.WriteString( m_credentialIdentifier );
                        writer.WriteEndElement();
                    }
                    writer.WriteEndElement();
                    break;
                case DefaultValues.CardType.KerberosAuth:
                    writer.WriteStartElement( XmlNames.WSIdentity.KerberosV5CredentialElement, XmlNames.WSIdentity.Namespace );
                    writer.WriteEndElement();
                    break;
                case DefaultValues.CardType.SelfIssuedAuth:
                    writer.WriteStartElement( XmlNames.WSIdentity.SelfIssuedCredentialElement, XmlNames.WSIdentity.Namespace );
                    if( !string.IsNullOrEmpty( m_credentialIdentifier ) )
                    {
                        writer.WriteStartElement( XmlNames.WSIdentity.PrivatePersonalIdentifierElement, XmlNames.WSIdentity.Namespace );
                        writer.WriteString( m_credentialIdentifier );
                        writer.WriteEndElement();
                    }
                    else
                    {
                        throw new InvalidDataException( "No PPID was specified" );
                    }
                    writer.WriteEndElement();
                    break;
                case DefaultValues.CardType.SmartCard:
                    writer.WriteStartElement( XmlNames.WSIdentity.X509V3CredentialElement, XmlNames.WSIdentity.Namespace );

                    writer.WriteStartElement( XmlNames.XmlDSig.X509DataElement, XmlNames.XmlDSig.Namespace );
                    if( !string.IsNullOrEmpty( m_credentialIdentifier ) )
                    {
                        writer.WriteStartElement( XmlNames.WSSecurityExt.KeyIdentifierElement, XmlNames.WSSecurityExt.Namespace );
                        writer.WriteAttributeString( XmlNames.WSSecurityExt.ValueTypeAttribute,
                                         null,
                                         XmlNames.WSSecurityExt.Sha1ThumbrpintKeyTypeValue );
                        writer.WriteString( m_credentialIdentifier );
                        writer.WriteEndElement();
                    }
                    else
                    {
                        throw new InvalidDataException( "No thumbprint was specified" );
                    }
                    writer.WriteEndElement();
                    writer.WriteEndElement();
                    break;
                default:
                    break;
            }
            writer.WriteEndElement(); //end of user credential
            writer.WriteEndElement(); // end of tokenservice
            writer.WriteEndElement(); //end of tokenservice list
            //
            // tokentypes
            //
            writer.WriteStartElement( XmlNames.WSIdentity.SupportedTokenTypeListElement, XmlNames.WSIdentity.Namespace );
            foreach( string type in m_tokenTypes )
            {
                writer.WriteElementString( XmlNames.WSTrust.TokenType,
                                           XmlNames.WSTrust.Namespace,
                                           type );
            }
            writer.WriteEndElement();

            //
            // claims
            //
            writer.WriteStartElement( XmlNames.WSIdentity.SupportedClaimTypeListElement, XmlNames.WSIdentity.Namespace );
            foreach( ClaimInfo clm in m_supportedClaims )
            {

                writer.WriteStartElement( XmlNames.WSIdentity.SupportedClaimTypeElement, XmlNames.WSIdentity.Namespace );
                writer.WriteAttributeString( XmlNames.WSIdentity.UriAttribute, clm.Id );

                
                if( !String.IsNullOrEmpty( clm.DisplayTag ) )
                {
                    writer.WriteElementString( XmlNames.WSIdentity.DisplayTagElement,
                                                   XmlNames.WSIdentity.Namespace,
                                                   clm.DisplayTag );
                }

                if( !String.IsNullOrEmpty( clm.Description ) )
                {
                    writer.WriteElementString( XmlNames.WSIdentity.DescriptionElement,
                                               XmlNames.WSIdentity.Namespace,
                                               clm.Description );
                }
                writer.WriteEndElement();

            }
            writer.WriteEndElement();

            //
            // RequireAppliesTo
            //
            if( m_requireAppliesTo )
            {
                writer.WriteElementString( XmlNames.WSIdentity.RequireAppliesToElement, XmlNames.WSIdentity.Namespace, null );
            }

            //
            // Privacy Notice
            //
            if( !String.IsNullOrEmpty( m_privacyNoticeAt ) )
            {
                writer.WriteStartElement( XmlNames.WSIdentity.PrivacyNoticeAtElement, XmlNames.WSIdentity.Namespace );
                writer.WriteString( m_privacyNoticeAt );
                writer.WriteEndElement();
            }
            writer.WriteEndElement();

            writer.Close();



            //
            // Sign the xml content
            //
            stream.Position = 0;

            XmlDocument doc = new XmlDocument();
            doc.PreserveWhitespace = false;
            doc.Load( stream );

            SignedXml signed = new SignedXml();
            signed.SigningKey = cert.PrivateKey;
            signed.Signature.SignedInfo.CanonicalizationMethod
                = SignedXml.XmlDsigExcC14NTransformUrl;

            Reference reference = new Reference();
            reference.Uri = "#_Object_InfoCard";
            reference.AddTransform(
                        new XmlDsigExcC14NTransform() );
            signed.AddReference( reference );


            KeyInfo info = new KeyInfo();
            KeyInfoX509Data data = new KeyInfoX509Data( cert,
                X509IncludeOption.WholeChain );
            info.AddClause( data );

            signed.KeyInfo = info;
            DataObject cardData = new DataObject( "_Object_InfoCard", null, null, doc.DocumentElement );
            signed.AddObject( cardData );

            signed.ComputeSignature();

            XmlElement e = signed.GetXml();

            XmlTextWriter fileWriter = new XmlTextWriter( filename, Encoding.UTF8 );
            e.WriteTo( fileWriter );
            fileWriter.Flush();
            fileWriter.Close();
        }

        /// <summary>
        /// Check if all needed information for creating this card is present.
        /// </summary>
        /// <returns>True - all information is present or else false.</returns>
        public bool IsComplete()
        {
            return ( !String.IsNullOrEmpty( m_cardVersion )
                  && !String.IsNullOrEmpty( m_cardId )
                  && !String.IsNullOrEmpty( m_issuerName )
                  && !String.IsNullOrEmpty( m_issuerId )
                  && !String.IsNullOrEmpty( m_mexUri )
                  && ( null != m_issuedOn )
                  && ( null != m_expiresOn )
                  && ( null != m_supportedClaims && m_supportedClaims.Count > 0 )
                  && ( null != m_tokenTypes && m_tokenTypes.Length > 0 )
                  && !( ( m_cardType == DefaultValues.CardType.SelfIssuedAuth || m_cardType == DefaultValues.CardType.SmartCard ) && String.IsNullOrEmpty( m_credentialIdentifier ) ) );

        }

    }






}
