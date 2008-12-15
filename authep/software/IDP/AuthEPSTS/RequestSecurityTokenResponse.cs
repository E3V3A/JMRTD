using System;
using System.Collections.Generic;
using System.Text;
using System.ServiceModel.Channels;
using System.ServiceModel.Security;
using System.Xml;
using System.IdentityModel.Tokens;
using System.IdentityModel.Claims;
using System.Security.Cryptography.X509Certificates;
using System.Runtime.Serialization;
using System.IdentityModel.Selectors;
using System.Configuration;
using System.IO;
using System.Security.Cryptography;

using RST = nl.telin.authep.sts.RequestSecurityToken;

using nl.telin.authep.lib;
using nl.telin.authep.network;

namespace nl.telin.authep.sts
{
    /// <summary>
    /// Represents a response to a security token request. 
    /// </summary>
    public class RequestSecurityTokenResponse : BodyWriter
    {
        string context;
        SecurityKeyIdentifier useKey;
        string keyType;
        string ppid;
        MRZInfo mrz;
        List<string> claimTypes;

        /// <summary>
        /// Initializes a new instance of the <b>RequestSecurityTokenResponse</b> using the specified
        /// security token request, ppid and MRZ information.
        /// </summary>
        /// <param name="rst"><b>RST</b> to which this instance is a response (RSTR)</param>
        /// <param name="ppid">Identifier of the person requesting the token</param>
        /// <param name="mrz">MRZ information used for constructing this RSTR</param>
        public RequestSecurityTokenResponse(RST rst, string ppid, MRZInfo mrz)
            :base(false)
        {
            this.context = rst.Context;
            this.useKey = rst.UseKey;
            this.keyType = rst.KeyType;
            this.claimTypes = rst.ClaimTypes;
            this.ppid = ppid;
            this.mrz = mrz;
        }

        /// <summary>
        /// Returns the SAML attributes to insert into the token
        /// </summary>
        /// <returns>SAML attributes</returns>
        protected List<SamlAttribute> GetTokenAttributes()
        {
            List<SamlAttribute> result = new List<SamlAttribute>();

            string givenName = "";
            foreach (string s in mrz.getSecondaryIdentifiers())
                givenName += s + " ";
            
            /*
            result.Add(new SamlAttribute(new Claim(ClaimTypes.GivenName, givenName, Rights.PossessProperty)));
            result.Add(new SamlAttribute(new Claim(ClaimTypes.Surname, mrz.getPrimaryIdentifier(), Rights.PossessProperty)));
            result.Add(new SamlAttribute(new Claim(ClaimTypes.Locality, mrz.getNationality().getName(), Rights.PossessProperty)));
            result.Add(new SamlAttribute(new Claim(ClaimTypes.DateOfBirth, mrz.getDateOfBirth().ToShortDateString(), Rights.PossessProperty)));
            result.Add(new SamlAttribute(new Claim(ClaimTypes.Gender, mrz.getGender().ToString(), Rights.PossessProperty)));
            result.Add(new SamlAttribute(new Claim("http://schemas.authep.nl/claims/documentmumber", mrz.getDocumentNumber(), Rights.PossessProperty)));
            result.Add(new SamlAttribute(new Claim("http://schemas.authep.nl/claims/personalnumber", mrz.getPersonalNumber(), Rights.PossessProperty)));
            result.Add(new SamlAttribute(new Claim("http://schemas.authep.nl/claims/issuingstate", mrz.getIssuingState().getName(), Rights.PossessProperty)));
            result.Add(new SamlAttribute(new Claim("http://schemas.authep.nl/claims/dateofexpiry", mrz.getDateOfExpiry().ToShortDateString(), Rights.PossessProperty)));
            result.Add(new SamlAttribute(new Claim(ClaimTypes.PPID, ppid, Rights.PossessProperty)));
            */
            foreach (string claimType in claimTypes)
            {
                if (claimType.Equals(ClaimTypes.GivenName))
                    result.Add(new SamlAttribute(new Claim(ClaimTypes.GivenName, givenName, Rights.PossessProperty)));
                else if (claimType.Equals(ClaimTypes.Surname))
                    result.Add(new SamlAttribute(new Claim(ClaimTypes.Surname, mrz.getPrimaryIdentifier(), Rights.PossessProperty)));
                else if (claimType.Equals(ClaimTypes.Locality))
                    result.Add(new SamlAttribute(new Claim(ClaimTypes.Locality, mrz.getNationality().getName(), Rights.PossessProperty)));
                else if (claimType.Equals(ClaimTypes.DateOfBirth))
                    result.Add(new SamlAttribute(new Claim(ClaimTypes.DateOfBirth, mrz.getDateOfBirth().Ticks.ToString(), Rights.PossessProperty)));
                else if (claimType.Equals(ClaimTypes.Gender))
                    result.Add(new SamlAttribute(new Claim(ClaimTypes.Gender, mrz.getGender().ToString(), Rights.PossessProperty)));
                else if (claimType.Equals("http://schemas.authep.nl/claims/documentmumber"))
                    result.Add(new SamlAttribute(new Claim("http://schemas.authep.nl/claims/documentmumber", mrz.getDocumentNumber(), Rights.PossessProperty)));
                else if (claimType.Equals("http://schemas.authep.nl/claims/personalnumber"))
                    result.Add(new SamlAttribute(new Claim("http://schemas.authep.nl/claims/personalnumber", mrz.getPersonalNumber(), Rights.PossessProperty)));
                else if (claimType.Equals("http://schemas.authep.nl/claims/issuingstate"))
                    result.Add(new SamlAttribute(new Claim("http://schemas.authep.nl/claims/issuingstate", mrz.getIssuingState().getName(), Rights.PossessProperty)));
                else if (claimType.Equals("http://schemas.authep.nl/claims/dateofexpiry"))
                    result.Add(new SamlAttribute(new Claim("http://schemas.authep.nl/claims/dateofexpiry", mrz.getDateOfExpiry().Ticks, Rights.PossessProperty)));
                else if (claimType.Equals(ClaimTypes.PPID))
                    result.Add(new SamlAttribute(new Claim(ClaimTypes.PPID, ppid, Rights.PossessProperty)));
            }


            return result;
        }

        /// <summary>
        /// Build the contents of the SAML token
        /// </summary>
        /// <param name="writer"><b>XmlDictionaryWriter</b> to write the contents of this token to</param>
        protected override void OnWriteBodyContents(XmlDictionaryWriter writer)
        {
            // Subject
            SamlSubject subject = new SamlSubject();
            if ( this.useKey != null )
            {
                // Add the key and the Holder-Of-Key confirmation method
                subject.KeyIdentifier = this.useKey;
                subject.ConfirmationMethods.Add( SamlConstants.HolderOfKey );
            }
            else
            {
                // This is a bearer token
                subject.ConfirmationMethods.Add( SamlConstants.SenderVouches );
            }


            // Attributes, statements, conditions, and assertions
            List<SamlStatement> statements = new List<SamlStatement>();
            List<SamlAttribute> attributes = GetTokenAttributes();
            
            
            statements.Add(new SamlAuthenticationStatement(subject, Constants.Saml.AuthenticationMethods.Unspecified, DateTime.Now, null, null, null));
            statements.Add(new SamlAttributeStatement(subject, attributes));
            SamlConditions conditions = new SamlConditions(DateTime.Now, (DateTime.Now + TimeSpan.FromHours(8.0)));
            SamlAssertion assertion = new SamlAssertion("uuid-" + Guid.NewGuid(), Program.Issuer, DateTime.Now, conditions, null, statements);

            // Build the signing token
            SecurityToken signingToken = new X509SecurityToken(Program.SigningCertificate);
            SecurityKeyIdentifier keyIdentifier = new SecurityKeyIdentifier(signingToken.CreateKeyIdentifierClause<X509RawDataKeyIdentifierClause>());
            SigningCredentials signingCredentials = new SigningCredentials(signingToken.SecurityKeys[0], SecurityAlgorithms.RsaSha1Signature, SecurityAlgorithms.Sha1Digest, keyIdentifier);
            assertion.SigningCredentials = signingCredentials;

            // Build the SAML token
            SamlSecurityToken token = new SamlSecurityToken(assertion);
            SecurityKeyIdentifierClause attachedReference = token.CreateKeyIdentifierClause<SamlAssertionKeyIdentifierClause>();
            SecurityKeyIdentifierClause unattachedReference = token.CreateKeyIdentifierClause<SamlAssertionKeyIdentifierClause>();

            //
            // Write the XML
            //
            //writer = XmlDictionaryWriter.CreateTextWriter(File.CreateText("output.xml").BaseStream);

            // RSTR
            writer.WriteStartElement(Constants.WSTrust.NamespaceUri.Prefix, Constants.WSTrust.Elements.RequestSecurityTokenResponse, Constants.WSTrust.NamespaceUri.Uri);
            if (context != null)
            {
                writer.WriteAttributeString(Constants.WSTrust.Attributes.Context, context);
            }

            // TokenType
            writer.WriteElementString(Constants.WSTrust.NamespaceUri.Prefix, Constants.WSTrust.Elements.TokenType, Constants.WSTrust.NamespaceUri.Uri, Constants.WSTrust.TokenTypes.Saml10Assertion);

            // RequestedSecurityToken (the SAML token)
            SecurityTokenSerializer tokenSerializer = new WSSecurityTokenSerializer();
            writer.WriteStartElement(Constants.WSTrust.NamespaceUri.Prefix, Constants.WSTrust.Elements.RequestedSecurityToken, Constants.WSTrust.NamespaceUri.Uri);
            tokenSerializer.WriteToken(writer, token);
            writer.WriteEndElement();

            // RequestedAttachedReference
            writer.WriteStartElement(Constants.WSTrust.NamespaceUri.Prefix, Constants.WSTrust.Elements.RequestedAttachedReference, Constants.WSTrust.NamespaceUri.Uri);
            tokenSerializer.WriteKeyIdentifierClause(writer, attachedReference);
            writer.WriteEndElement();

            // RequestedUnattachedReference
            writer.WriteStartElement(Constants.WSTrust.NamespaceUri.Prefix, Constants.WSTrust.Elements.RequestedUnattachedReference, Constants.WSTrust.NamespaceUri.Uri);
            tokenSerializer.WriteKeyIdentifierClause(writer, unattachedReference);
            writer.WriteEndElement();

            // RequestedDisplayToken (display token)
            string displayTokenNS = "http://schemas.xmlsoap.org/ws/2005/05/identity";
            writer.WriteStartElement("wsid", "RequestedDisplayToken", displayTokenNS);
            writer.WriteStartElement("wsid", "DisplayToken", displayTokenNS);
            foreach (SamlAttribute attribute in attributes)
            {
                writer.WriteStartElement("wsid", "DisplayClaim", displayTokenNS);
                writer.WriteAttributeString("Uri", attribute.Namespace + "/" + attribute.Name);
                writer.WriteStartElement("wsid", "DisplayTag", displayTokenNS);
                    writer.WriteValue(attribute.Name);
                    writer.WriteEndElement();
                    writer.WriteStartElement("wsid", "Description", displayTokenNS);
                    writer.WriteValue(attribute.Namespace + "/" + attribute.Name);
                    writer.WriteEndElement();
                    foreach (string attributeValue in attribute.AttributeValues)
                    {
                        writer.WriteStartElement("wsid", "DisplayValue", displayTokenNS);
                        writer.WriteValue(attributeValue);
                        writer.WriteEndElement();
                    }
                writer.WriteEndElement();
            }
            writer.WriteEndElement();
            writer.WriteEndElement();

            // RSTR End
            writer.WriteEndElement();
            
            //writer.Close();
        }
    }
}
