using System;
using System.Collections.Generic;
using System.IO;
using System.Security.Cryptography.X509Certificates;
using System.Security.Cryptography;
using System.Text;
using Org.BouncyCastle.Asn1;
using Org.BouncyCastle.Asn1.X509;

using Org.BouncyCastle.Crypto;
using Org.BouncyCastle.Crypto.Engines;
using Org.BouncyCastle.Crypto.Generators;
using Org.BouncyCastle.Crypto.Parameters;
using Org.BouncyCastle.Math;
using Org.BouncyCastle.Security;

namespace nl.telin.authep.lib
{
    /// <summary>
    /// File structure for the EF_DG15 file.
    /// Datagroup 15 contains the public key used in AA.
    /// </summary>
    public class DG15File : IDGFile
    {
        /// <summary>
        /// Get the public key contained in this EF_DG15 file.
        /// </summary>
        public RsaPublicKeyStructure PublicKey;

        /// <summary>
        /// Constructs a new EF_DG15 file.
        /// </summary>
        /// <param name="data">bytes of the EF_DG15 file</param>
        public DG15File(byte[] data)
        {
            dgNumber = 15;
            raw = new byte[data.Length];
            Array.Copy(data, RawBytes, data.Length);
            MemoryStream dg15MemStream = new MemoryStream(data);
            BERTLVInputStream dg15Stream = new BERTLVInputStream(dg15MemStream);
            int tag = dg15Stream.readTag();
            if (tag != IDGFile.EF_DG15_TAG) throw new ArgumentException("Expected EF_DG15_TAG");
            int dg15Length = dg15Stream.readLength();
            byte[] value = dg15Stream.readValue();
            

            Asn1InputStream aIn = new Asn1InputStream(value);
            /*Asn1Sequence seq = (Asn1Sequence) aIn.ReadObject();
            string alg =  ((DerSequence) seq[0])[0].ToString();
            byte[] publicKey = ((DerBitString)seq[1]).GetBytes();*/

            SubjectPublicKeyInfo info = SubjectPublicKeyInfo.GetInstance(aIn.ReadObject());
            PublicKey = RsaPublicKeyStructure.GetInstance(info.GetPublicKey());
        }
    }
}
