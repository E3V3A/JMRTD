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
using System.Data;
using System.Configuration;
using System.Web;
using System.Web.Security;
using System.Web.UI;
using System.Web.UI.WebControls;
using System.Web.UI.WebControls.WebParts;
using System.Web.UI.HtmlControls;
using System.Security.Cryptography.X509Certificates;
using System.Text;
using System.Security.Cryptography;

namespace nl.telin.authep.informationcards
{
    /// <summary>
    /// VerificationCode provides a the ability to generate codes 
    /// for a round-trip email verification
    /// </summary>
    public class VerificationCode
    {
        /// <summary>
        /// the length of the code desired (without dashes)
        /// </summary>
        private const int validationCodeLength = 12;
        /// <summary>
        /// the distance between the dashes in the generated code
        /// </summary>
        private const int validationCodeDashInterval= 4;

        /// <summary>
        /// the character map to use to generate the code
        /// </summary>
        private static string charMapString = "QL23456789ABCDEFGHJKMNPRSTUVWXYZ";
        /// <summary>
        /// the char array of the same
        /// </summary>
        private static char[] charMap = charMapString.ToCharArray();
        /// <summary>
        /// the length of the char map (optimization)
        /// </summary>
        private static int charMapLength = charMap.Length;
        /// <summary>
        /// private key data for the HMACSHA256 to generate the codes
        /// </summary>
        private static byte[] validationCodeKey;

        /// <summary>
        /// the context is a value representing the intent or 
        /// operation that the user was performing when they generated the code
        /// </summary>
        private int context;
        /// <summary>
        /// The message are the elements to use as seed data for the code
        /// </summary>
        private object[] message;

        /// <summary>
        /// the generated code
        /// </summary>
        private string code;

        /// <summary>
        /// the companion checksum code
        /// </summary>
        private string confirmationCode;

        /// <summary>
        /// Constructor.
        /// 
        /// Takes the context and any objects for the message
        /// </summary>
        /// <example>
        /// 
        ///    VerificationCode v;
        ///    v = new VerificationCode(task, emailaddress, ppid);
        /// 
        ///    if (v.IsValid("xxx-xxx-xx"))
        ///        GetContext("xxx-xxx-xx");
        /// 
        /// </example>
        /// <param name="context"></param>
        /// <param name="message"></param>
        public VerificationCode(int context, params object[] message)
        {
            // the context must be able to be stored and retrieved as 
            // a single character in the verification code, so must
            // not be larger than the number of possible codes.
            if (context > charMapLength)
                throw new ArgumentException("context value exceeds the resolution of the character map");

            // must have *some* data to throw in the hash.
            if (null == message)
                throw new ArgumentNullException("message", "message may not be null");
            if (0 == message.Length)
                throw new ArgumentException("message must have at least one element", "message");

            this.context = context;
            this.message = message;
        }


        /// <summary>
        /// Gets the code, generates it if neccesary
        /// </summary>
        public string Code
        {
            get
            {
                if (null == code)
                {
                    char[] prefix = new char[2];
                    int randomCosmeticSeed = new Random().Next(charMapLength);

                    // time seed
                    prefix[0] = charMap[randomCosmeticSeed];
                    // context  
                    prefix[1] = charMap[(context + randomCosmeticSeed) % charMapLength];

                    StringBuilder messagebuilder = new StringBuilder();
                    for (int i = 0; i < message.Length; i++)
                        messagebuilder.Append(message[i].ToString());

                    code = ComputeAbbreviatedValidationHash(Encoding.UTF8.GetBytes(prefix), Encoding.UTF8.GetBytes(messagebuilder.ToString()));
                }
                return code;
            }
        }

        /// <summary>
        /// Gets the companion code, generates it if neccesary
        /// </summary>
        public string ConfirmationCode
        {
            get
            {
                if (null == confirmationCode)
                    confirmationCode = ComputeAbbreviatedValidationHash(new byte[0], Encoding.UTF8.GetBytes(Code));

                return confirmationCode;
            }
        }

     
        /// <summary>
        /// validates the existing code against the original one.
        /// 
        /// Assumes the time seed and context of the original code.
        /// </summary>
        /// <param name="existingCode">Original Code</param>
        /// <returns>true if the codes match</returns>
        public bool IsValid(string existingCode)
        {
            if (string.IsNullOrEmpty(existingCode) || existingCode.Length < 2)
                throw new ArgumentException("existing code is not valid", "existingCode");

            string prefix = existingCode.Substring(0, 2);

            StringBuilder messagebuilder = new StringBuilder();
            for (int i = 0; i < message.Length; i++)
                messagebuilder.Append(message[i].ToString());

            // return if the code match or not.
            return existingCode.Equals(ComputeAbbreviatedValidationHash(Encoding.UTF8.GetBytes(prefix), Encoding.UTF8.GetBytes(messagebuilder.ToString())));
        }

        /// <summary>
        /// Returns the context (int) of the orignal code.
        /// </summary>
        /// <param name="existingCode">the original code presented to the user</param>
        /// <returns>the context (an int)</returns>
        public static int GetContext(string existingCode)
        {
            if (string.IsNullOrEmpty(existingCode) || existingCode.Length < 2)
                throw new ArgumentException("existing code is not valid", "existingCode");

            return ((charMapString.IndexOf(existingCode[1]) - charMapString.IndexOf(existingCode[0])) + charMapLength) % charMapLength;
        }

        /// <summary>
        /// Validates that the confirmatiioon
        /// </summary>
        /// <param name="existingCode"></param>
        /// <param name="originalCode"></param>
        /// <returns></returns>
        public static bool IsProperlySigned(string existingCode, string originalCode)
        {
            return originalCode.Equals(ComputeAbbreviatedValidationHash(new byte[0], Encoding.UTF8.GetBytes(existingCode)));
        }

        /// <summary>
        /// Computes an hashed validation code .
        /// 
        /// We are trying to stop the possibilty that an attacker can submit
        /// a token, and fool us into allowing them to respond to a verification 
        /// request.
        /// 
        /// We don't need to expose the full breadth of the cryptographic operation
        /// to verify that we have signed something; just enough to make sure that
        /// it can't be replicated without the private data (in this case, some arbitrary data from the config).
        /// </summary>
        /// <param name="prefix">text to prefix the output message with, and to salt the message with</param>
        /// <param name="messagebytes">the value to hash with.</param>
        /// <returns></returns>
        private static string ComputeAbbreviatedValidationHash(byte[] prefix, byte[] messagebytes)
        {
            if (null == prefix)
                throw new ArgumentNullException("prefix", "prefix may not be null");
            if (null == messagebytes)
                throw new ArgumentNullException("messagebytes", "messagebytes may not be null");

            StringBuilder result = new StringBuilder();
            byte[] shaInput;
            byte[] shaOutput;

            shaInput = new byte[prefix.Length + messagebytes.Length];
            prefix.CopyTo(shaInput,0);
            messagebytes.CopyTo(shaInput, prefix.Length);

            // hash the message
            using (HMACSHA256 hmacsha256 = new HMACSHA256(ValidationCodeKey))
                shaOutput = hmacsha256.ComputeHash(shaInput);

            // prefill the prefix bytes
            for (int j = 0; j < prefix.Length; j++)
                result.Append((char)prefix[j]);

            // abbreviate the hash.
            for (int j = prefix.Length; j < validationCodeLength; j++)
            {
                if (j > 0 && j % validationCodeDashInterval== 0)
                    result.Append('-');

                result.Append(charMap[shaOutput[j] % charMapLength]);
            }
            return result.ToString();
        }
        
        /// <summary>
        /// Retrieves the random salt data to use for the key as part of the hash
        /// </summary>
        private static byte[] ValidationCodeKey
        {
            get
            {
                if (null == validationCodeKey)
                {
                    string vck= ConfigurationManager.AppSettings["ValidationCodeKey"];
                    if (string.IsNullOrEmpty(vck))
                        throw new InformationCardException("The validationCodeKey setting is not present in the web.config.");
                    validationCodeKey = Encoding.UTF8.GetBytes(vck);
                }
                return validationCodeKey;
            }
        }
    }

}