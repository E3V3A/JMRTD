//-----------------------------------------------------------------------------
// Copyright (c) Microsoft Corporation.  All rights reserved.
//-----------------------------------------------------------------------------
using System;
using System.Collections.Generic;
using System.Text;
using System.Xml;

namespace nl.telin.authep.managedcardwriter
{
    /// <summary>
    /// Contains xml declarations which are used by the managed card writer.
    /// </summary>
    public sealed class XmlNames
    {
        private XmlNames() { }

        internal sealed class Saml11
        {
            private Saml11() { }

            public const string DefaultPrefix = "saml";
            public const string Namespace = "urn:oasis:names:tc:SAML:1.0:assertion";
            public const string AltNamespace = "http://docs.oasis-open.org/wss/oasis-wss-saml-token-profile-1.1#SAMLV1.1";
        }
        internal sealed class Saml10
        {
            private Saml10() { }

            public const string DefaultPrefix = "saml";
            public const string Namespace = "urn:oasis:names:tc:SAML:1.0:assertion";
        }

        internal sealed class Xml
        {
            private Xml() { }
            public const string DefaultPrefix = "xml";
            public const string Namespace = "http://www.w3.org/XML/1998/namespace";
            public const string Language = "lang";
            public const string DateTimeFormat = "yyyy-MM-ddTHH:mm:ssZ";
        }

        internal sealed class XmlSchema
        {
            private XmlSchema() { }

            public const string DefaultPrefix = "xsd";
            public const string Namespace = "http://www.w3.org/2001/XMLSchema";
            public const string SchemaLocation = "http://www.w3.org/2001/xml.xsd";
            public const string LocalSchemaLocation = "xml.xsd";
        }
        internal sealed class WSAddressing
        {
            private WSAddressing() { }

            public const string DefaultPrefix = "wsa";
            public const string Namespace = "http://www.w3.org/2005/08/addressing";
            public const string SchemaLocation = Namespace + "/addressing.xsd";
            public const string LocalSchemaLocation = "addressing.xsd";
            public const string EndpointReference = "EndpointReference";
            public const string Address = "Address";
            public const string Metadata = "Metadata";

        }
        internal sealed class WSAddressing04
        {
            private WSAddressing04() { }

            public const string DefaultPrefix = "wsa04";
            public const string Namespace = "http://schemas.xmlsoap.org/ws/2004/08/addressing";
            public const string SchemaLocation = "http://schemas.xmlsoap.org/ws/2004/08/addressing/";
            public const string LocalSchemaLocation = "addressing04.xsd";

        }
        internal sealed class WSPolicy
        {
            private WSPolicy() { }

            public const string DefaultPrefix = "wsp";
            public const string Namespace = "http://schemas.xmlsoap.org/ws/2004/09/policy";
            public const string SchemaLocation = Namespace + "/ws-policy.xsd";
            public const string LocalSchemaLocation = "ws-policy.xsd";
            public const string Policy = "Policy";
            public const string AppliesTo = "AppliesTo";
        }
        internal sealed class XmlEnc
        {
            private XmlEnc() { }

            public const string DefaultPrefix = "enc";
            public const string Namespace = "http://www.w3.org/2001/04/xmlenc#";
            public const string EncryptedData = "EncryptedData";
            public const string EncryptionMethod = "EncryptionMethod";
            public const string EncryptionProperties = "EncryptionProperties";
            public const string CipherData = "CipherData";
            public const string CipherValue = "CipherValue";
            public const string Encoding = "Encoding";
            public const string MimeType = "MimeType";
            public const string Type = "Type";
            public const string Id = "Id";
            public const string Algorithm = "Algorithm";
            public const string LocalSchemaLocation = "xenc-schema.xsd";
            public const string SchemaLocation = "http://www.w3.org/TR/xmlenc-core/xenc-schema.xsd";
        }
        internal sealed class XmlDSig
        {
            private XmlDSig() { }

            public const string DefaultPrefix = "dsig";
            public const string Namespace = "http://www.w3.org/2000/09/xmldsig#";
            public const string SchemaLocation = "http://www.w3.org/TR/xmldsig-core/xmldsig-core-schema.xsd";
            public const string LocalSchemaLocation = "xmldsig-core-schema.xsd";
            public const string X509CertificateElement = "X509Certificate";
            public const string X509IssuerSerialElement = "X509IssuerSerial";
            public const string X509IssuerNameElement = "X509IssuerName";
            public const string X509SerialNumberElement = "X509SerialNumber";
            public const string X509DataElement = "X509Data";
            public const string RSAKeyValueElement = "RSAKeyValue";
            public const string Signature = "Signature";
        }
        internal sealed class WSSecurityUtility
        {
            private WSSecurityUtility() { }

            public const string DefaultPrefix = "wssu";
            public const string Namespace = "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd";
            public const string SchemaLocation = "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd";
            public const string LocalSchemaLocation = "oasis-200401-wss-wssecurity-utility-1.0.xsd";
        }
        internal sealed class WSSecurityExt
        {
            private WSSecurityExt() { }

            public const string DefaultPrefix = "wsse";
            public const string Namespace = "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd";
            public const string SchemaLocation = "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd";
            public const string LocalSchemaLocation = "oasis-200401-wss-wssecurity-secext-1.0.xsd";
            public const string Sha1ThumbrpintKeyTypeValue = "http://docs.oasis-open.org/wss/2004/xx/oasis-2004xx-wss-soap-message-security-1.1#ThumbprintSHA1";
            public const string KeyIdentifierElement = "KeyIdentifier";
            public const string ValueTypeAttribute = "ValueType";

        }
        internal sealed class WSMetadataExchange
        {
            private WSMetadataExchange() { }
            public const string DefaultPrefix = "wsmex";
            public const string Namespace = "http://schemas.xmlsoap.org/ws/2004/09/mex";
        }
        internal sealed class Soap11
        {
            private Soap11() { }
            public const string DefaultPrefix = "soap";
            public const string Namespace = "http://schemas.xmlsoap.org/soap/envelope/";
        }
        internal sealed class Soap12
        {
            private Soap12() { }
            public const string DefaultPrefix = "soap";
            public const string Namespace = "http://www.w3.org/2003/05/soap-envelope";
        }

        internal sealed class WSIdentity
        {
            private WSIdentity() { }

            public const string DefaultPrefix = "wsid";
            public const string Namespace = "http://schemas.xmlsoap.org/ws/2005/05/identity";
            public const string LocalSchemaLocation = "identity.xsd";
            public const string ClaimElement = "ClaimType";
            public const string SupportedClaimTypeElement = "SupportedClaimType";
            public const string ProvisionAtElement = "ProvisionAt";
            public const string RequestBrowserToken = "RequestBrowserToken";
            public const string EndpointIdentity = "Identity";
            public const string ThumbprintUri = Namespace + "#KeyThumbprint";
            public const string DictionaryUri = Namespace + "/claims";
            public const string SelfIssuerUri = Namespace + "/issuer/self";
            public const string PersonalCategoryUri = "http://icardissuer.xmlsoap.org/categories/identitycard";
            public const string UserNamePasswordAuth = Namespace + "/A12nMethod/UsernamePassword";
            public const string KerberosV5Auth = Namespace + "/A12nMethod/KerberosV5";
            public const string X509V3SoftAuth = Namespace + "/A12nMethod/X509V3Soft";
            public const string X509V3SmartCardAuth = Namespace + "/A12nMethod/X509V3SmartCard";
            public const string SelfIssuedAuth = Namespace + "/A12nMethod/SelfIssuedToken";
            public const string RequestDisplayTokenElement = "RequestDisplayToken";
            public const string RequestedDisplayTokenElement = "RequestedDisplayToken";
            public const string DisplayTokenElement = "DisplayToken";
            public const string DisplayTokenTextElement = "DisplayTokenText";
            public const string DisplayClaimElement = "DisplayClaim";
            public const string DisplayTagElement = "DisplayTag";
            public const string DisplayValueElement = "DisplayValue";
            public const string DescriptionElement = "Description";
            public const string InfoCardElement = "InformationCard";
            public const string RoamingInfoCardElement = "RoamingInformationCard";
            public const string InfoCardRefElement = "InformationCardReference";
            public const string CardNameElement = "CardName";
            public const string CardImageElement = "CardImage";
            public const string CardIdElement = "CardId";
            public const string CardVersionElement = "CardVersion";
            public const string IssuerNameElement = "IssuerName";
            public const string IssuerElement = "Issuer";
            public const string IssuerUriElement = "IssuerUri";
            public const string TimeIssuedElement = "TimeIssued";
            public const string TimeExpiresElement = "TimeExpires";
            public const string SupportedClaimTypeListElement = "SupportedClaimTypeList";
            public const string SupportedTokenTypeListElement = "SupportedTokenTypeList";
            public const string RequirePinProtectionElement = "RequirePinProtection";
            public const string TokenServiceListElement = "TokenServiceList";
            public const string TokenServiceElement = "TokenService";
            public const string UserCredentialElement = "UserCredential";
            public const string DisplayCredentialHintElement = "DisplayCredentialHint";
            public const string UserNamePasswordCredentialElement = "UsernamePasswordCredential";
            public const string X509V3CredentialElement = "X509V3Credential";
            public const string KerberosV5CredentialElement = "KerberosV5Credential";
            public const string SelfIssuedCredentialElement = "SelfIssuedCredential";
            public const string RequireAppliesToElement = "RequireAppliesTo";
            public const string UserNameElement = "Username";
            public const string PrivatePersonalIdentifierElement = "PrivatePersonalIdentifier";
            public const string MaxTokenAgeElement = "MaxTokenAge";
            public const string OpaqueEndPointElement = "OpaqueEndpoint";
            public const string UriAttribute = "Uri";
            public const string OptionalAttribute = "Optional";
            public const string MimeTypeAttribute = "MimeType";
            public const string MethodAttribute = "Method";
            public const string PrivacyNoticeAtElement = "PrivacyNotice";
            public const string IsManagedElement = "IsManaged";
            public const string MasterKeyElement = "MasterKey";
            public const string PinDigestElement = "PinDigest";
            public const string HashSaltElement = "HashSalt";
            public const string TimeLastUpdatedElement = "TimeLastUpdated";
            public const string ClaimValueElement = "ClaimValue";
            public const string RoamingStoreElement = "RoamingStore";
            public const string IsSelfIssuedElement = "IsSelfIssued";
            public const string StoreSaltElement = "StoreSalt";
            public const string IssuerIdElement = "IssuerId";
            public const string IterationCountElement = "IterationCount";
            public const string EncryptedStoreElement = "EncryptedStore";
            public const string BackgroundColorElement = "BackgroundColor";
            public const string UserPrincipalNameElement = "UserPrincipalName";
            public const string InfoCardMetaDataElement = "InfoCardMetaData";
            public const string InfoCardPrivateDataElement = "InfoCardPrivateData";
            public const string ClaimValueListElement = "ClaimValueList";
            public const string ValueElement = "Value";
        }

        internal sealed class WSTrust
        {
            private WSTrust() { }

            public const string DefaultPrefix = "wst";
            public const string Namespace = "http://schemas.xmlsoap.org/ws/2005/02/trust";
            public const string SchemaLocation = Namespace + "/ws-trust.xsd";
            public const string LocalSchemaLocation = "ws-trust.xsd";
            public const string ClaimsElement = "Claims";
            public const string TokenType = "TokenType";
            public const string EncryptWith = "EncryptWith";
            public const string CustomToken = "customToken";
            public const string KeyType = "KeyType";
            public const string KeySize = "KeySize";
            public const string UseKey = "UseKey";
            public const string SigAttribute = "Sig";
            public static readonly Uri KeyTypeSymmetric = new Uri("http://schemas.xmlsoap.org/ws/2005/02/trust/SymmetricKey");
            public static readonly Uri KeyTypeAsymmetric = new Uri("http://schemas.xmlsoap.org/ws/2005/02/trust/PublicKey");
            public const string RequestSecurityToken = "RequestSecurityToken";
            public const string RequestSecurityTokenAction = Namespace + "/RST/Issue";
            public const string RequestSecurityTokenResponseAction = Namespace + "/RSTR/Issue";
            public const string RequestedSecurityToken = "RequestedSecurityToken";

        }
        internal sealed class WSTransfer
        {
            private WSTransfer() { }

            public const string DefaultPrefix = "wxf";
            public const string Namespace = "http://schemas.xmlsoap.org/ws/2004/09/transfer";
            public const string Create = "Create";
            public const string CreateAction = "http://schemas.xmlsoap.org/ws/2004/09/transfer/Create";
            public const string CreateResponseAction = "http://schemas.xmlsoap.org/ws/2004/09/transfer/CreateResponse";
            public const string Get = "Get";
            public const string GetAction = "http://schemas.xmlsoap.org/ws/2004/09/transfer/Get";
            public const string GetResponseAction = "http://schemas.xmlsoap.org/ws/2004/09/transfer/GetResponse";
            public const string Put = "Put";
            public const string PutAction = "http://schemas.xmlsoap.org/ws/2004/09/transfer/Put";
            public const string PutResponseAction = "http://schemas.xmlsoap.org/ws/2004/09/transfer/PutResponse";
            public const string Delete = "Delete";
            public const string DeleteAction = "http://schemas.xmlsoap.org/ws/2004/09/transfer/Delete";
            public const string DeleteResponseAction = "http://schemas.xmlsoap.org/ws/2004/09/transfer/DeleteResponse";
            public const string ResourceCreated = "ResourceCreated";
            public const string WSTransferContractName = "WS-Transfer";
        }

        /// <summary>
        /// Create a new <b>XmlNamespaceManager</b> using a specified <b>XmlNameTable</b>.
        /// </summary>
        /// <param name="nameTable"><b>XmlNameTable</b> to use.</param>
        /// <returns>An instance of <see cref="XmlNamespaceManager"/>.</returns>
        public static XmlNamespaceManager CreateNamespaceManager(XmlNameTable nameTable)
        {
            XmlNamespaceManager mgr = new XmlNamespaceManager(nameTable);
            mgr.AddNamespace(XmlNames.Saml11.DefaultPrefix, XmlNames.Saml11.Namespace);
            mgr.AddNamespace(XmlNames.Soap12.DefaultPrefix, XmlNames.Soap12.Namespace);
            mgr.AddNamespace(XmlNames.WSAddressing.DefaultPrefix, XmlNames.WSAddressing.Namespace);
            mgr.AddNamespace(XmlNames.WSIdentity.DefaultPrefix, XmlNames.WSIdentity.Namespace);
            mgr.AddNamespace(XmlNames.WSMetadataExchange.DefaultPrefix, XmlNames.WSMetadataExchange.Namespace);
            mgr.AddNamespace(XmlNames.WSPolicy.DefaultPrefix, XmlNames.WSPolicy.Namespace);
            mgr.AddNamespace(XmlNames.WSSecurityExt.DefaultPrefix, XmlNames.WSSecurityExt.Namespace);
            mgr.AddNamespace(XmlNames.WSSecurityUtility.DefaultPrefix, XmlNames.WSSecurityUtility.Namespace);
            mgr.AddNamespace(XmlNames.WSTransfer.DefaultPrefix, XmlNames.WSTransfer.Namespace);
            mgr.AddNamespace(XmlNames.WSTrust.DefaultPrefix, XmlNames.WSTrust.Namespace);
            mgr.AddNamespace(XmlNames.XmlDSig.DefaultPrefix, XmlNames.XmlDSig.Namespace);
            mgr.AddNamespace(XmlNames.XmlEnc.DefaultPrefix, XmlNames.XmlEnc.Namespace);
            mgr.AddNamespace(XmlNames.XmlSchema.DefaultPrefix, XmlNames.XmlSchema.Namespace);
            return mgr;
        }

    }

}