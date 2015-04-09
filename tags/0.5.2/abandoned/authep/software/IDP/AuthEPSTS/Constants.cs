using System;

namespace nl.telin.authep.sts.Constants
{
    /// <summary>
    /// Contains string representations of full written numbers.
    /// </summary>
    public static class Numbers
    {
        /// <summary>
        /// TwoThousandFortyEight : 2084
        /// </summary>
        public const string TwoThousandFortyEight = "2048";
    }
}
namespace nl.telin.authep.sts.Constants.WSIdentity
{
    /// <summary>
    /// Contains URI's for some commonly used namespaces.
    /// </summary>
    public static class NamespaceUri
    {
        /// <summary>
        /// Base URI for most identity related definitions
        /// </summary>
        public const string Uri = "http://schemas.xmlsoap.org/ws/2005/05/identity";
        /// <summary>
        /// XML namespace prefix
        /// </summary>
        public const string Prefix = "wsid";
    }

    /// <summary>
    /// Identity elements constants
    /// </summary>
    public static class Elements
    {
        /// <summary>
        /// InformationCardReference
        /// </summary>
        public const string InformationCardReference = "InformationCardReference";
        /// <summary>
        /// RequestDisplayToken
        /// </summary>
        public const string RequestDisplayToken = "RequestDisplayToken";
        /// <summary>
        /// ClientPseudonym
        /// </summary>
        public const string ClientPseudonym = "ClientPseudonym";
    }

    /// <summary>
    /// Define different keytypes
    /// </summary>
    public static class KeyTypes
    {
        /// <summary>
        /// NoProofKey: RP-STS doesn't need to construct the proof token. 
        /// (The proof token contains information that the receiver needs in order to prove it is able to use the returned token.)
        /// </summary>
        public const string NoProofKey = "http://schemas.xmlsoap.org/ws/2005/05/identity/NoProofKey";
    }
}

namespace nl.telin.authep.sts.Constants.Saml
{
    /// <summary>
    /// Define different authentication methods
    /// </summary>
    public static class AuthenticationMethods
    {
        /// <summary>
        /// Unspecified authentication method
        /// </summary>
        public const string Unspecified = "urn:oasis:namespace:tc:SAML:1.0:am:unspecified";
    }
}

namespace nl.telin.authep.sts.Constants.XmlEnc
{
    /// <summary>
    /// Define differnt encryption algorithms
    /// </summary>
    public static class Algorithms
    {
        /// <summary>
        /// RSA-OAEP algorithm
        /// </summary>
        public const string RsaOaepMgf1P = "http://www.w3.org/2001/04/xmlenc#rsa-oaep-mgf1p";
    }

}

namespace nl.telin.authep.sts.Constants.XmlDsig
{
    /// <summary>
    /// Namespace for Xml signatures
    /// </summary>
    public static class NamespaceUri
    {
        /// <summary>
        /// URI for Xml signatures
        /// </summary>
        public const string Uri = "http://www.w3.org/2000/09/xmldsig#";
    }

    /// <summary>
    /// Xml signatures algorithms
    /// </summary>
    public static class Algorithms
    {
        /// <summary>
        /// Rsa Sha algorithm
        /// </summary>
        public const string RsaSha1 = "http://www.w3.org/2000/09/xmldsig#rsa-sha1";
    }
}

namespace nl.telin.authep.sts.Constants.XmlCanonicalization
{
    /// <summary>
    /// Xml canonicalization algorithms
    /// </summary>
    public static class Algorithms
    {
        /// <summary>
        /// Exc c14n algorithm
        /// </summary>
        public const string ExcC14N = "http://www.w3.org/2001/10/xml-exc-c14n#";
    }
}

namespace nl.telin.authep.sts.Constants.WSTrust
{
    /// <summary>
    /// WS Trust namespace constants
    /// </summary>
    public static class NamespaceUri
    {
        /// <summary>
        /// WS Trust URI
        /// </summary>
        public const string Uri = "http://schemas.xmlsoap.org/ws/2005/02/trust";
        /// <summary>
        /// WS Trust namespace prefix
        /// </summary>
        public const string Prefix = "t";
    }

    /// <summary>
    /// WS Trust actions for STS
    /// </summary>
    public static class Actions
    {
        /// <summary>
        /// Defines the request to issue a token
        /// </summary>
        public const string Issue = "http://schemas.xmlsoap.org/ws/2005/02/trust/RST/Issue";
        /// <summary>
        /// Defines the STS' response to an issue request
        /// </summary>
        public const string IssueResponse = "http://schemas.xmlsoap.org/ws/2005/02/trust/RSTR/Issue";

        /// <summary>
        /// Defines the request to renew a token
        /// </summary>
        public const string Renew = "http://schemas.xmlsoap.org/ws/2005/02/trust/RST/Renew";
        /// <summary>
        /// Defines the STS' response to a renew request
        /// </summary>
        public const string RenewResponse = "http://schemas.xmlsoap.org/ws/2005/02/trust/RSTR/Renew";

        /// <summary>
        /// Defines the request to validate a token
        /// </summary>
        public const string Validate = "http://schemas.xmlsoap.org/ws/2005/02/trust/RST/Validate";
        /// <summary>
        /// Defines the STS' response to a validate request
        /// </summary>
        public const string ValidateResponse = "http://schemas.xmlsoap.org/ws/2005/02/trust/RSTR/Validate";

        /// <summary>
        /// Defines the request to cancel STS interaction
        /// </summary>
        public const string Cancel = "http://schemas.xmlsoap.org/ws/2005/02/trust/RST/Cancel";
        /// <summary>
        /// Defines the STS' response to a cancel request
        /// </summary>
        public const string CancelResponse = "http://schemas.xmlsoap.org/ws/2005/02/trust/RSTR/Cancel";
    }

    /// <summary>
    /// WS Trust Attributes
    /// </summary>
    public static class Attributes
    {
        /// <summary>
        /// Context information
        /// </summary>
        public const string Context = "Context";
        /// <summary>
        /// Type
        /// </summary>
        public const string Type = "Type";
    }

    /// <summary>
    /// WS Trust Elements
    /// </summary>
    public static class Elements
    {
        /// <summary>
        /// EncryptWith
        /// </summary>
        public const string EncryptWith = "EncryptWith";
        /// <summary>
        /// EncryptionAlgorithm
        /// </summary>
        public const string EncryptionAlgorithm = "EncryptionAlgorithm";
        /// <summary>
        /// Lifetime
        /// </summary>
        public const string Lifetime = "Lifetime";
        /// <summary>
        /// Claims
        /// </summary>
        public const string Claims = "Claims";
        /// <summary>
        /// KeyType
        /// </summary>
        public const string KeyType = "KeyType";
        /// <summary>
        /// KeySize
        /// </summary>
        public const string KeySize = "KeySize";
        /// <summary>
        /// RequestType
        /// </summary>
        public const string RequestType = "RequestType";
        /// <summary>
        /// RequestSecurityTokenResponse
        /// </summary>
        public const string RequestSecurityTokenResponse = "RequestSecurityTokenResponse";
        /// <summary>
        /// RequestSecurityToken
        /// </summary>
        public const string RequestSecurityToken = "RequestSecurityToken";
        /// <summary>
        /// RequestedSecurityToken
        /// </summary>
        public const string RequestedSecurityToken = "RequestedSecurityToken";
        /// <summary>
        /// RequestedAttachedReference
        /// </summary>
        public const string RequestedAttachedReference = "RequestedAttachedReference";
        /// <summary>
        /// RequestedUnattachedReference
        /// </summary>
        public const string RequestedUnattachedReference = "RequestedUnattachedReference";
        /// <summary>
        /// SignWith
        /// </summary>
        public const string SignWith = "SignWith";
        /// <summary>
        /// TokenType
        /// </summary>
        public const string TokenType = "TokenType";
        /// <summary>
        /// UseKey
        /// </summary>
        public const string UseKey = "UseKey";
        /// <summary>
        /// CanonicalizationAlgorithm
        /// </summary>
        public const string CanonicalizationAlgorithm = "CanonicalizationAlgorithm";
    }

    /// <summary>
    /// Define different request types
    /// </summary>
    public static class RequestTypes
    {
        /// <summary>
        /// Issue a token
        /// </summary>
        public const string Issue = "http://schemas.xmlsoap.org/ws/2005/02/trust/Issue";
    }

    /// <summary>
    /// Define different token types
    /// </summary>
    public static class TokenTypes
    {
        /// <summary>
        /// Saml 1.0 assertion
        /// </summary>
        public const string Saml10Assertion = "urn:oasis:names:tc:SAML:1.0:assertion";
    }

    /// <summary>
    /// Define differnt keytypes
    /// </summary>
    public static class KeyTypes
    {
        /// <summary>
        /// Asymetric key
        /// </summary>
        public const string Asymmetric = "http://schemas.xmlsoap.org/ws/2005/02/trust/PublicKey";
    }
}
