using System;
using System.Collections.Generic;
using System.Text;
using System.Runtime.InteropServices;

namespace nl.telin.authep.managedcardwriter
{
    /// <summary>
    /// Defines a section in an ini file.
    /// </summary>
    public class Section
    {
        internal Key[] keys;
        internal string name;
        /// <summary>
        /// Create an instance of <b>Section</b>
        /// </summary>
        /// <param name="sectionName">name of the section</param>
        /// <param name="keys">keys that can be used in the section</param>
        public Section( string sectionName,  Key[] keys)
        {
            this.keys = keys;
            name = sectionName;
        }

        /// <summary>
        /// Get the key of section given an index.
        /// </summary>
        /// <param name="index">Index of the section to get.</param>
        /// <returns>Key of the indexed section.</returns>
        public Key this[string index]
        {
            get
            {
                foreach (Key k in keys)
                    if (k.name.Equals(index, StringComparison.CurrentCultureIgnoreCase))
                        return k;
                return new Key("","");
            }
        }
    }

    /// <summary>
    /// Defines a key in an ini file.
    /// </summary>
    public class Key
    {
        internal string name;
        internal string value;
        /// <summary>
        /// Craete a new instance of <b>Key</b>.
        /// </summary>
        /// <param name="keyName">Name of the key.</param>
        /// <param name="value">Value of the key.</param>
        public Key(string keyName, string value)
        {
            name = keyName;
            this.value = value;
        }
    }

    /// <summary>
    /// <b>IniFile</b> can read an inifile and make it's <see cref="Section"/>'s available.
    /// </summary>
    public class IniFile
    {
        [DllImport("kernel32", SetLastError = true)]
        private static extern int GetPrivateProfileString(string pSection, string pKey, string pDefault, byte[] prReturn, int pBufferLen, string pFile);

        internal Section[] sections;

        /// <summary>
        /// Create a new instance of <b>IniFile</b>.
        /// </summary>
        /// <param name="fileName">Path to the inifile to read.</param>
        public IniFile(string fileName)
        {
            if (!System.IO.File.Exists(fileName))
                throw new System.IO.FileNotFoundException("File " + fileName + " Not found");

            byte[] buffer = new byte[32767];
            int i = 0;
            string[] sTmp, kTmp;

            i = GetPrivateProfileString(null, null, null, buffer, 32767, fileName);
            if (i == 0)
            {
                sections = new Section[0];
                return;
            }

            sTmp = System.Text.Encoding.GetEncoding(1252).GetString(buffer, 0, i).TrimEnd((char)0).Split((char)0);
            sections = new Section[sTmp.Length];
            for( int j = 0; j< sTmp.Length ; j++)
            {
                i = GetPrivateProfileString(sTmp[j], null, null, buffer, 32767, fileName);
                kTmp = System.Text.Encoding.GetEncoding(1252).GetString(buffer, 0, i).TrimEnd((char)0).Split((char)0);
                Key[] keys = new Key[kTmp.Length];
                for( int k =0; k< kTmp.Length ; k++)
                {
                    i = GetPrivateProfileString(sTmp[j], kTmp[k], null, buffer, 32767, fileName);
                    keys[k] = new Key( kTmp[k] , System.Text.Encoding.GetEncoding(1252).GetString(buffer, 0, i).TrimEnd((char)0));
                }
                sections[j] = new Section(sTmp[j], keys);
            }
        }

        /// <summary>
        /// Get a particular section of this inifile by specifying an index.
        /// </summary>
        /// <param name="index">Index of the section to get.</param>
        /// <returns>A section.</returns>
        public Section this[string index]
        {
            get
            {
                foreach( Section s in sections )
                    if( s.name.Equals( index, StringComparison.CurrentCultureIgnoreCase ) )
                        return s;
                return new Section("", new Key[0]);
            }
        }
    }

}
