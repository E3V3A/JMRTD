/*
 * $Id: $
 */

package sos.data;

/**
 * ISO 3166 country codes.
 * Table based on Wikipedia information.
 * 
 * @author Martijn Oostdijk (martijn.oostdijk@gmail.com)
 */
public class Country
{
	public static final Country
	AD = new Country(0x020, "AD", "AND", "Andorra"),
	AE = new Country(0x784, "AE", "ARE", "United Arab Emirates"),
	AF = new Country(0x004, "AF", "AFG", "Afghanistan"),
	AG = new Country(0x028, "AG", "ATG", "Antigua and Barbuda"),
	AI = new Country(0x660, "AI", "AIA", "Anguilla"),
	AL = new Country(0x008, "AL", "ALB", "Albania"),
	AM = new Country(0x051, "AM", "ARM", "Armenia"),
	AN = new Country(0x530, "AN", "ANT", "Netherlands Antilles"),
	AO = new Country(0x024, "AO", "AGO", "Angola"),
	AQ = new Country(0x010, "AQ", "ATA", "Antarctica"),
	AR = new Country(0x032, "AR", "ARG", "Argentina"),
	AS = new Country(0x016, "AS", "ASM", "American Samoa"),
	AT = new Country(0x040, "AT", "AUT", "Austria"),
	AU = new Country(0x036, "AU", "AUS", "Australia"),
	AW = new Country(0x533, "AW", "ABW", "Aruba"),
	AX = new Country(0x248, "AX", "ALA", "Aland Islands"),
	AZ = new Country(0x031, "AZ", "AZE", "Azerbaijan"),
	BA = new Country(0x070, "BA", "BIH", "Bosnia and Herzegovina"),
	BB = new Country(0x052, "BB", "BRB", "Barbados"),
	BD = new Country(0x050, "BD", "BGD", "Bangladesh"),
	BE = new Country(0x056, "BE", "BEL", "Belgium"),
	BF = new Country(0x854, "BF", "BFA", "Burkina Faso"),
	BG = new Country(0x100, "BG", "BGR", "Bulgaria"),
	BH = new Country(0x048, "BH", "BHR", "Bahrain"),
	BI = new Country(0x108, "BI", "BDI", "Burundi"),
	BJ = new Country(0x204, "BJ", "BEN", "Benin"),
	BL = new Country(0x652, "BL", "BLM", "Saint Barthlemy"),
	BM = new Country(0x060, "BM", "BMU", "Bermuda"),
	BN = new Country(0x096, "BN", "BRN", "Brunei Darussalam"),
	BO = new Country(0x068, "BO", "BOL", "Bolivia"),
	BR = new Country(0x076, "BR", "BRA", "Brazil"),
	BS = new Country(0x044, "BS", "BHS", "Bahamas"),
	BT = new Country(0x064, "BT", "BTN", "Bhutan"),
	BV = new Country(0x074, "BV", "BVT", "Bouvet Island"),
	BW = new Country(0x072, "BW", "BWA", "Botswana"),
	BY = new Country(0x112, "BY", "BLR", "Belarus"),
	BZ = new Country(0x084, "BZ", "BLZ", "Belize"),
	CA = new Country(0x124, "CA", "CAN", "Canada"),
	CC = new Country(0x166, "CC", "CCK", "Cocos (Keeling) Islands"),
	CD = new Country(0x180, "CD", "COD", "Congo the Democratic Republic of the"),
	CF = new Country(0x140, "CF", "CAF", "Central African Republic"),
	CG = new Country(0x178, "CG", "COG", "Congo"),
	CH = new Country(0x756, "CH", "CHE", "Switzerland"),
	CI = new Country(0x384, "CI", "CIV", "Cote d'Ivoire"),
	CK = new Country(0x184, "CK", "COK", "Cook Islands"),
	CL = new Country(0x152, "CL", "CHL", "Chile"),
	CM = new Country(0x120, "CM", "CMR", "Cameroon"),
	CN = new Country(0x156, "CN", "CHN", "China"),
	CO = new Country(0x170, "CO", "COL", "Colombia"),
	CR = new Country(0x188, "CR", "CRI", "Costa Rica"),
	CU = new Country(0x192, "CU", "CUB", "Cuba"),
	CV = new Country(0x132, "CV", "CPV", "Cape Verde"),
	CX = new Country(0x162, "CX", "CXR", "Christmas Island"),
	CY = new Country(0x196, "CY", "CYP", "Cyprus"),
	CZ = new Country(0x203, "CZ", "CZE", "Czech Republic"),
	DE = new Country(0x276, "DE", "DEU", "Germany"),
	DJ = new Country(0x262, "DJ", "DJI", "Djibouti"),
	DK = new Country(0x208, "DK", "DNK", "Denmark"),
	DM = new Country(0x212, "DM", "DMA", "Dominica"),
	DO = new Country(0x214, "DO", "DOM", "Dominican Republic"),
	DZ = new Country(0x012, "DZ", "DZA", "Algeria"),
	EC = new Country(0x218, "EC", "ECU", "Ecuador"),
	EE = new Country(0x233, "EE", "EST", "Estonia"),
	EG = new Country(0x818, "EG", "EGY", "Egypt"),
	EH = new Country(0x732, "EH", "ESH", "Western Sahara"),
	ER = new Country(0x232, "ER", "ERI", "Eritrea"),
	ES = new Country(0x724, "ES", "ESP", "Spain"),
	ET = new Country(0x231, "ET", "ETH", "Ethiopia"),
	FI = new Country(0x246, "FI", "FIN", "Finland"),
	FJ = new Country(0x242, "FJ", "FJI", "Fiji"),
	FK = new Country(0x238, "FK", "FLK", "Falkland Islands (Malvinas)"),
	FM = new Country(0x583, "FM", "FSM", "Micronesia Federated States of"),
	FO = new Country(0x234, "FO", "FRO", "Faroe Islands"),
	FR = new Country(0x250, "FR", "FRA", "France"),
	GA = new Country(0x266, "GA", "GAB", "Gabon"),
	GB = new Country(0x826, "GB", "GBR", "United Kingdom"),
	GD = new Country(0x308, "GD", "GRD", "Grenada"),
	GE = new Country(0x268, "GE", "GEO", "Georgia"),
	GF = new Country(0x254, "GF", "GUF", "French Guiana"),
	GG = new Country(0x831, "GG", "GGY", "Guernsey"),
	GH = new Country(0x288, "GH", "GHA", "Ghana"),
	GI = new Country(0x292, "GI", "GIB", "Gibraltar"),
	GL = new Country(0x304, "GL", "GRL", "Greenland"),
	GM = new Country(0x270, "GM", "GMB", "Gambia"),
	GN = new Country(0x324, "GN", "GIN", "Guinea"),
	GP = new Country(0x312, "GP", "GLP", "Guadeloupe"),
	GQ = new Country(0x226, "GQ", "GNQ", "Equatorial Guinea"),
	GR = new Country(0x300, "GR", "GRC", "Greece"),
	GS = new Country(0x239, "GS", "SGS", "South Georgia and the South Sandwich Islands"),
	GT = new Country(0x320, "GT", "GTM", "Guatemala"),
	GU = new Country(0x316, "GU", "GUM", "Guam"),
	GW = new Country(0x624, "GW", "GNB", "Guinea-Bissau"),
	GY = new Country(0x328, "GY", "GUY", "Guyana"),
	HK = new Country(0x344, "HK", "HKG", "Hong Kong"),
	HM = new Country(0x334, "HM", "HMD", "Heard Island and McDonald Islands"),
	HN = new Country(0x340, "HN", "HND", "Honduras"),
	HR = new Country(0x191, "HR", "HRV", "Croatia"),
	HT = new Country(0x332, "HT", "HTI", "Haiti"),
	HU = new Country(0x348, "HU", "HUN", "Hungary"),
	ID = new Country(0x360, "ID", "IDN", "Indonesia"),
	IE = new Country(0x372, "IE", "IRL", "Ireland"),
	IL = new Country(0x376, "IL", "ISR", "Israel"),
	IM = new Country(0x833, "IM", "IMN", "Isle of Man"),
	IN = new Country(0x356, "IN", "IND", "India"),
	IO = new Country(0x086, "IO", "IOT", "British Indian Ocean Territory"),
	IQ = new Country(0x368, "IQ", "IRQ", "Iraq"),
	IR = new Country(0x364, "IR", "IRN", "Iran Islamic Republic of"),
	IS = new Country(0x352, "IS", "ISL", "Iceland"),
	IT = new Country(0x380, "IT", "ITA", "Italy"),
	JE = new Country(0x832, "JE", "JEY", "Jersey"),
	JM = new Country(0x388, "JM", "JAM", "Jamaica"),
	JO = new Country(0x400, "JO", "JOR", "Jordan"),
	JP = new Country(0x392, "JP", "JPN", "Japan"),
	KE = new Country(0x404, "KE", "KEN", "Kenya"),
	KG = new Country(0x417, "KG", "KGZ", "Kyrgyzstan"),
	KH = new Country(0x116, "KH", "KHM", "Cambodia"),
	KI = new Country(0x296, "KI", "KIR", "Kiribati"),
	KM = new Country(0x174, "KM", "COM", "Comoros"),
	KN = new Country(0x659, "KN", "KNA", "Saint Kitts and Nevis"),
	KP = new Country(0x408, "KP", "PRK", "Korea Democratic People's Republic of"),
	KR = new Country(0x410, "KR", "KOR", "Korea Republic of"),
	KW = new Country(0x414, "KW", "KWT", "Kuwait"),
	KY = new Country(0x136, "KY", "CYM", "Cayman Islands"),
	KZ = new Country(0x398, "KZ", "KAZ", "Kazakhstan"),
	LA = new Country(0x418, "LA", "LAO", "Lao People's Democratic Republic"),
	LB = new Country(0x422, "LB", "LBN", "Lebanon"),
	LC = new Country(0x662, "LC", "LCA", "Saint Lucia"),
	LI = new Country(0x438, "LI", "LIE", "Liechtenstein"),
	LK = new Country(0x144, "LK", "LKA", "Sri Lanka"),
	LR = new Country(0x430, "LR", "LBR", "Liberia"),
	LS = new Country(0x426, "LS", "LSO", "Lesotho"),
	LT = new Country(0x440, "LT", "LTU", "Lithuania"),
	LU = new Country(0x442, "LU", "LUX", "Luxembourg"),
	LV = new Country(0x428, "LV", "LVA", "Latvia"),
	LY = new Country(0x434, "LY", "LBY", "Libyan Arab Jamahiriya"),
	MA = new Country(0x504, "MA", "MAR", "Morocco"),
	MC = new Country(0x492, "MC", "MCO", "Monaco"),
	MD = new Country(0x498, "MD", "MDA", "Moldova"),
	ME = new Country(0x499, "ME", "MNE", "Montenegro"),
	MF = new Country(0x663, "MF", "MAF", "Saint Martin (French part)"),
	MG = new Country(0x450, "MG", "MDG", "Madagascar"),
	MH = new Country(0x584, "MH", "MHL", "Marshall Islands"),
	MK = new Country(0x807, "MK", "MKD", "Macedonia the former Yugoslav Republic of"),
	ML = new Country(0x466, "ML", "MLI", "Mali"),
	MM = new Country(0x104, "MM", "MMR", "Myanmar"),
	MN = new Country(0x496, "MN", "MNG", "Mongolia"),
	MO = new Country(0x446, "MO", "MAC", "Macao"),
	MP = new Country(0x580, "MP", "MNP", "Northern Mariana Islands"),
	MQ = new Country(0x474, "MQ", "MTQ", "Martinique"),
	MR = new Country(0x478, "MR", "MRT", "Mauritania"),
	MS = new Country(0x500, "MS", "MSR", "Montserrat"),
	MT = new Country(0x470, "MT", "MLT", "Malta"),
	MU = new Country(0x480, "MU", "MUS", "Mauritius"),
	MV = new Country(0x462, "MV", "MDV", "Maldives"),
	MW = new Country(0x454, "MW", "MWI", "Malawi"),
	MX = new Country(0x484, "MX", "MEX", "Mexico"),
	MY = new Country(0x458, "MY", "MYS", "Malaysia"),
	MZ = new Country(0x508, "MZ", "MOZ", "Mozambique"),
	NA = new Country(0x516, "NA", "NAM", "Namibia"),
	NC = new Country(0x540, "NC", "NCL", "New Caledonia"),
	NE = new Country(0x562, "NE", "NER", "Niger"),
	NF = new Country(0x574, "NF", "NFK", "Norfolk Island"),
	NG = new Country(0x566, "NG", "NGA", "Nigeria"),
	NI = new Country(0x558, "NI", "NIC", "Nicaragua"),
	NL = new Country(0x528, "NL", "NLD", "Netherlands"),
	NO = new Country(0x578, "NO", "NOR", "Norway"),
	NP = new Country(0x524, "NP", "NPL", "Nepal"),
	NR = new Country(0x520, "NR", "NRU", "Nauru"),
	NU = new Country(0x570, "NU", "NIU", "Niue"),
	NZ = new Country(0x554, "NZ", "NZL", "New Zealand"),
	OM = new Country(0x512, "OM", "OMN", "Oman"),
	PA = new Country(0x591, "PA", "PAN", "Panama"),
	PE = new Country(0x604, "PE", "PER", "Peru"),
	PF = new Country(0x258, "PF", "PYF", "French Polynesia"),
	PG = new Country(0x598, "PG", "PNG", "Papua New Guinea"),
	PH = new Country(0x608, "PH", "PHL", "Philippines"),
	PK = new Country(0x586, "PK", "PAK", "Pakistan"),
	PL = new Country(0x616, "PL", "POL", "Poland"),
	PM = new Country(0x666, "PM", "SPM", "Saint Pierre and Miquelon"),
	PN = new Country(0x612, "PN", "PCN", "Pitcairn"),
	PR = new Country(0x630, "PR", "PRI", "Puerto Rico"),
	PS = new Country(0x275, "PS", "PSE", "Palestinian Territory Occupied"),
	PT = new Country(0x620, "PT", "PRT", "Portugal"),
	PW = new Country(0x585, "PW", "PLW", "Palau"),
	PY = new Country(0x600, "PY", "PRY", "Paraguay"),
	QA = new Country(0x634, "QA", "QAT", "Qatar"),
	RE = new Country(0x638, "RE", "REU", "Reunion"),
	RO = new Country(0x642, "RO", "ROU", "Romania"),
	RS = new Country(0x688, "RS", "SRB", "Serbia"),
	RU = new Country(0x643, "RU", "RUS", "Russian Federation"),
	RW = new Country(0x646, "RW", "RWA", "Rwanda"),
	SA = new Country(0x682, "SA", "SAU", "Saudi Arabia"),
	SB = new Country(0x090, "SB", "SLB", "Solomon Islands"),
	SC = new Country(0x690, "SC", "SYC", "Seychelles"),
	SD = new Country(0x736, "SD", "SDN", "Sudan"),
	SE = new Country(0x752, "SE", "SWE", "Sweden"),
	SG = new Country(0x702, "SG", "SGP", "Singapore"),
	SH = new Country(0x654, "SH", "SHN", "Saint Helena"),
	SI = new Country(0x705, "SI", "SVN", "Slovenia"),
	SJ = new Country(0x744, "SJ", "SJM", "Svalbard and Jan Mayen"),
	SK = new Country(0x703, "SK", "SVK", "Slovakia"),
	SL = new Country(0x694, "SL", "SLE", "Sierra Leone"),
	SM = new Country(0x674, "SM", "SMR", "San Marino"),
	SN = new Country(0x686, "SN", "SEN", "Senegal"),
	SO = new Country(0x706, "SO", "SOM", "Somalia"),
	SR = new Country(0x740, "SR", "SUR", "Suriname"),
	ST = new Country(0x678, "ST", "STP", "Sao Tome and Principe"),
	SV = new Country(0x222, "SV", "SLV", "El Salvador"),
	SY = new Country(0x760, "SY", "SYR", "Syrian Arab Republic"),
	SZ = new Country(0x748, "SZ", "SWZ", "Swaziland"),
	TC = new Country(0x796, "TC", "TCA", "Turks and Caicos Islands"),
	TD = new Country(0x148, "TD", "TCD", "Chad"),
	TF = new Country(0x260, "TF", "ATF", "French Southern Territories"),
	TG = new Country(0x768, "TG", "TGO", "Togo"),
	TH = new Country(0x764, "TH", "THA", "Thailand"),
	TJ = new Country(0x762, "TJ", "TJK", "Tajikistan"),
	TK = new Country(0x772, "TK", "TKL", "Tokelau"),
	TL = new Country(0x626, "TL", "TLS", "Timor-Leste"),
	TM = new Country(0x795, "TM", "TKM", "Turkmenistan"),
	TN = new Country(0x788, "TN", "TUN", "Tunisia"),
	TO = new Country(0x776, "TO", "TON", "Tonga"),
	TR = new Country(0x792, "TR", "TUR", "Turkey"),
	TT = new Country(0x780, "TT", "TTO", "Trinidad and Tobago"),
	TV = new Country(0x798, "TV", "TUV", "Tuvalu"),
	TW = new Country(0x158, "TW", "TWN", "Taiwan Province of China"),
	TZ = new Country(0x834, "TZ", "TZA", "Tanzania United Republic of"),
	UA = new Country(0x804, "UA", "UKR", "Ukraine"),
	UG = new Country(0x800, "UG", "UGA", "Uganda"),
	UM = new Country(0x581, "UM", "UMI", "United States Minor Outlying Islands"),
	US = new Country(0x840, "US", "USA", "United States"),
	UY = new Country(0x858, "UY", "URY", "Uruguay"),
	UZ = new Country(0x860, "UZ", "UZB", "Uzbekistan"),
	VA = new Country(0x336, "VA", "VAT", "Holy See (Vatican City State)"),
	VC = new Country(0x670, "VC", "VCT", "Saint Vincent and the Grenadines"),
	VE = new Country(0x862, "VE", "VEN", "Venezuela"),
	VG = new Country(0x092, "VG", "VGB", "Virgin Islands British"),
	VI = new Country(0x850, "VI", "VIR", "Virgin Islands U.S."),
	VN = new Country(0x704, "VN", "VNM", "Viet Nam"),
	VU = new Country(0x548, "VU", "VUT", "Vanuatu"),
	WF = new Country(0x876, "WF", "WLF", "Wallis and Futuna"),
	WS = new Country(0x882, "WS", "WSM", "Samoa"),
	YE = new Country(0x887, "YE", "YEM", "Yemen"),
	YT = new Country(0x175, "YT", "MYT", "Mayotte"),
	ZA = new Country(0x710, "ZA", "ZAF", "South Africa"),
	ZM = new Country(0x894, "ZM", "ZMB", "Zambia"),
	ZW = new Country(0x716, "ZW", "ZWE", "Zimbabwe");

	private static final Country[] VALUES =
	{
		AW, AF, AO, AI, AX, AL, AD, AN, AE, AR, AM, AS, AQ, TF, AG, AU,
		AT, AZ, BI, BE, BJ, BF, BD, BG, BH, BS, BA, BL, BY, BZ, BM, BO,
		BR, BB, BN, BT, BV, BW, CF, CA, CC, CH, CL, CN, CI, CM, CD, CG,
		CK, CO, KM, CV, CR, CU, CX, KY, CY, CZ, DE, DJ, DM, DK, DO, DZ,
		EC, EG, ER, EH, ES, EE, ET, FI, FJ, FK, FR, FO, FM, GA, GB, GE,
		GG, GH, GI, GN, GP, GM, GW, GQ, GR, GD, GL, GT, GF, GU, GY, HK,
		HM, HN, HR, HT, HU, ID, IM, IN, IO, IE, IR, IQ, IS, IL, IT, JM,
		JE, JO, JP, KZ, KE, KG, KH, KI, KN, KR, KW, LA, LB, LR, LY, LC,
		LI, LK, LS, LT, LU, LV, MO, MF, MA, MC, MD, MG, MV, MX, MH, MK,
		ML, MT, MM, ME, MN, MP, MZ, MR, MS, MQ, MU, MW, MY, YT, NA, NC,
		NE, NF, NG, NI, NU, NL, NO, NP, NR, NZ, OM, PK, PA, PN, PE, PH,
		PW, PG, PL, PR, KP, PT, PY, PS, PF, QA, RE, RO, RU, RW, SA, SD,
		SN, SG, GS, SH, SJ, SB, SL, SV, SM, SO, PM, RS, ST, SR, SK, SI,
		SE, SZ, SC, SY, TC, TD, TG, TH, TJ, TK, TM, TL, TO, TT, TN, TR,
		TV, TW, TZ, UG, UA, UM, UY, US, UZ, VA, VC, VE, VG, VI, VN, VU,
		WF, WS, YE, ZA, ZM, ZW
	};

	private int code;
	private String alpha2Code;
	private String alpha3Code;
	private String name;

	private Country(int code, String alpha2Code, String alpha3Code, String name) {
		this.code = code;
		this.alpha2Code = alpha2Code;
		this.alpha3Code = alpha3Code;
		this.name = name;
	}

	public static Country getInstance(int code) {
		for (Country country: VALUES) {
			if (country.code == code) { return country; }
		}
		throw new IllegalArgumentException("Illegal country code" + Integer.toHexString(code));
	}

	public static Country getInstance(String code) {
		if (code == null) { throw new IllegalArgumentException("Illegal country code " + code); }
		code = code.trim();
		switch (code.length()) {
		case 2: return fromAlpha2(code);
		case 3: return fromAlpha3(code);
		default: throw new IllegalArgumentException("Illegal country code " + code);
		}
	}

	public static Country[] values() {
		return VALUES;
	}

	public int valueOf() {
		return code;
	}

	private static Country fromAlpha2(String code) {
		for (Country country: VALUES) {
			if (country.alpha2Code.equals(code)) { return country; }
		}
		throw new IllegalArgumentException("Illegal country code " + code);
	}

	private static Country fromAlpha3(String code) {
		for (Country country: VALUES) {
			if (country.alpha3Code.equals(code)) { return country; }
		}
		throw new IllegalArgumentException("Illegal country code " + code);
	}

	public String getName() {
		return name;
	}
	
	public String toString() {
		return alpha2Code;
	}

	public String toAlpha3Code() {
		return alpha3Code;
	}
}
