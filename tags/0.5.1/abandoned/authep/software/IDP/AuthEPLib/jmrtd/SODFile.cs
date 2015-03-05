using System;
using System.Collections;
using System.IO;
using System.Text;
using Org.BouncyCastle.Asn1;
using Org.BouncyCastle.Asn1.X509;
using Org.BouncyCastle.Asn1.Nist;
using Org.BouncyCastle.Asn1.Cms;
using Org.BouncyCastle.Asn1.Icao;
using System.Security.Cryptography;
using System.Security.Cryptography.X509Certificates;

namespace nl.telin.authep.lib
{
    /// <summary>
    /// File structure for the EF_SOD file.
    /// This file contains the security object.
    /// </summary>
    public class SODFile
    {
        private static DerObjectIdentifier SHA1_HASH_ALG_OID = new DerObjectIdentifier("1.3.14.3.2.26");
        private static DerObjectIdentifier SHA1_WITH_RSA_ENC_OID = new DerObjectIdentifier("1.2.840.113549.1.1.5");
        private static DerObjectIdentifier SHA256_HASH_ALG_OID = new DerObjectIdentifier("2.16.840.1.101.3.4.2.1");
        private static DerObjectIdentifier E_CONTENT_TYPE_OID = new DerObjectIdentifier("1.2.528.1.1006.1.20.1");
        private static DerObjectIdentifier ICAO_SOD_OID = new DerObjectIdentifier("2.23.136.1.1.1");
        private static DerObjectIdentifier SIGNED_DATA_OID = new DerObjectIdentifier("1.2.840.113549.1.7.2");
        private static DerObjectIdentifier RFC_3369_CONTENT_TYPE_OID = new DerObjectIdentifier("1.2.840.113549.1.9.3");
        private static DerObjectIdentifier RFC_3369_MESSAGE_DIGEST_OID = new DerObjectIdentifier("1.2.840.113549.1.9.4");
        private static DerObjectIdentifier RSA_SA_PSS_OID = new DerObjectIdentifier("1.2.840.113549.1.1.10");
        private static DerObjectIdentifier PKCS1_SHA256_WITH_RSA_OID = new DerObjectIdentifier("1.2.840.113549.1.1.11");
        private static DerObjectIdentifier PKCS1_SHA384_WITH_RSA_OID = new DerObjectIdentifier("1.2.840.113549.1.1.12");
        private static DerObjectIdentifier PKCS1_SHA512_WITH_RSA_OID = new DerObjectIdentifier("1.2.840.113549.1.1.13");
        private static DerObjectIdentifier PKCS1_SHA224_WITH_RSA_OID = new DerObjectIdentifier("1.2.840.113549.1.1.14");

        private SignedData _signedData;
        private LdsSecurityObject _lds;

        /// <summary>
        /// Constructs a new EF_SOD file.
        /// </summary>
        /// <param name="data">bytes of the EF_DG1 file</param>
        public SODFile(byte[] data)
        {
            MemoryStream dataStream = new MemoryStream(data);
            BERTLVInputStream tlvStream = new BERTLVInputStream(dataStream);
            int tag = tlvStream.readTag();
            if (tag != IDGFile.EF_SOD_TAG) throw new ArgumentException("Expected EF_SOD_TAG");
            int length = tlvStream.readLength();

            Asn1InputStream sodAsn1 = new Asn1InputStream(dataStream);
            DerSequence seq = (DerSequence)sodAsn1.ReadObject();
            DerObjectIdentifier objectIdentifier = (DerObjectIdentifier)seq[0];

            //DerTaggedObject o = (DerTaggedObject)seq[1];
            DerSequence s2 = (DerSequence)((DerTaggedObject)seq[1]).GetObject();
            IEnumerator e = s2.GetEnumerator();
            e.MoveNext();
            DerInteger version = (DerInteger)e.Current;
            e.MoveNext();
            Asn1Set digestAlgorithms = (Asn1Set)e.Current;
            e.MoveNext();
            ContentInfo contentInfo = ContentInfo.GetInstance(e.Current);

            Asn1Set signerInfos = null;
            bool certsBer = false;
            bool crlsBer = false;
            Asn1Set certificates = null;
            Asn1Set crls = null;

            while (e.MoveNext())
            {
                Object o = e.Current;
                if (o is Asn1TaggedObject)
                {
                    Asn1TaggedObject tagged = (Asn1TaggedObject)o;
                    switch (tagged.TagNo)
                    {
                        case 0:
                            certsBer = tagged is BerTaggedObject;
                            certificates = Asn1Set.GetInstance(tagged, false);
                            break;
                        case 1:
                            crlsBer = tagged is BerTaggedObject;
                            crls = Asn1Set.GetInstance(tagged, false);
                            break;
                        default:
                            throw new ArgumentException("unknown tag value " + tagged.TagNo);
                    }
                }
                else
                {
                    signerInfos = (Asn1Set)o;
                }
            }
            _signedData = new SignedData(digestAlgorithms, contentInfo, certificates, crls, signerInfos);
            byte[] content = ((DerOctetString)contentInfo.Content).GetOctets();
            Asn1InputStream inStream = new Asn1InputStream(content);
            _lds = new LdsSecurityObject((Asn1Sequence)inStream.ReadObject());
        }

        /// <summary>
        /// Gets the stored data group hashes.
        /// </summary>
        /// <returns>data group hashes indexed by data group numbers (1 to 16)</returns>
        public DataGroupHash[] GetDataGroupHashes()
        {
            return _lds.GetDatagroupHash();
        }

        /// <summary>
        /// Gets the document signing certificate.
	    /// Use this certificate to verify that
	    /// <i>eSignature</i> is a valid signature for
	    /// <i>eContent</i>. This certificate itself is
	    /// signed using the country signing certificate.
        /// </summary>
        /// <returns>the document signing certificate</returns>
        public X509Certificate2 GetDocSigningCertificate()
        {
            if (_signedData.Certificates.Count != 1)
                Console.WriteLine("WARNING: found {0} certificates", _signedData.Certificates.Count);
            byte[] certBytes = _signedData.Certificates[0].GetEncoded();
            //X509Certificate cert = new X509Certificate(certBytes);
            X509Certificate2 cert = new X509Certificate2(certBytes);

            return cert;
        }

        /// <summary>
        /// Verifies the signature over the contents of the security object.
	    /// Clients can also use the accessors of this class and check the
	    /// validity of the signature for themselves.
	    /// 
	    /// See RFC 3369, Cryptographic Message Syntax, August 2002,
	    /// Section 5.4 for details.
        /// </summary>
        /// <returns>status of the verification</returns>
        public bool CheckDocSignature()
        {
            return CheckDocSignature(GetDocSigningCertificate());
        }

        /// <summary>
        /// Verifies the signature over the contents of the security object.
        /// Clients can also use the accessors of this class and check the
        /// validity of the signature for themselves.
        /// 
        /// See RFC 3369, Cryptographic Message Syntax, August 2002,
        /// Section 5.4 for details.
        /// </summary>
        /// <param name="docSigningCert">the certificate to use (should be X509 certificate)</param>
        /// <returns>status of verification</returns>
        public bool CheckDocSignature(X509Certificate2 docSigningCert)
        {
            byte[] eContent = GetEContent();
            byte[] signature = GetSignerInfo().EncryptedDigest.GetOctets();

            string encAlg = GetSignerInfo().DigestEncryptionAlgorithm.ObjectID.Id;
            /*if(encAlg.Equals(RSA_SA_PSS_OID.ToString()))
                encAlg = LookupMnemonicByOID(GetSignerInfo().DigestAlgorithm.ObjectID) + "withRSA/PSS"; 
            else if(encAlg.Equals(SHA256_HASH_ALG_OID.ToString()))
                encAlg = LookupMnemonicByOID(GetSignerInfo().DigestAlgorithm.ObjectID) + "withRSA/PSS";
            else if(encAlg.Equals(PKCS1_SHA256_WITH_RSA_OID.ToString()))
                encAlg = LookupMnemonicByOID(GetSignerInfo().DigestAlgorithm.ObjectID) + "withRSA/PSS";*/

            SHA256 sha256 = SHA256.Create();
            byte[] eContentHash = sha256.ComputeHash(eContent);
            
            RSACryptoServiceProvider rsa = (RSACryptoServiceProvider)docSigningCert.PublicKey.Key;
            bool signatureCheck = rsa.VerifyHash(eContentHash,encAlg, signature);

            X509Chain chain = new X509Chain();
            chain.ChainPolicy.RevocationMode = X509RevocationMode.NoCheck;
            bool signerCheck = chain.Build(docSigningCert);

            return signatureCheck & signerCheck;
        }

        private static String LookupMnemonicByOID(DerObjectIdentifier oid) {
		    if (oid.Equals(X509ObjectIdentifiers.Organization)) { return "O"; }
            if (oid.Equals(X509ObjectIdentifiers.OrganizationalUnitName)) { return "OU"; }
            if (oid.Equals(X509ObjectIdentifiers.CommonName)) { return "CN"; }
            if (oid.Equals(X509ObjectIdentifiers.CountryName)) { return "C"; }
            if (oid.Equals(X509ObjectIdentifiers.StateOrProvinceName)) { return "ST"; }
            if (oid.Equals(X509ObjectIdentifiers.LocalityName)) { return "L"; }
            if (oid.Equals(X509ObjectIdentifiers.IdSha1)) { return "SHA1"; }
            if (oid.Equals(NistObjectIdentifiers.IdSha224)) { return "SHA224"; }
            if (oid.Equals(NistObjectIdentifiers.IdSha256)) { return "SHA256"; }
            if (oid.Equals(NistObjectIdentifiers.IdSha384)) { return "SHA384"; }
            if (oid.Equals(NistObjectIdentifiers.IdSha512)) { return "SHA512"; }
            if (oid.Equals(PKCS1_SHA256_WITH_RSA_OID)) { return "SHA256withRSA"; }
            if (oid.Equals(PKCS1_SHA384_WITH_RSA_OID)) { return "SHA384withRSA"; }
            if (oid.Equals(PKCS1_SHA512_WITH_RSA_OID)) { return "SHA512withRSA"; }
            if (oid.Equals(PKCS1_SHA224_WITH_RSA_OID)) { return "SHA224withRSA"; }
		    throw new ArgumentException("Unknown OID " + oid);
	}

        private SignerInfo GetSignerInfo()
        {
            if (_signedData.SignerInfos.Count != 1)
                Console.WriteLine("WARNING: found {0} signerInfos", _signedData.SignerInfos.Count);

            SignerInfo info = new SignerInfo((Asn1Sequence)_signedData.SignerInfos[0]);  
            return info;
        }

        private byte[] GetEContent()
        {
            SignerInfo signerInfo = GetSignerInfo();
            Asn1Set signedAttributesSet = signerInfo.AuthenticatedAttributes;

            ContentInfo contentInfo = _signedData.EncapContentInfo;
            byte[] contentBytes = ((DerOctetString)contentInfo.Content).GetOctets();

            if (signedAttributesSet.Count == 0) // signed attributes absent, return content to be signed
                return contentBytes;
            else
            // Signed attributes present (i.e. a structure containing a hash of the content), return that structure to be signed... 
            // This option is taken by ICAO passports.
            {
                byte[] attributeBytes = signedAttributesSet.GetDerEncoded();
                string digAlg = signerInfo.DigestAlgorithm.ObjectID.Id;
                
                // We'd better check that the content actually digests to the hash value contained! ;)
                IEnumerator attributes = signedAttributesSet.GetEnumerator();
                byte[] storedDigestedContent = null;
                while (attributes.MoveNext())
                {
                    Org.BouncyCastle.Asn1.Cms.Attribute attribute = new Org.BouncyCastle.Asn1.Cms.Attribute((Asn1Sequence)attributes.Current);
                    DerObjectIdentifier attrType = attribute.AttrType;
                    if (attrType.Equals(RFC_3369_MESSAGE_DIGEST_OID))
                    {
                        Asn1Set attrValueSet = attribute.AttrValues;
                        if (attrValueSet.Count != 1)
                            Console.WriteLine("WARNING: expected only one attribute value in signedAttribute message diegest in eContent!");
                        storedDigestedContent = ((DerOctetString)attrValueSet[0]).GetOctets();
                    }
                }
                if(storedDigestedContent == null)
                    Console.WriteLine("WARNING: error extracting signedAttribute message digest in eContent!");

                byte[] computedDigestedContent = SHA256.Create().ComputeHash(contentBytes);
                if (!Compare(storedDigestedContent, computedDigestedContent))
                    Console.WriteLine("WARNING: error checking signedAttribute message digest in eContent!");

                return attributeBytes;
            }
        }

        private static bool Compare(byte[] array1, byte[] array2)
        {
            if (array1.Length != array2.Length) return false;
            for (int i = 0; i < array1.Length; ++i)
            {
                if (array1[i] != array2[i])
                    return false;
            }
            return true;
        }
    }
}
