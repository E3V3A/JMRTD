/*
 * Copyright (c) Microsoft Corporation.  All rights reserved.
 * 
 * This code is licensed under the Microsoft Permissive License (Ms-PL)
 * 
 * SEE: http://www.microsoft.com/resources/sharedsource/licensingbasics/permissivelicense.mspx
 * 
 * or the EULA.TXT file that comes with this software.
 */
using System;
using System.Collections.Generic;
using System.Configuration;
using System.IdentityModel.Claims;
using System.IdentityModel.Policy;
using System.IdentityModel.Selectors;
using System.IdentityModel.Tokens;
using System.IO;
using System.Net.Mail;
using System.Security.Cryptography;
using System.Security.Principal;
using System.ServiceModel.Security;
using System.Text;
using System.Xml;

namespace nl.telin.authep.informationcards
{
    /// <summary>
    /// <b>TokenUtility</b> has methods to help handling tokens. <b>TokenUtility</b> can authenticate tokens,
    /// calculate SSID's and more.
    /// </summary>
    public class TokenUtility
    {
        /// <summary>
        /// Default amount of time a token can be out of synch with time.
        /// </summary>
        public static readonly TimeSpan DefaultClockSkew = new TimeSpan(0, 5, 0); // 5 minutes

        // store the token skew TimeSpan, save the object creation time
        private static TimeSpan _maxTokenSkew = new TimeSpan(0);

        // store the token identification claim type 
        private static string _identificationClaimType;

        // audience restriction
        private static Uri _audience;

        /// <summary>
        /// If this property is set, we will validate that the token is 
        /// scoped for the audience given. This is an exact match.
        /// </summary>
        public static Uri Audience 
        {
            get { return _audience; }
            set { _audience = value; }
        }

        /// <summary>
        /// This is the Maximum amount the token can be out of synch with time.
        /// 
        /// You can add a web.config parameter to override the default 5 minutes:
        /// 
        /// <add name="MaximumClockSkew" value="600" />
        ///
        ///     example: 10 minutes.
        /// 
        /// </summary>
        static TimeSpan MaximumTokenSkew
        {
            get
            {
                if (_maxTokenSkew.Ticks == 0)
                {
                    string tokenskew = ConfigurationManager.AppSettings["MaximumClockSkew"];
                    _maxTokenSkew = string.IsNullOrEmpty(tokenskew) ?  DefaultClockSkew : new TimeSpan(0, 0, Int32.Parse(tokenskew));
                }
                return _maxTokenSkew;
            }
        }

        /// <summary>
        /// Return the claim type to use for generating the UniqueID claim
        /// This defaults to PPID, but can be overridden in the web.config:
        /// 
        /// <add name="IdentificationClaimType" value="http://some.claim/uri" />
        /// 
        /// You shouldn't override this unless you know what you are doing.
        /// 
        /// </summary>
        public static string IdentificationClaimType
        {
            get
            {
                if (string.IsNullOrEmpty(_identificationClaimType))
                {
                    _identificationClaimType = ConfigurationManager.AppSettings["IdentificationClaimType"];

                    // Or, default to PPID
                    if (string.IsNullOrEmpty(_identificationClaimType))
                        _identificationClaimType = ClaimTypes.PPID;

                }
                return _identificationClaimType;
            }
        }

        /// <summary>
        /// Token Authentication. Translates the decrypted data into a AuthContext
        /// 
        /// This method makes a strong assumption that the decrypted token in in UTF-8 format.
        /// </summary>
        /// <param name="decryptedTokenData">Decrypted token</param>
        public static AuthorizationContext AuthenticateToken(byte[] decryptedTokenData)
        {
            string t = Encoding.UTF8.GetString(decryptedTokenData);
            XmlReader reader = new XmlTextReader(new StreamReader(new MemoryStream(decryptedTokenData), Encoding.UTF8));

            // Extensibility Point:
            // in order to accept different token types, you would need to add additional 
            // code to create an authenticationcontext from the security token. 
            // This code only supports SamlSecurityToken objects.
            SamlSecurityToken token = WSSecurityTokenSerializer.DefaultInstance.ReadToken(reader, null) as SamlSecurityToken;

            if( null == token )
                throw new InformationCardException("Unable to read security token");

            if (null != token.SecurityKeys && token.SecurityKeys.Count > 0)
                throw new InformationCardException("Token Security Keys Exist");

            if (null != _audience &&
                 null != token.Assertion.Conditions &&
                 null != token.Assertion.Conditions.Conditions)
            {
                foreach (SamlCondition condition in token.Assertion.Conditions.Conditions)
                {
                    SamlAudienceRestrictionCondition audienceCondition = condition as SamlAudienceRestrictionCondition;

                    if (null != audienceCondition)
                    {
                        bool match = false;

                        foreach (Uri audience in audienceCondition.Audiences)
                        {
                            match = audience.Equals(_audience);
                            if (match) break;
                        }

                        //
                        // The token is invalid if any condition is not valid. 
                        // An audience restriction condition is valid if any audience 
                        // matches the Relying Party.
                        //
                        if (!match)
                        {
                            throw new InformationCardException("The token is invalid: The audience restrictions does not match the Relying Party.");
                        }
                    }
                }
            }
            /*SamlSecurityTokenAuthenticator SamlAuthenticator = new SamlSecurityTokenAuthenticator(new List<SecurityTokenAuthenticator>(
                                            new SecurityTokenAuthenticator[]{
                                                new RsaSecurityTokenAuthenticator(),
                                                new X509SecurityTokenAuthenticator() }), MaximumTokenSkew);*/
            SamlSecurityTokenAuthenticator SamlAuthenticator = new SamlSecurityTokenAuthenticator(new List<SecurityTokenAuthenticator>(
                                            new SecurityTokenAuthenticator[]{
                                                new RsaSecurityTokenAuthenticator(),
                                                new X509SecurityTokenAuthenticator(X509CertificateValidator.None) }), MaximumTokenSkew);

            return AuthorizationContext.CreateDefaultAuthorizationContext(SamlAuthenticator.ValidateToken(token));
        }

        /// <summary>
        ///     Translates claims to strings
        /// </summary>
        /// <param name="claim">Claim to translate to a string</param>
        /// <returns></returns>
        public static string GetResourceValue(Claim claim)
        {
            string strClaim = claim.Resource as string;
            if (!string.IsNullOrEmpty(strClaim))
                return strClaim;

            IdentityReference reference = claim.Resource as IdentityReference;
            if (null != reference)
            {
                return reference.Value;
            }

            ICspAsymmetricAlgorithm rsa = claim.Resource as ICspAsymmetricAlgorithm;
            if (null != rsa)
            {
                using (SHA256 sha = new SHA256Managed())
                {
                    return Convert.ToBase64String(sha.ComputeHash(rsa.ExportCspBlob(false)));
                }
            }

            MailAddress mail = claim.Resource as MailAddress;
            if (null != mail)
            {
                return mail.ToString();
            }

            byte[] bufferValue = claim.Resource as byte[];
            if (null != bufferValue)
            {
                return Convert.ToBase64String(bufferValue);
            }

            return claim.Resource.ToString();
        }

        /// <summary>
        /// Generates a UniqueID based off the Issuer's key and the IdentificationClaimType (default == PPID) 
        /// </summary>
        /// <param name="authzContext">the Authorization Context</param>
        /// <returns>a unique id</returns>
        public static string GetUniqueName(AuthorizationContext authzContext)
        {
            Claim uniqueIssuerClaim = null;
            Claim uniqueUserClaim = null;

            foreach (ClaimSet cs in authzContext.ClaimSets)
            {
                Claim currentIssuerClaim = GetUniqueRsaClaim(cs.Issuer);

                foreach (Claim c in cs.FindClaims(IdentificationClaimType, Rights.PossessProperty))
                {
                    if (null == currentIssuerClaim)
                    {
                        // Found a claim in a ClaimSet with no RSA issuer.
                        return null;
                    }

                    if (null == uniqueUserClaim)
                    {
                        uniqueUserClaim = c;
                        uniqueIssuerClaim = currentIssuerClaim;
                    }
                    else if (!uniqueIssuerClaim.Equals(currentIssuerClaim))
                    {
                        // Found two of the desired claims with different
                        // issuers. No unique name.
                        return null;
                    }
                    else if (!uniqueUserClaim.Equals(c))
                    {
                        // Found two of the desired claims with different
                        // values. No unique name.
                        return null;
                    }
                }
            }

            // No claim of the desired type was found
            if (null == uniqueUserClaim)
                return null;

            // Unexpected resource type
            string claimValue = uniqueUserClaim.Resource as string;
            if (null == claimValue)
                return null;

            // Unexpected resource type for RSA
            RSA rsa = uniqueIssuerClaim.Resource as RSA;
            if (null == rsa)
                return null;

            return ComputeCombinedId(rsa, claimValue);
        }

        /// <summary>
        /// Gets the Unique RSA Claim from the SAML token.
        /// </summary>
        /// <param name="cs">the claimset which contains the claim</param>
        /// <returns>a RSA claim</returns>
        public static Claim GetUniqueRsaClaim(ClaimSet cs)
        {
            Claim rsa = null;

            foreach (Claim c in cs.FindClaims(ClaimTypes.Rsa, Rights.PossessProperty))
            {
                if (null == rsa)
                {
                    rsa = c;
                }
                else if (!rsa.Equals(c))
                {
                    // Found two non-equal RSA claims
                    return null;
                }
            }
            return rsa;
        }

        /// <summary>
        /// Does the actual calculation of a combined ID from a value and an RSA key.
        /// </summary>
        /// <param name="issuerKey">The key of the issuer of the token</param>
        /// <param name="claimValue">the claim value to hash with.</param>
        /// <returns></returns>
        public static string ComputeCombinedId(RSA issuerKey, string claimValue)
        {
            int nameLength = Encoding.UTF8.GetByteCount(claimValue);
            RSAParameters rsaParams = issuerKey.ExportParameters(false);
            byte[] shaInput;
            byte[] shaOutput;

            int i = 0;
            shaInput = new byte[rsaParams.Modulus.Length + rsaParams.Exponent.Length + nameLength];
            rsaParams.Modulus.CopyTo(shaInput, i);
            i += rsaParams.Modulus.Length;
            rsaParams.Exponent.CopyTo(shaInput, i);
            i += rsaParams.Exponent.Length;
            i += Encoding.UTF8.GetBytes(claimValue, 0, claimValue.Length, shaInput, i);

            using (SHA256 sha = SHA256.Create())
            {
                shaOutput = sha.ComputeHash(shaInput);
            }

            return Convert.ToBase64String(shaOutput);
        }

        /// <summary>
        /// Generates the Site Specific ID to match the one in the Identity Selector.
        /// 
        /// The Identity Selector displays this instead of displaying the PPID.
        /// </summary>
        /// <param name="ppid">the PPID</param>
        /// <returns>a string containing the XXX-XXXX-XXX cosmetic value</returns>
        public static string CalculateSiteSpecificID(string ppid)
        {
            int callSignChars = 10;
            char[] charMap = "QL23456789ABCDEFGHJKMNPRSTUVWXYZ".ToCharArray();
            int charMapLength = charMap.Length;

            byte[] raw = Convert.FromBase64String(ppid);
            raw = SHA1.Create().ComputeHash(raw);

            StringBuilder callSign = new StringBuilder();

            for (int i = 0; i < callSignChars; i++)
            {
                //
                // after char 3 and char 7, place a dash
                //
                if (i == 3 || i == 7)
                {
                    callSign.Append('-');
                }
                callSign.Append(charMap[raw[i] % charMapLength]);
            }
            return callSign.ToString();
        }
    }
}
