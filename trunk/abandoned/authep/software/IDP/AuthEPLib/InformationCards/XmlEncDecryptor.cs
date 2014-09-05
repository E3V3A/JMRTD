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
using System.IdentityModel.Selectors;
using System.IdentityModel.Tokens;
using System.IO;
using System.Security.Cryptography;
using System.Security.Cryptography.X509Certificates;
using System.ServiceModel.Security;
using System.Xml;

namespace nl.telin.authep.informationcards
{
    /// <summary>
    /// Summary description for XmlEncDecryptor
    /// </summary>
    public class XmlEncDecryptor
    {
        // list of certificates allowed to decrypt with.
        static List<SecurityToken> _tokens = null;

        static class XmlEncryptionStrings
        {
            public const string Namespace = "http://www.w3.org/2001/04/xmlenc#";
            public const string EncryptionMethod = "EncryptionMethod";
            public const string CipherValue = "CipherValue";
            public const string Algorithm = "Algorithm";
            public const string EncryptedData = "EncryptedData";
            public const string CipherData = "CipherData";
        }

        /// <summary>
        /// Adds a certificate to the list of certificates to decrypt with.
        /// </summary>
        /// <param name="certificate">The x509 cert to use for decryption</param>
        public static void AddDecryptionCertificate(X509Certificate2 certificate)
        {
            Tokens.Add(new X509SecurityToken(certificate));
        }

        /// <summary>
        /// Adds a certificate to the list of certificates to decrypt with.
        /// 
        /// </summary>
        /// <param name="storeName">store name of the certificate</param>
        /// <param name="storeLocation">store location</param>
        /// <param name="thumbprint">thumbprint of the cert to use</param>
        public static void AddDecryptionCertificate(StoreName storeName, StoreLocation storeLocation, string thumbprint)
        {
            X509Store store = new X509Store(storeName, storeLocation);
            store.Open(OpenFlags.ReadOnly);

            foreach (X509Certificate2 certificate in store.Certificates.Find(X509FindType.FindByThumbprint, thumbprint, true) )
                if( certificate.HasPrivateKey )
                    Tokens.Add(new X509SecurityToken(certificate));

            store.Close();
        }

        /// <summary>
        /// Adds a store of certificates to the list of certificates to decrypt with.
        /// 
        /// </summary>
        /// <param name="storeName">store name of the certificates</param>
        /// <param name="storeLocation">store location</param>
        public static void AddDecryptionCertificates(StoreName storeName, StoreLocation storeLocation)
        {
            X509Store store = new X509Store(storeName, storeLocation);
            store.Open(OpenFlags.ReadOnly);

            foreach (X509Certificate2 certificate in store.Certificates)
                if (certificate.HasPrivateKey)
                    Tokens.Add(new X509SecurityToken(certificate));

            store.Close();
        }
        
        /// <summary>
        /// Assembles a list of possible decryption certificates, from the store/location set
        /// 
        /// Defaults to localmachine:my (same place SSL certs are)
        /// 
        /// </summary>
        static List<SecurityToken> Tokens
        {
            get
            {
                if (null == _tokens)
                {
                    _tokens = new List<SecurityToken>();

                    StoreName storeName = StoreName.My;
                    StoreLocation storeLocation = StoreLocation.LocalMachine;

                    string rpStoreName = ConfigurationManager.AppSettings["StoreName"];
                    string rpStoreLocation = ConfigurationManager.AppSettings["StoreLocation"];

                    if (!string.IsNullOrEmpty(rpStoreName))
                        storeName = (StoreName)Enum.Parse(typeof(StoreName), rpStoreName, true);

                    if (!string.IsNullOrEmpty(rpStoreLocation))
                        storeLocation = (StoreLocation)Enum.Parse(typeof(StoreLocation), rpStoreLocation, true);

                    AddDecryptionCertificates(storeName, storeLocation);
                }
                return _tokens;
            }
        }

        /// <summary>
        /// Decrpyts a security token from an XML EncryptedData 
        /// </summary>
        /// <param name="xmlToken">the XML token to decrypt</param>
        /// <returns>A byte array of the contents of the encrypted token</returns>
        public static byte[] DecryptToken(string xmlToken)
        {
            XmlReader reader = new XmlTextReader(new StringReader(xmlToken));
            byte[] securityTokenData;
            string encryptionAlgorithm;
            SecurityKeyIdentifier keyIdentifier;
            bool isEmptyElement;

            // if it's not an xml:enc element, something is dreadfully wrong.
            if (!reader.IsStartElement(XmlEncryptionStrings.EncryptedData, XmlEncryptionStrings.Namespace))
                throw new InvalidOperationException();
            
            reader.Read(); // looks good.

            // if it's not an encryption method, something is dreadfully wrong.
            if (!reader.IsStartElement(XmlEncryptionStrings.EncryptionMethod, XmlEncryptionStrings.Namespace))
                throw new InformationCardException("Failed to find the encryptionAlgorithm");

            // Looks good, let's grab the alg.
            isEmptyElement = reader.IsEmptyElement;
            encryptionAlgorithm = reader.GetAttribute(XmlEncryptionStrings.Algorithm);
            reader.Read();

            if (!isEmptyElement)
            {
                while (reader.IsStartElement())
                    reader.Skip();
                reader.ReadEndElement();
            }

            // get the key identifier
            keyIdentifier = WSSecurityTokenSerializer.DefaultInstance.ReadKeyIdentifier(reader);

            // resolve the symmetric key
            SymmetricSecurityKey decryptingKey = (SymmetricSecurityKey)SecurityTokenResolver.CreateDefaultSecurityTokenResolver(Tokens.AsReadOnly(), false).ResolveSecurityKey(keyIdentifier[0]);
            SymmetricAlgorithm algorithm = decryptingKey.GetSymmetricAlgorithm(encryptionAlgorithm);

            // dig for the security token data itself.
            reader.ReadStartElement(XmlEncryptionStrings.CipherData, XmlEncryptionStrings.Namespace);
            reader.ReadStartElement(XmlEncryptionStrings.CipherValue, XmlEncryptionStrings.Namespace);
            securityTokenData = Convert.FromBase64String(reader.ReadString());
            reader.ReadEndElement(); // CipherValue
            reader.ReadEndElement(); // CipherData
            reader.ReadEndElement(); // EncryptedData

            // decrypto-magic!
            int ivSize = algorithm.BlockSize / 8;
            byte[] iv = new byte[ivSize];
            Buffer.BlockCopy(securityTokenData, 0, iv, 0, iv.Length);
            algorithm.Padding = PaddingMode.ISO10126;
            algorithm.Mode = CipherMode.CBC;
            ICryptoTransform decrTransform = algorithm.CreateDecryptor(algorithm.Key, iv);
            byte[] plainText = decrTransform.TransformFinalBlock(securityTokenData, iv.Length, securityTokenData.Length - iv.Length);
            decrTransform.Dispose();

            return plainText;
        }
    }
}