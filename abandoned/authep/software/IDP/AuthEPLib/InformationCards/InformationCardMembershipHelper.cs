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
using System.Data;
using System.Data.SqlClient;
using System.IdentityModel.Claims;
using System.Security.Cryptography;
using System.Text;
using System.Web.Security;
using System.Web.SessionState;
//using DataSetTableAdapters;
using System.Security.Cryptography.X509Certificates;

namespace nl.telin.authep.informationcards
{
    /// <summary>
    /// This class contains methods to enhance a SQL membership provider with Information Cards.
    /// </summary>
    public class InformationCardMembershipHelper
    {

        /// <summary>
        /// Gets a username given the UNIQUEID of the token 
        /// </summary>
        /// <param name="uniqueId">the unique id (PPID+ISSUER ID)</param>
        /// <returns>the username, or null when there is no such user.</returns>
        public static string GetUsernameFromUniqueID(string uniqueId)
        {
            try
            {
                using (SqlConnection connection = new SqlConnection(ConfigurationManager.ConnectionStrings["LocalSqlServer"].ConnectionString))
                {
                    connection.Open();

                    SqlCommand cmd = new SqlCommand("dbo.aspnet_InformationCards_Lookup", connection);
                    cmd.CommandType = CommandType.StoredProcedure;
                    cmd.Parameters.Add(new SqlParameter("@uniqueId", uniqueId));
                    return (cmd.ExecuteScalar() as string); 
                }
            }
            catch (SqlException)
            {
                return null;
            }
        }

        /// <summary>
        /// Associates a PPID with a user account.
        /// </summary>
        /// <param name="username">the account to associate with</param>
        /// <param name="uniqueId">the UniqueID of the token</param>
        /// <param name="ppid">the PPID claim of the token</param>
        public static void AssociateUser(string username, string uniqueId, string ppid)
        {
            using (SqlConnection connection = new SqlConnection(ConfigurationManager.ConnectionStrings["LocalSqlServer"].ConnectionString))
            {
                connection.Open();

                SqlCommand cmd = new SqlCommand("dbo.aspnet_InformationCards_Associate", connection);
                cmd.CommandType = CommandType.StoredProcedure;
                cmd.Parameters.Add(new SqlParameter("@ApplicationName", Membership.ApplicationName));
                cmd.Parameters.Add(new SqlParameter("@UserName", username));
                cmd.Parameters.Add(new SqlParameter("@UniqueID", uniqueId));
                cmd.Parameters.Add(new SqlParameter("@PPID", ppid));
                cmd.ExecuteNonQuery();
            }
        }


        /// <summary>
        /// Unlinks a card from a user account
        /// </summary>
        /// <param name="username">user account name</param>
        /// <param name="ppid">the PPID claim of the token</param>
        public static void UnAssociateUser(string username, string ppid)
        {
            using (SqlConnection connection = new SqlConnection(ConfigurationManager.ConnectionStrings["LocalSqlServer"].ConnectionString))
            {
                connection.Open();
                SqlCommand cmd = new SqlCommand("dbo.aspnet_InformationCards_UnAssociate", connection);
                cmd.CommandType = CommandType.StoredProcedure;
                cmd.Parameters.Add(new SqlParameter("@ApplicationName", Membership.ApplicationName));
                cmd.Parameters.Add(new SqlParameter("@UserName", username));
                cmd.Parameters.Add(new SqlParameter("@PPID", ppid));
                cmd.ExecuteNonQuery();
            }
        }

        /// <summary>
        /// Get's a user's PPIDs
        /// </summary>
        /// <param name="username">the username of the account</param>
        /// <returns>a list of PPIDs</returns>
        public static List<string> GetUsersPPIDs(string username)
        {
            List<string> result = new List<string>();
            using (SqlConnection connection = new SqlConnection(ConfigurationManager.ConnectionStrings["LocalSqlServer"].ConnectionString))
            {
                connection.Open();

                SqlCommand cmd = new SqlCommand("dbo.aspnet_InformationCards_FindAllPPIDForUser", connection);
                cmd.CommandType = CommandType.StoredProcedure;
                cmd.Parameters.Add(new SqlParameter("@ApplicationName", Membership.ApplicationName));
                cmd.Parameters.Add(new SqlParameter("@UserName", username));
                SqlDataReader reader = cmd.ExecuteReader();
                while (reader.Read())
                {
                    string data = (reader.IsDBNull(reader.GetOrdinal("PPID"))) ? null : reader["PPID"].ToString();
                    if (!string.IsNullOrEmpty(data))
                    {
                        result.Add(data);
                    }
                }
                return result;
            }
        }

        /// <summary>
        /// Gets either the value in the collection or a empty string if the key doens't exist
        /// </summary>
        /// <param name="collection"></param>
        /// <param name="key"></param>
        /// <returns></returns>
        private static string GetValueOrEmptyString( Dictionary<string,string> collection , string key )
        {
            string result = string.Empty;
            collection.TryGetValue(key, out result);
            return result;
        }

        /// <summary>
        /// Checks if the username is free in the account
        /// </summary>
        /// <param name="username">the username</param>
        /// <returns>true if the name is not taken</returns>
        public static bool IsUsernameAvailable(string username)
        {
            return (Membership.FindUsersByName(username).Count == 0);
        }

        /// <summary>
        /// Gets an availible username by adding digits to a given username if required.
        /// </summary>
        /// <param name="username">the proposed username</param>
        /// <returns>a unused username</returns>
        public static string NextAvailableUsername(string username)
        {
            string result = username;
            int n = 1;

            while (!IsUsernameAvailable(result))
                result = string.Format("{0}{1}", username, n++);

            return result;
        }


        /// <summary>
        /// This Gets a unique username, guessing based off of the first name, last name and the email address.
        /// 
        /// NOTE: 
        ///     This is an EXAMPLE of how to autogenerate a username for the user.
        ///     Your application may wish a better algorithm, or better, allow the user to choose a name  
        ///     This is not Perscriptive Guidance, just an option
        /// 
        /// </summary>
        /// <param name="first">First Name</param>
        /// <param name="second">Last Name</param>
        /// <param name="email">Email Address</param>
        /// <returns>an unused username</returns>
        public static string GetUniqueNewUserName(string first, string second, string email)
        {
            string newname;
            string bestSoFar = "";

            // first try email before the @
            if (!string.IsNullOrEmpty(email) && email.IndexOf('@') > 0)
            {
                newname = email.Substring(0, email.IndexOf('@'));
                if (IsUsernameAvailable(newname))
                    return newname;
                bestSoFar = NextAvailableUsername(newname);
            }

            // first
            if (!string.IsNullOrEmpty(first))
            {
                newname = first;
                if (IsUsernameAvailable(newname))
                    return newname;
                if (string.IsNullOrEmpty(bestSoFar))
                    bestSoFar = NextAvailableUsername(newname);
            }

            // last
            if (!string.IsNullOrEmpty(second))
            {
                newname = second;
                if (IsUsernameAvailable(newname))
                    return newname;
                if (string.IsNullOrEmpty(bestSoFar))
                    bestSoFar = NextAvailableUsername(newname);
            }

            // first.last
            if (!string.IsNullOrEmpty(first) && !string.IsNullOrEmpty(second))
            {
                newname = string.Format("{0}.{1}", first, second);
                if (IsUsernameAvailable(newname))
                    return newname;
                if (string.IsNullOrEmpty(bestSoFar))
                    bestSoFar = NextAvailableUsername(newname);
            }

            // flast
            if (!string.IsNullOrEmpty(first) && !string.IsNullOrEmpty(second))
            {
                newname = first[0] + second;
                if (IsUsernameAvailable(newname))
                    return newname;
                if (string.IsNullOrEmpty(bestSoFar))
                    bestSoFar = NextAvailableUsername(newname);
            }

            // NewUser
            if (string.IsNullOrEmpty(bestSoFar))
            {
                newname = "NewUser";
                if (IsUsernameAvailable(newname))
                    return newname;
                if (string.IsNullOrEmpty(bestSoFar))
                    bestSoFar = NextAvailableUsername(newname);
            }
            return bestSoFar;
        }

        private static string cardValue(Dictionary<string, string> card, string key)
        {
            return card.ContainsKey(key) ? card[key] : string.Empty;
        }
    }
}