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
using System.IdentityModel.Claims;
using System.IdentityModel.Policy;

namespace nl.telin.authep.informationcards
{
    /// <summary>
    /// A class representing a simpler view of a SAML token 
    /// </summary>
    public class Token
    {
        private AuthorizationContext _authorizationContext = null;
        private Dictionary<string,string> _claims;
        private string _uniqueId;
       
        /// <summary>
        /// Returns the AuthorizationContext behind this token
        /// </summary>
        public AuthorizationContext AuthorizationContext
        {
            get { return _authorizationContext; }
        }

        /// <summary>
        /// Token Constructor
        /// </summary>
        /// <param name="xmlToken">Encrypted xml token</param>
        public Token(String xmlToken)
        {
            byte[] decryptedData = XmlEncDecryptor.DecryptToken(xmlToken);
            _authorizationContext = TokenUtility.AuthenticateToken(decryptedData);
        }

        /// <summary>
        /// Gets the UniqueID of this token. 
        /// 
        /// By default, this uses the PPID and the Issuer's Public Key and hashes them 
        /// together to generate a UniqueID. To use a different field, add a line to the web.config:
        /// 
        ///     <add name="IdentityClaimType" value="http://schemas.xmlsoap.org/ws/2005/05/identity/claims/privatepersonalidentifier" />
        ///
        /// Replacing the value with the URI for your unique claim. 
        /// </summary>
        public string UniqueID
        {
            get
            {
                if( string.IsNullOrEmpty(_uniqueId) )
                    _uniqueId = TokenUtility.GetUniqueName(_authorizationContext);

                return _uniqueId;
            }
        }

        /// <summary>
        /// Returns the "SiteSpecificID" that equals the one the user sees in the Identity Selector.
        /// </summary>
        public string SiteSpecificID
        {
            get
            {
                return TokenUtility.CalculateSiteSpecificID(Claims[ClaimTypes.PPID]);
            }
        }

        /// <summary>
        /// Flattens the claims into a dictionary
        /// </summary>
        protected void FlattenClaims()
        {
            if (null != _claims)
                return;

            _claims = new Dictionary<string,string>();

            foreach( ClaimSet set in _authorizationContext.ClaimSets )
                foreach (Claim claim in set )
                    if (claim.Right == Rights.PossessProperty)
                        _claims.Add(claim.ClaimType, TokenUtility.GetResourceValue(claim));
        }

        /// <summary>
        /// Exposes the claims in all the claimsets as a dictionary of strings.
        /// </summary>
        public Dictionary<string,string> Claims
        {
            get
            {
                if (null == _claims)
                    FlattenClaims();
                return _claims;
            }
        }
    }
}
