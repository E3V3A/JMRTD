using System;
using System.Collections.Generic;
using System.Text;
using System.Security.Cryptography;
using Org.BouncyCastle.Asn1.X509;
using Org.BouncyCastle.Asn1.Icao;
using Org.BouncyCastle.Crypto.Digests;
using Org.BouncyCastle.Crypto.Engines;
using Org.BouncyCastle.Crypto.Parameters;
using Org.BouncyCastle.Crypto.Signers;

namespace nl.telin.authep.lib
{
    /// <summary>
    /// The <b>Verification</b> class assists with doing some of the checks that can be performed on a digital passport. 
    /// </summary>
    public class Verification
    {
        /// <summary>
        /// Check the hash values of a collection of DG files with the stored hash values in a SOd file.
        /// </summary>
        /// <param name="dgFiles">DG files from which the hashes need to be checked.</param>
        /// <param name="sod">SOd file containing hashes for the DG files.</param>
        /// <returns>True if all hashes match the values in the SOd file or else false.</returns>
        public static bool CheckHash(List<IDGFile> dgFiles, SODFile sod)
        {
            foreach (IDGFile dgFile in dgFiles)
            {
                if (!CheckHash(dgFile, sod))
                    return false;
            }
            return true;
        }

        /// <summary>
        /// Check the hash value of a specific DG file with the stored hash value in a SOd file.
        /// </summary>
        /// <param name="dgFile">DG file from which the hash needs to be checked.</param>
        /// <param name="sod">SOd file containing the hash for the DG file.</param>
        /// <returns>True if the hash of the DG file matches the value stored in the SOd file.</returns>
        public static bool CheckHash(IDGFile dgFile, SODFile sod)
        {
            foreach (DataGroupHash hash in sod.GetDataGroupHashes())
            {
                if (hash.DataGroupNumber == dgFile.DataGroupNumber)
                {
                    SHA256 sha256 = SHA256.Create();
                    byte[] dgHash = sha256.ComputeHash(dgFile.RawBytes);
                    byte[] sodHash = hash.DataGroupHashValue.GetOctets();
                    return Compare(dgHash, sodHash);
                }
            }
            throw new ArgumentException("Hash of DG" + dgFile.DataGroupNumber + " not found in SOd.");
        }

        /// <summary>
        /// Check an Active Authentication reply from the passport.
        /// </summary>
        /// <param name="publicKey">The AA public key read from the passport.</param>
        /// <param name="message">The original message.</param>
        /// <param name="signature">The response from the passport</param>
        /// <returns>True if the signature is correct for this message.</returns>
        public static bool CheckAA(RsaPublicKeyStructure publicKey, byte[] message, byte[] signature)
        {
            SHA1 sha1 = SHA1.Create();
            RsaEngine rsa = new RsaEngine();
            RsaKeyParameters p = new RsaKeyParameters(false, publicKey.Modulus, publicKey.PublicExponent);
            rsa.Init(false, p);

            byte[] digestedMessage = sha1.ComputeHash(message); // should always be 20 bytes
            byte[] m2 = new byte[8];
            Array.Copy(digestedMessage, 0, m2, 0, m2.Length);
            byte[] plainText = rsa.ProcessBlock(signature, 0, signature.Length);
            byte[] m1 = recoverMessage(digestedMessage.Length, plainText);

            Sha1Digest digest = new Sha1Digest();
            Iso9796d2Signer signer = new Iso9796d2Signer(rsa, digest);
            signer.Init(false, p);
            signer.BlockUpdate(m1, 0, m1.Length);
            signer.BlockUpdate(m2, 0, m2.Length);
            return signer.VerifySignature(signature);
        }

        /// <summary>
        /// Recovers the M1 part of the message sent back by the AA protocol
        /// (INTERNAL AUTHENTICATE command). The algorithm is described in
        /// ISO 9796-2:2002 9.3.
        /// Based on code by Ronny (ronny@cs.ru.nl) who presumably ripped this from Bouncy Castle. 
        /// </summary>
        /// <param name="digestLength">should be 20</param>
        /// <param name="plaintext">response from card, already 'decrypted' (using the AA public key)</param>
        /// <returns>The m1 part of the message.</returns>
        public static byte[] recoverMessage(int digestLength, byte[] plaintext)
        {
            if (plaintext == null || plaintext.Length < 1)
            {
                throw new ArgumentException("Plaintext too short to recover message");
            }
            if (((plaintext[0] & 0xC0) ^ 0x40) != 0)
            {
                // 0xC0 = 1100 0000, 0x40 = 0100 0000
                throw new FormatException("Could not get M1");
            }
            if (((plaintext[plaintext.Length - 1] & 0xF) ^ 0xC) != 0)
            {
                // 0xF = 0000 1111, 0xC = 0000 1100
                throw new FormatException("Could not get M1");
            }
            int delta = 0;
            if (((plaintext[plaintext.Length - 1] & 0xFF) ^ 0xBC) == 0)
            {
                delta = 1;
            }
            else
            {
                // 0xBC = 1011 1100
                throw new FormatException("Could not get M1");
            }

            /* find out how much padding we've got */
            int paddingLength = 0;
            for (; paddingLength < plaintext.Length; paddingLength++)
            {
                // 0x0A = 0000 1010
                if (((plaintext[paddingLength] & 0x0F) ^ 0x0A) == 0)
                {
                    break;
                }
            }
            int messageOffset = paddingLength + 1;

            int paddedMessageLength = plaintext.Length - delta - digestLength;
            int messageLength = paddedMessageLength - messageOffset;

            /* there must be at least one byte of message string */
            if (messageLength <= 0)
            {
                throw new FormatException("Could not get M1");
            }

            /* TODO: if we contain the whole message as well, check the hash of that. */
            if ((plaintext[0] & 0x20) == 0)
            {
                throw new FormatException("Could not get M1");
            }
            else
            {
                byte[] recoveredMessage = new byte[messageLength];
                //System.arraycopy(plaintext, messageOffset, recoveredMessage, 0, messageLength);
                Array.Copy(plaintext, messageOffset, recoveredMessage, 0, messageLength);
                return recoveredMessage;
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
