using System;
using System.Collections.Generic;
using System.IO;
using System.Text;

[assembly: CLSCompliant(true)]
namespace nl.telin.authep.lib
{
    /// <summary>
    /// Possible values for a person's gender. Integer values correspond to section 5.5.3 of ISO 19794-5.
    /// </summary>
    public enum Gender { 
        /// <summary>
        /// Gender not specified.
        /// </summary>
        UNSPECIFIED=0,
        /// <summary>
        /// Gender is male.
        /// </summary>
        MALE=1,
        /// <summary>
        /// Gender is female.
        /// </summary>
        FEMALE=2,
        /// <summary>
        /// Gender is unknown.
        /// </summary>
        UNKNOWN=3}

    /// <summary>
    /// Data structure for storing the MRZ information as found in DG1. Based on ICAO Doc 9303 part 1 and 3.
    /// </summary>
    public class MRZInfo
    {
        private static bool ArrayEquals(System.Array array1, System.Array array2)
        {
            bool result = false;
            if ((array1 == null) && (array2 == null))
                result = true;
            else if ((array1 != null) && (array2 != null))
            {
                if (array1.Length == array2.Length)
                {
                    int length = array1.Length;
                    result = true;
                    for (int index = 0; index < length; index++)
                    {
                        if (!(array1.GetValue(index).Equals(array2.GetValue(index))))
                        {
                            result = false;
                            break;
                        }
                    }
                }
            }
            return result;
        }

        private static sbyte[] ConvertByteArray(byte[] data)
        {
            sbyte[] result = new sbyte[data.Length];

            for (int i = 0; i < data.Length; ++i)
                result[i] = (sbyte)data[i];

            return result;
        }

        private static byte[] ConvertByteArray(sbyte[] data)
        {
            byte[] result = new byte[data.Length];

            for (int i = 0; i < data.Length; ++i)
                result[i] = (byte)data[i];

            return result;
        }

        /// <summary>
        /// Unspecified document type (do not use, choose ID1 or ID3).
        /// </summary>
	    public const int DOC_TYPE_UNSPECIFIED = 0;
	    /// <summary>
        /// ID1 document type for credit card sized national identity cards.
	    /// </summary>
	    public const int DOC_TYPE_ID1 = 1;
	    /// <summary>
        /// ID2 document type.
	    /// </summary>
	    public const int DOC_TYPE_ID2 = 2;
	    /// <summary>
        /// ID3 document type for passport booklets.
	    /// </summary>
	    public const int DOC_TYPE_ID3 = 3;                           

	private const string MRZ_CHARS = "<0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ";

	private const string SDF = "yyMMdd";

	private int documentType;
	private Country issuingState;
	private string primaryIdentifier;
	private string[] secondaryIdentifiers;
    private Country nationality;
	private string documentNumber;
	private string personalNumber;
	private DateTime dateOfBirth;
	private Gender gender;
    private DateTime dateOfExpiry;
	private char documentNumberCheckDigit;
	private char dateOfBirthCheckDigit;
	private char dateOfExpiryCheckDigit;
	private char personalNumberCheckDigit;
	private char compositeCheckDigit;
	private string optionalData2; // FIXME: Last field on line 2 of ID3 MRZ.

	/// <summary>
    /// Constructs a new MRZ.
	/// </summary>
    /// <param name="documentType">document type</param>
	/// <param name="issuingState">issuing state</param>
	/// <param name="primaryIdentifier">card holder name</param>
	/// <param name="secondaryIdentifiers">card holder name</param>
	/// <param name="documentNumber">document number</param>
    /// <param name="nationality">nationality</param>
	/// <param name="dateOfBirth">date of birth</param>
	/// <param name="gender">gender</param>
    /// <param name="dateOfExpiry">date of expiry</param>
	/// <param name="personalNumber">personal number</param>
    public MRZInfo(int documentType, Country issuingState,
			string primaryIdentifier, string[] secondaryIdentifiers,
            string documentNumber, Country nationality, DateTime dateOfBirth,
            Gender gender, DateTime dateOfExpiry, string personalNumber)
    {
		this.documentType = documentType;
		this.issuingState = issuingState;
		this.primaryIdentifier = primaryIdentifier;
		this.secondaryIdentifiers = secondaryIdentifiers;
		this.documentNumber = documentNumber;
		this.nationality = nationality; 
		this.dateOfBirth = dateOfBirth;
		this.gender = gender;
		this.dateOfExpiry = dateOfExpiry;
		this.personalNumber = personalNumber;
		if (documentType == DOC_TYPE_ID1) {
			this.optionalData2 = "<<<<<<<<<<<";
		}
		checkDigit();
	}

	/// <summary>
    /// Constructs a new MRZ.
	/// </summary>
    /// <param name="stream">contains the contents of DG1 (without the tag and length)</param>
	public MRZInfo(Stream stream) {
		try {
			BinaryReader dataIn = new BinaryReader(stream);
			this.documentType = readDocumentType(dataIn);
			if (documentType == DOC_TYPE_ID1) {
				this.issuingState = readIssuingState(dataIn);
				this.documentNumber = readDocumentNumber(dataIn, 9);
                this.documentNumberCheckDigit = (char)dataIn.ReadByte();
				this.personalNumber = trimFillerChars(readPersonalNumber(dataIn, 14)); // (FIXED by hakan@elgin.nl) not 15 but 14 let control digit out of this read
                dataIn.ReadByte(); // MO: always '<'?
				this.personalNumberCheckDigit = checkDigit(personalNumber); // (Also: hakan@elgin.nl sugests to: read control digite of sofinumber instead.)
				this.dateOfBirth = readDateOfBirth(dataIn);
                this.dateOfBirthCheckDigit = (char)dataIn.ReadByte();
				this.gender = readGender(dataIn);
				this.dateOfExpiry = readDateOfExpiry(dataIn);
                this.dateOfExpiryCheckDigit = (char)dataIn.ReadByte();
				this.nationality = readNationality(dataIn);
				byte[] optionalData2Bytes = new byte[11];
                dataIn.Read(optionalData2Bytes, 0, optionalData2Bytes.Length);
				this.optionalData2 = UTF8Encoding.UTF8.GetString(optionalData2Bytes);
                this.compositeCheckDigit = (char)dataIn.ReadByte();
				string name = readName(dataIn, 30);
				processNameIdentifiers(name);
			} else {
				/* Assume it's a ID3 document */
				this.issuingState = readIssuingState(dataIn);
				string name = readName(dataIn, 39);
				processNameIdentifiers(name);
				this.documentNumber = readDocumentNumber(dataIn, 9);
                this.documentNumberCheckDigit = (char)dataIn.ReadByte();
				this.nationality = readNationality(dataIn);
				this.dateOfBirth = readDateOfBirth(dataIn);
                this.dateOfBirthCheckDigit = (char)dataIn.ReadByte();
				this.gender = readGender(dataIn);
				this.dateOfExpiry = readDateOfExpiry(dataIn);
                this.dateOfExpiryCheckDigit = (char)dataIn.ReadByte();
				this.personalNumber = trimFillerChars(readPersonalNumber(dataIn, 14));
                this.personalNumberCheckDigit = (char)dataIn.ReadByte();
                this.compositeCheckDigit = (char)dataIn.ReadByte();
			}
		} catch (IOException ioe) {
            Console.WriteLine(ioe.StackTrace);
			throw new ArgumentException("Invalid MRZ input source");
		}
	}

	private void processNameIdentifiers(string mrzNameString) {
        int delimIndex = mrzNameString.IndexOf("<<");
		if (delimIndex < 0) {
			throw new ArgumentException("Input does not contain primary identifier!");
		}
        primaryIdentifier = mrzNameString.Substring(0, (delimIndex) - (0));
        string rest = mrzNameString.Substring(mrzNameString.IndexOf("<<") + 2);
		processSecondaryIdentifiers(rest);
	}

	private void processSecondaryIdentifiers(string secondaryIdentifiersString) {
		Tokenizer st = new Tokenizer(secondaryIdentifiersString, "<");
        List<string> result = new List<string>();
		while (st.HasMoreTokens()) {
			string identifier = st.NextToken();
			if (identifier != null && identifier.Length > 0) {
				result.Add(identifier);
			}
		}
		secondaryIdentifiers = result.ToArray();
	}

	private static string trimFillerChars(string str) {
		byte[] chars = UTF8Encoding.UTF8.GetBytes(str.Trim());
		for (int i = 0; i < chars.Length; i++) {
			if (chars[i] == '<') { chars[i] = (byte) ' '; }
		}
        return UTF8Encoding.UTF8.GetString(chars).Trim();
	}

        /// <summary>
        /// Gets this MRZInfo in a byte encoded format.
        /// </summary>
        /// <returns>Byte encoded MRZInfo</returns>
	public byte[] getEncoded() {
		try {
            MemoryStream stream = new MemoryStream();
			BinaryWriter dataOut = new BinaryWriter(stream);
			writeDocumentType(dataOut);
			if (documentType == DOC_TYPE_ID1) {
				/* Assume it's an ID1 document */
				writeIssuingState(dataOut);
				writeDocumentNumber(dataOut, 9); /* FIXME: max size of field */
				dataOut.Write(documentNumberCheckDigit);
				writePersonalNumber(dataOut, 14); /* FIXME: max size of field */
				dataOut.Write('<'); // FIXME: correct? Some people suggested checkDigit(personalNumber)...
				writeDateOfBirth(dataOut);
				dataOut.Write(dateOfBirthCheckDigit);
				writeGender(dataOut);
				writeDateOfExpiry(dataOut);
				dataOut.Write(dateOfExpiryCheckDigit);
				writeNationality(dataOut);
				dataOut.Write( UTF8Encoding.UTF8.GetBytes(optionalData2)); // TODO: Understand this...
				dataOut.Write(compositeCheckDigit);
				writeName(dataOut, 30);
			} else {
				/* Assume it's a ID3 document */
				writeIssuingState(dataOut);
				writeName(dataOut, 39);
				writeDocumentNumber(dataOut, 9);
                dataOut.Write(documentNumberCheckDigit);
				writeNationality(dataOut);
				writeDateOfBirth(dataOut);
                dataOut.Write(dateOfBirthCheckDigit);
				writeGender(dataOut);
				writeDateOfExpiry(dataOut);
                dataOut.Write(dateOfExpiryCheckDigit);
				writePersonalNumber(dataOut, 14); /* FIXME: max size of field */
                dataOut.Write(personalNumberCheckDigit);
                dataOut.Write(compositeCheckDigit);
			}
            byte[] result = stream.GetBuffer();
			dataOut.Close();
			return result;
		} catch (IOException ioe) {
            Console.WriteLine(ioe.StackTrace);
			return null;
		}
	}

	private void writeIssuingState(BinaryWriter dataOut) {
        dataOut.Write(UTF8Encoding.UTF8.GetBytes(issuingState.toAlpha3Code()));
	}

    private void writePersonalNumber(BinaryWriter dataOut, int width)
    {
        dataOut.Write(UTF8Encoding.UTF8.GetBytes(mrzFormat(personalNumber, width)));  
	}

    private void writeDateOfExpiry(BinaryWriter dataOut)
    {
        dataOut.Write(UTF8Encoding.UTF8.GetBytes(dateOfExpiry.ToString(SDF)));
	}

    private void writeGender(BinaryWriter dataOut)
    {
        dataOut.Write(UTF8Encoding.UTF8.GetBytes(genderToString()));
	}

    private void writeDateOfBirth(BinaryWriter dataOut)
    {
        dataOut.Write(UTF8Encoding.UTF8.GetBytes(dateOfBirth.ToString(SDF)));
	}

    private void writeNationality(BinaryWriter dataOut)
    {
        dataOut.Write(UTF8Encoding.UTF8.GetBytes(nationality.toAlpha3Code()));
	}

    private void writeDocumentNumber(BinaryWriter dataOut, int width)
    {
        dataOut.Write(UTF8Encoding.UTF8.GetBytes(mrzFormat(documentNumber,width)));
	}

    private void writeName(BinaryWriter dataOut, int width)
    {
        dataOut.Write(UTF8Encoding.UTF8.GetBytes(nameToString(width)));
	}

    private void writeDocumentType(BinaryWriter dataOut)
    {
        dataOut.Write(UTF8Encoding.UTF8.GetBytes(documentTypeToString()));
	}

	private string documentTypeToString() {
		switch (documentType) {
		case DOC_TYPE_ID1: return "I<";
		case DOC_TYPE_ID2: return "P<";
		case DOC_TYPE_ID3: return "P<";
		default: return "P<";
		}
	}

	private string genderToString() {
		switch (gender) {
		case Gender.MALE: return "M";
		case Gender.FEMALE: return "F";
		default: return "<";
		}
	}

	private string nameToString(int width) {
        StringBuilder name = new StringBuilder();
		name.Append(primaryIdentifier);
		name.Append("<");
		for (int i = 0; i < secondaryIdentifiers.Length; i++) {
			name.Append("<");
			name.Append(secondaryIdentifiers[i]);
		}
		return mrzFormat(name.ToString(), width);
	}


        /// <summary>
        /// Reads the type of document.
        /// ICAO Doc 9303 part 1 gives an example.
        /// </summary>
        /// <param name="reader">reader to read from</param>
        /// <returns>a string of length 2 containing the document type</returns>
	    private int readDocumentType(BinaryReader reader) {
		    byte[] docTypeBytes = new byte[2];
            reader.Read(docTypeBytes,0,docTypeBytes.Length);
            string docTypeStr = UTF8Encoding.UTF8.GetString(docTypeBytes);
		    if (docTypeStr.StartsWith("A") || docTypeStr.StartsWith("C") || docTypeStr.StartsWith("I")) {
			    return DOC_TYPE_ID1;
		    } else if (docTypeStr.StartsWith("P")) {
			    return DOC_TYPE_ID3;
		    }
		    return DOC_TYPE_UNSPECIFIED;
	    }

        /// <summary>
        /// Reads the issuing state as a three letter string.
        /// </summary>
        /// <param name="reader">input source</param>
        /// <returns>a string of length 3 containing an abbreviation of the issuing state or organization</returns>
	    private Country readIssuingState(BinaryReader reader){
		    byte[] data = new byte[3];
            reader.Read(data, 0, data.Length);
		    return Country.getInstance(UTF8Encoding.UTF8.GetString(data) );
	    }

        /// <summary>
        /// Reads the passport holder's name, including &lt; characters.
        /// </summary>
        /// <param name="reader">input source</param>
        /// <param name="le">maximal length</param>
        /// <returns>a string containing last name and first names seperated by spaces</returns>
	    private string readName(BinaryReader reader, int le) {
		    byte[] data = new byte[le];
            reader.Read(data,0,data.Length);
    //		for (int i = 0; i < data.length; i++) {
    //		if (data[i] == '<') {
    //		data[i] = ' ';
    //		}
    //		}
            string name = UTF8Encoding.UTF8.GetString(data).Trim();
		    return name;
	    }

        /// <summary>
        /// Reads the document number.
        /// </summary>
        /// <param name="reader">input source</param>
        /// <param name="le">maximal length</param>
        /// <returns>the document number</returns>
	    private string readDocumentNumber(BinaryReader reader, int le) {
		    byte[] data = new byte[le];
		    reader.Read(data,0,data.Length);
		    return UTF8Encoding.UTF8.GetString(data).Trim();
	    }

	
        /// <summary>
        /// Reads the personal number of the passport holder (or other optional data).
        /// </summary>
        /// <param name="reader">input source</param>
        /// <param name="le">maximal length</param>
        /// <returns></returns>
	    private string readPersonalNumber(BinaryReader reader, int le) {
		    byte[] data = new byte[le];
		    reader.Read(data,0,data.Length);
		    return trimFillerChars(UTF8Encoding.UTF8.GetString(data));
	    }

        /// <summary>
        /// Reads the nationality of the passport holder.
        /// </summary>
        /// <param name="reader">input source</param>
        /// <returns>a string of length 3 containing the nationality of the passport holder</returns>
	    private Country readNationality(BinaryReader reader) {
		    byte[] data = new byte[3];
		    reader.Read(data,0,data.Length);
            return Country.getInstance(UTF8Encoding.UTF8.GetString(data).Trim());
	    }

        /// <summary>
        /// Reads the 1 letter gender information.
        /// </summary>
        /// <param name="reader">input source</param>
        /// <returns>the gender of the passport holder</returns>
	    private Gender readGender(BinaryReader reader) {
		    byte[] data = new byte[1];
		    reader.Read(data,0,data.Length);
            string genderStr = UTF8Encoding.UTF8.GetString(data).Trim();
		    if (genderStr.Equals("M",StringComparison.CurrentCultureIgnoreCase)) {
			    return Gender.MALE;
		    }
		    if (genderStr.Equals("F",StringComparison.CurrentCultureIgnoreCase)) {
			    return Gender.FEMALE;
		    }
		    return Gender.UNKNOWN;
	    }

        /// <summary>
        /// Reads the date of birth of the passport holder. Base year is 1900.
        /// </summary>
        /// <param name="reader">input source</param>
        /// <returns>the date of birth</returns>
        private DateTime readDateOfBirth(BinaryReader reader)
        {
		    byte[] data = new byte[6];
		    reader.Read(data,0,data.Length);
            string datestring = UTF8Encoding.UTF8.GetString(data).Trim();
		    return parseDate(1900, datestring);
	    }

        /// <summary>
        /// Reads the date of expiry of this document. Base year is 2000.
        /// </summary>
        /// <param name="reader">input source</param>
        /// <returns>the date of expiry</returns>
    private DateTime readDateOfExpiry(BinaryReader reader)
    {
		byte[] data = new byte[6];
		reader.Read(data,0,data.Length);
        return parseDate(2000, UTF8Encoding.UTF8.GetString(data).Trim());
	}

    private static DateTime parseDate(int baseYear, string datestring)
    {
		if (datestring.Length != 6) {
			throw new FormatException("Wrong date format!");
		}
        int year = baseYear + int.Parse(datestring.Substring(0, 2));
        int month = int.Parse(datestring.Substring(2, 2));
		int day = int.Parse(datestring.Substring(4, 2));

        return new DateTime(year, month, day); // TODO: does month need -1 ?? (see java sources)
	}

    /// <summary>
    /// Gets the date of birth of the passport holder.
    /// </summary>
    /// <returns>date of birth (with 1900 as base year)</returns>
    public DateTime getDateOfBirth()
    {
	    return dateOfBirth;
    }

    /// <summary>
    /// Sets the date of birth of the passport holder.
    /// </summary>
    /// <param name="dateOfBirth">New date of birth.</param>
	public void setDateOfBirth(DateTime dateOfBirth) {
		this.dateOfBirth = dateOfBirth;
		checkDigit();
	}

    /// <summary>
    /// Gets the date of expiry
    /// </summary>
    /// <returns>date of expiry (with 2000 as base year)</returns>
	public DateTime getDateOfExpiry() {
		return dateOfExpiry;
	}

    /// <summary>
    /// Set the date of expiry.
    /// </summary>
    /// <param name="dateOfExpiry">New date of expiry</param>
	public void setDateOfExpiry(DateTime dateOfExpiry) {
		this.dateOfExpiry = dateOfExpiry;
		checkDigit();
	}

    /// <summary>
    /// Gets the document number.
    /// </summary>
    /// <returns>The document number.</returns>
	public string getDocumentNumber() {
		return documentNumber;
	}

    /// <summary>
    /// Set the document number.
    /// </summary>
    /// <param name="documentNumber">The new document number</param>
	public void setDocumentNumber(string documentNumber) {
		this.documentNumber = documentNumber.Trim();
		checkDigit();
	}

    /// <summary>
    /// Gets the document type.
    /// </summary>
    /// <returns>The document type.</returns>
	public int getDocumentType() {
		return documentType;
	}

    /// <summary>
    /// Gets the issuing state.
    /// </summary>
    /// <returns>The issuing state.</returns>
    public Country getIssuingState()
    {
		return issuingState;
	}

    /// <summary>
    /// Set the issuing state.
    /// </summary>
    /// <param name="issuingState">New issuing state.</param>
    public void setIssuingState(Country issuingState)
    {
		this.issuingState = issuingState;
		checkDigit();
	}
	
    /// <summary>
    /// Gets the passport holder's last name.
    /// </summary>
    /// <returns>Last name.</returns>
	public string getPrimaryIdentifier() {
		return primaryIdentifier;
	}

    /// <summary>
    /// Set the passport holder's last name.
    /// </summary>
    /// <param name="primaryIdentifier">New last name.</param>
	public void setPrimaryIdentifier(string primaryIdentifier) {
		this.primaryIdentifier = primaryIdentifier.Trim();
		checkDigit();
	}
        
    /// <summary>
    /// Gets the passport holder's first names.
    /// </summary>
    /// <returns>First names.</returns>
	public string[] getSecondaryIdentifiers() {
		return secondaryIdentifiers;
	}

    /// <summary>
    /// Set the passport holder's first names.
    /// </summary>
    /// <param name="secondaryIdentifiers">New first names.</param>
	public void setSecondaryIdentifiers(string[] secondaryIdentifiers) {
		if (secondaryIdentifiers == null) {
			this.secondaryIdentifiers = null;
		} else {
			this.secondaryIdentifiers = new string[secondaryIdentifiers.Length];
            Array.Copy(secondaryIdentifiers, this.secondaryIdentifiers, secondaryIdentifiers.Length);
		}
		checkDigit();
	}

    /// <summary>
    /// Set the passport holder's first name.
    /// </summary>
    /// <param name="secondaryIdentifiers">New first name.</param>
	public void setSecondaryIdentifiers(string secondaryIdentifiers) {
		processSecondaryIdentifiers(secondaryIdentifiers.Trim());
		checkDigit();
	}

    /// <summary>
    /// Gets the passport holder's nationality.
    /// </summary>
    /// <returns>A country.</returns>
    public Country getNationality()
    {
		return nationality;
	}

    /// <summary>
    /// Set the passport holder's nationality.
    /// </summary>
    /// <param name="nationality">New nationality.</param>
    public void setNationality(Country nationality)
    {
		this.nationality = nationality;
		checkDigit();
	}

    /// <summary>
    /// Gets the personal number.
    /// </summary>
    /// <returns>Personal number.</returns>
	public string getPersonalNumber() {
		return personalNumber;
	}

    /// <summary>
    /// Set the personal number.
    /// </summary>
    /// <param name="personalNumber">Personal number.</param>
	public void setPersonalNumber(string personalNumber) {
		this.personalNumber = trimFillerChars(personalNumber);
		checkDigit();
	}

    /// <summary>
    /// Gets the passport holder's gender.
    /// </summary>
    /// <returns>Gender.</returns>
	public Gender getGender() {
		return gender;
	}

    /// <summary>
    /// Set the passport holder's gender.
    /// </summary>
    /// <param name="gender">New gender.</param>
	public void setGender(Gender gender) {
		this.gender = gender;
		checkDigit();
	}

    /// <summary>
    /// Creates a textual representation of this MRZ. This is the 2 or 3 line representation
    ///(depending on the document type) as it appears in the document. All lines end in
    /// a newline char.
    /// </summary>
    /// <returns>The MRZ as text.</returns>
	public override string ToString() {
        StringBuilder result = new StringBuilder();
		if (documentType == DOC_TYPE_ID1) {
			/* 
			 * FIXME: some composite check digit
			 *        should go into this one as well...
			 */
			result.Append(documentTypeToString());
            result.Append(issuingState.toAlpha3Code());
            result.Append(documentNumber);
            result.Append(documentNumberCheckDigit);
            result.Append(mrzFormat(personalNumber, 14));
            result.Append("<"); // FIXME: not sure... maybe check digit?
			// out.append(checkDigit(personalNumber));
            result.Append("\n");
            result.Append(dateOfBirth.ToString(SDF));
            result.Append(dateOfBirthCheckDigit);
            result.Append(genderToString());
            result.Append(dateOfExpiry.ToString(SDF));
            result.Append(dateOfExpiryCheckDigit);
            result.Append(nationality.toAlpha3Code());
            result.Append(optionalData2);
            result.Append(compositeCheckDigit); // should be: upper + middle line?
            result.Append("\n");
            result.Append(nameToString(30));
            result.Append("\n");
		} else {
            result.Append(documentTypeToString());
            result.Append(issuingState.toAlpha3Code());
            result.Append(nameToString(39));
            result.Append("\n");
            result.Append(documentNumber);
            result.Append(documentNumberCheckDigit);
            result.Append(nationality.toAlpha3Code());
            result.Append(dateOfBirth.ToString(SDF));
            result.Append(dateOfBirthCheckDigit);
            result.Append(genderToString());
            result.Append(dateOfExpiry.ToString(SDF));
            result.Append(dateOfExpiryCheckDigit);
            result.Append(mrzFormat(personalNumber, 14));
            result.Append(personalNumberCheckDigit);
            result.Append(compositeCheckDigit);
            result.Append("\n");
		}
        return result.ToString();
	}

        /// <summary>
        /// Compare this instance of MRZInfo with another object.
        /// </summary>
        /// <param name="obj">Object to compare with</param>
        /// <returns>True if this instance is equal to the given object or else false.</returns>
	public override bool Equals(Object obj) {
		if (obj == this) { return true; }
		if (obj == null) { return false; }
		if (!(obj.GetType().Equals(this.GetType()))) { return false; }
		MRZInfo other = (MRZInfo)obj;
		if (documentType != other.documentType) { return false; }
		if (!issuingState.Equals(other.issuingState)) { return false; }
		if (!primaryIdentifier.Equals(other.primaryIdentifier)) { return false; }
		if (!ArrayEquals(secondaryIdentifiers, other.secondaryIdentifiers)) { return false; }
		if (!nationality.Equals(other.nationality)) { return false; }
		if (!documentNumber.Equals(other.documentNumber)) { return false; }
		if (!personalNumber.Equals(other.personalNumber)) { return false; }
		if (!dateOfBirth.Equals(other.dateOfBirth)) { return false; }
		if (gender != other.gender) { return false; }
		if (!dateOfExpiry.Equals(other.dateOfExpiry)) { return false; }
		return true;
	}

        /// <summary>
        /// Serves as a hash function for a particular type.
        /// </summary>
        /// <returns>hash code for this object.</returns>
    public override int GetHashCode()
    {
        return base.GetHashCode();
    }
	
	private void checkDigit() {
		this.documentNumberCheckDigit = checkDigit(documentNumber);
		this.dateOfBirthCheckDigit = checkDigit(dateOfBirth.ToString());
		this.dateOfExpiryCheckDigit = checkDigit(dateOfExpiry.ToString());
		this.personalNumberCheckDigit = checkDigit(mrzFormat(personalNumber, 14));
		StringBuilder composite = new StringBuilder();
		if (documentType == DOC_TYPE_ID1) {
			// TODO: Include: 6-30 (upper line), 1-7,9-15,19-29 (middle line)
			// composite.append(documentTypeToString());
			// composite.append(issuingState);
			composite.Append(documentNumber);
            composite.Append(documentNumberCheckDigit);
            composite.Append(mrzFormat(personalNumber, 15));
            composite.Append(dateOfBirth.ToString(SDF));
            composite.Append(dateOfBirthCheckDigit);
            composite.Append(dateOfExpiry.ToString(SDF));
            composite.Append(dateOfExpiryCheckDigit);
            composite.Append(optionalData2);
		} else {
            composite.Append(documentNumber);
            composite.Append(documentNumberCheckDigit);
            composite.Append(dateOfBirth.ToString(SDF));
            composite.Append(dateOfBirthCheckDigit);
            composite.Append(dateOfExpiry.ToString(SDF));
            composite.Append(dateOfExpiryCheckDigit);
            composite.Append(mrzFormat(personalNumber, 14));
            composite.Append(personalNumberCheckDigit);
		}
		this.compositeCheckDigit = checkDigit(composite.ToString());
	}

    /// <summary>
    /// Reformats the input string such that it only contains valid MRZ characters.
    /// </summary>
    /// <param name="str">the input string</param>
    /// <param name="width">the (minimal) width of the result</param>
    /// <returns>the reformatted string</returns>
	private static string mrzFormat(string str, int width) {
		str = str.ToUpper().Trim();
        StringBuilder result = new StringBuilder();
		for (int i = 0; i < str.Length; i++) {
            char c = str[i];
			if (MRZ_CHARS.IndexOf(c) == -1) {
				result.Append('<');
			} else {
                result.Append(c);
			}  
		}
		while (result.Length < width) {
			result.Append("<");
		}
		return result.ToString();
	}

    /// <summary>
    /// Computes the 7-3-1 check digit for part of the MRZ.
    /// </summary>
    /// <param name="str">a part of the MRZ.</param>
    /// <returns>the resulting check digit.</returns>
	public static char checkDigit(string str) {
		try {
            byte[] chars = UTF8Encoding.UTF8.GetBytes(str);
			int[] weights = { 7, 3, 1 };
			int result = 0;
			for (int i = 0; i < chars.Length; i++) {
				result = (result + weights[i % 3] * decodeMRZDigit(chars[i])) % 10;
			}
            chars = UTF8Encoding.UTF8.GetBytes(result.ToString());
			return (char)chars[0];
		} catch (Exception e) {
            Console.WriteLine(e.StackTrace);
			throw new ArgumentException(e.ToString());
		}
	}

    /// <summary>
    ///  Looks up the numerical value for MRZ characters. In order to be able to compute check digits.
    /// </summary>
    /// <param name="ch">a character from the MRZ.</param>
    /// <returns>the numerical value of the character.</returns>
	private static int decodeMRZDigit(byte ch) {
		switch ((char) ch) {
		case '<':
		case '0': return 0; case '1': return 1; case '2': return 2;
		case '3': return 3; case '4': return 4; case '5': return 5;
		case '6': return 6; case '7': return 7; case '8': return 8;
		case '9': return 9;
		case 'a': case 'A': return 10; case 'b': case 'B': return 11;
		case 'c': case 'C': return 12; case 'd': case 'D': return 13;
		case 'e': case 'E': return 14; case 'f': case 'F': return 15;
		case 'g': case 'G': return 16; case 'h': case 'H': return 17;
		case 'i': case 'I': return 18; case 'j': case 'J': return 19;
		case 'k': case 'K': return 20; case 'l': case 'L': return 21;
		case 'm': case 'M': return 22; case 'n': case 'N': return 23;
		case 'o': case 'O': return 24; case 'p': case 'P': return 25;
		case 'q': case 'Q': return 26; case 'r': case 'R': return 27;
		case 's': case 'S': return 28; case 't': case 'T': return 29;
		case 'u': case 'U': return 30; case 'v': case 'V': return 31;
		case 'w': case 'W': return 32; case 'x': case 'X': return 33;
		case 'y': case 'Y': return 34; case 'z': case 'Z': return 35;
		default:
			throw new FormatException("Could not decode MRZ character "
					+ ch + " ('" + ((char)ch).ToString() + "')");
		}
	}
    }
}
