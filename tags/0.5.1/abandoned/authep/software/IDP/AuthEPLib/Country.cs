using System;

#pragma warning disable 1591

namespace nl.telin.authep.lib
{
    /// <summary>
    /// CountryHelper contains the ISO 3166 country codes and some helper methods to use them.
    /// </summary>
    public class Country
    {
        private Code _code;

        /// <summary>
        /// Get the Code of this country.
        /// </summary>
        public Code CountryCode { get { return _code; } }

        /// <summary>
        /// Create a new instance of Country using the specified country code.
        /// </summary>
        /// <param name="code">Code of the country.</param>
        public Country(Code code)
        {
            _code = code;
        }

        /// <summary>
        /// A enumeration of all country codes within ISO 3166.
        /// </summary>
        public enum Code
        {
            AW = 0x533,
            AF = 0x004,
            AO = 0x024,
            AI = 0x660,
            AX = 0x248,
            AL = 0x008,
            AD = 0x020,
            AN = 0x530,
            AE = 0x784,
            AR = 0x032,
            AM = 0x051,
            AS = 0x016,
            AQ = 0x010,
            TF = 0x260,
            AG = 0x028,
            AU = 0x036,
            AT = 0x040,
            AZ = 0x031,
            BI = 0x108,
            BE = 0x056,
            BJ = 0x204,
            BF = 0x854,
            BD = 0x050,
            BG = 0x100,
            BH = 0x048,
            BS = 0x044,
            BA = 0x070,
            BL = 0x652,
            BY = 0x112,
            BZ = 0x084,
            BM = 0x060,
            BO = 0x068,
            BR = 0x076,
            BB = 0x052,
            BN = 0x096,
            BT = 0x064,
            BV = 0x074,
            BW = 0x072,
            CF = 0x140,
            CA = 0x124,
            CC = 0x166,
            CH = 0x756,
            CL = 0x152,
            CN = 0x156,
            CI = 0x384,
            CM = 0x120,
            CD = 0x180,
            CG = 0x178,
            CK = 0x184,
            CO = 0x170,
            KM = 0x174,
            CV = 0x132,
            CR = 0x188,
            CU = 0x192,
            CX = 0x162,
            KY = 0x136,
            CY = 0x196,
            CZ = 0x203,
            DE = 0x276,
            DJ = 0x262,
            DM = 0x212,
            DK = 0x208,
            DO = 0x214,
            DZ = 0x012,
            EC = 0x218,
            EG = 0x818,
            ER = 0x232,
            EH = 0x732,
            ES = 0x724,
            EE = 0x233,
            ET = 0x231,
            FI = 0x246,
            FJ = 0x242,
            FK = 0x238,
            FR = 0x250,
            FO = 0x234,
            FM = 0x583,
            GA = 0x266,
            GB = 0x826,
            GE = 0x268,
            GG = 0x831,
            GH = 0x288,
            GN = 0x324,
            GI = 0x292,
            GP = 0x312,
            GM = 0x270,
            GW = 0x624,
            GQ = 0x226,
            GR = 0x300,
            GD = 0x308,
            GL = 0x304,
            GT = 0x320,
            GF = 0x254,
            GU = 0x316,
            GY = 0x328,
            HK = 0x344,
            HM = 0x334,
            HN = 0x340,
            HR = 0x191,
            HT = 0x332,
            HU = 0x348,
            ID = 0x360,
            IM = 0x833,
            IN = 0x356,
            IO = 0x086,
            IE = 0x372,
            IR = 0x364,
            IQ = 0x368,
            IS = 0x352,
            IL = 0x376,
            IT = 0x380,
            JM = 0x388,
            JE = 0x832,
            JO = 0x400,
            JP = 0x392,
            KZ = 0x398,
            KE = 0x404,
            KG = 0x417,
            KH = 0x116,
            KI = 0x296,
            KN = 0x659,
            KR = 0x410,
            KW = 0x414,
            LA = 0x418,
            LB = 0x422,
            LR = 0x430,
            LY = 0x434,
            LC = 0x662,
            LI = 0x438,
            LK = 0x144,
            LS = 0x426,
            LT = 0x440,
            LU = 0x442,
            LV = 0x428,
            MO = 0x446,
            MF = 0x663,
            MA = 0x504,
            MC = 0x492,
            MD = 0x498,
            MG = 0x450,
            MV = 0x462,
            MX = 0x484,
            MH = 0x584,
            MK = 0x807,
            ML = 0x466,
            MT = 0x470,
            MM = 0x104,
            ME = 0x499,
            MN = 0x496,
            MP = 0x580,
            MZ = 0x508,
            MR = 0x478,
            MS = 0x500,
            MQ = 0x474,
            MU = 0x480,
            MW = 0x454,
            MY = 0x458,
            YT = 0x175,
            NA = 0x516,
            NC = 0x540,
            NE = 0x562,
            NF = 0x574,
            NG = 0x566,
            NI = 0x558,
            NO = 0x578,
            NU = 0x570,
            NL = 0x528,
            NP = 0x524,
            NR = 0x520,
            NZ = 0x554,
            OM = 0x512,
            PK = 0x586,
            PA = 0x591,
            PN = 0x612,
            PE = 0x604,
            PH = 0x608,
            PW = 0x585,
            PG = 0x598,
            PL = 0x616,
            PR = 0x630,
            KP = 0x408,
            PT = 0x620,
            PY = 0x600,
            PS = 0x275,
            PF = 0x258,
            QA = 0x634,
            RE = 0x638,
            RO = 0x642,
            RU = 0x643,
            RW = 0x646,
            SA = 0x682,
            SD = 0x736,
            SN = 0x686,
            SG = 0x702,
            GS = 0x239,
            SH = 0x654,
            SJ = 0x744,
            SB = 0x090,
            SL = 0x694,
            SV = 0x222,
            SM = 0x674,
            SO = 0x706,
            PM = 0x666,
            RS = 0x688,
            ST = 0x678,
            SR = 0x740,
            SK = 0x703,
            SI = 0x705,
            SE = 0x752,
            SZ = 0x748,
            SC = 0x690,
            SY = 0x760,
            TC = 0x796,
            TD = 0x148,
            TG = 0x768,
            TH = 0x764,
            TJ = 0x762,
            TK = 0x772,
            TM = 0x795,
            TL = 0x626,
            TO = 0x776,
            TT = 0x780,
            TN = 0x788,
            TR = 0x792,
            TV = 0x798,
            TW = 0x158,
            TZ = 0x834,
            UG = 0x800,
            UA = 0x804,
            UM = 0x581,
            UY = 0x858,
            US = 0x840,
            UZ = 0x860,
            VA = 0x336,
            VC = 0x670,
            VE = 0x862,
            VG = 0x092,
            VI = 0x850,
            VN = 0x704,
            VU = 0x548,
            WF = 0x876,
            WS = 0x882,
            YE = 0x887,
            ZA = 0x710,
            ZM = 0x894,
            ZW = 0x716
        };

        private static Country getInstance(int code)
        {
            foreach (Code c in Enum.GetValues(typeof(Code)))
            {
                if ((int)c == code)
                    return new Country(c);
            }
            return null;
        }

        /// <summary>
        /// Get an instance of country using the specified stringcode.
        /// </summary>
        /// <param name="code">code (string) to use for creating the instance</param>
        /// <returns>a new instance of country</returns>
        public static Country getInstance(String code) {
            if (code == null) { throw new ArgumentException("Illegal country code"); }
            code = code.Trim();
            switch (code.Length) {
                case 2: throw new ArgumentException("Illegal country code");
            case 3: return fromAlpha3(code);
            default: throw new ArgumentException("Illegal country code");
            }
        }

        /// <summary>
        /// Get a country istance using an alpha3 code.
        /// </summary>
        /// <param name="code">alpha 3 code to use for creating the instance</param>
        /// <returns>a new instance of country</returns>
        public static Country fromAlpha3(String code)
        {
            if (code.Equals("ABW")) { return new Country(Code.AW); }
            if (code.Equals("AFG")) { return new Country(Code.AF); }
            if (code.Equals("AGO")) { return new Country(Code.AO); }
            if (code.Equals("AIA")) { return new Country(Code.AI); }
            if (code.Equals("ALA")) { return new Country(Code.AX); }
            if (code.Equals("ALB")) { return new Country(Code.AL); }
            if (code.Equals("AND")) { return new Country(Code.AD); }
            if (code.Equals("ANT")) { return new Country(Code.AN); }
            if (code.Equals("ARE")) { return new Country(Code.AE); }
            if (code.Equals("ARG")) { return new Country(Code.AR); }
            if (code.Equals("ARM")) { return new Country(Code.AM); }
            if (code.Equals("ASM")) { return new Country(Code.AS); }
            if (code.Equals("ATA")) { return new Country(Code.AQ); }
            if (code.Equals("ATF")) { return new Country(Code.TF); }
            if (code.Equals("ATG")) { return new Country(Code.AG); }
            if (code.Equals("AUS")) { return new Country(Code.AU); }
            if (code.Equals("AUT")) { return new Country(Code.AT); }
            if (code.Equals("AZE")) { return new Country(Code.AZ); }
            if (code.Equals("BDI")) { return new Country(Code.BI); }
            if (code.Equals("BEL")) { return new Country(Code.BE); }
            if (code.Equals("BEN")) { return new Country(Code.BJ); }
            if (code.Equals("BFA")) { return new Country(Code.BF); }
            if (code.Equals("BGD")) { return new Country(Code.BD); }
            if (code.Equals("BGR")) { return new Country(Code.BG); }
            if (code.Equals("BHR")) { return new Country(Code.BH); }
            if (code.Equals("BHS")) { return new Country(Code.BS); }
            if (code.Equals("BIH")) { return new Country(Code.BA); }
            if (code.Equals("BLM")) { return new Country(Code.BL); }
            if (code.Equals("BLR")) { return new Country(Code.BY); }
            if (code.Equals("BLZ")) { return new Country(Code.BZ); }
            if (code.Equals("BMU")) { return new Country(Code.BM); }
            if (code.Equals("BOL")) { return new Country(Code.BO); }
            if (code.Equals("BRA")) { return new Country(Code.BR); }
            if (code.Equals("BRB")) { return new Country(Code.BB); }
            if (code.Equals("BRN")) { return new Country(Code.BN); }
            if (code.Equals("BTN")) { return new Country(Code.BT); }
            if (code.Equals("BVT")) { return new Country(Code.BV); }
            if (code.Equals("BWA")) { return new Country(Code.BW); }
            if (code.Equals("CAF")) { return new Country(Code.CF); }
            if (code.Equals("CAN")) { return new Country(Code.CA); }
            if (code.Equals("CCK")) { return new Country(Code.CC); }
            if (code.Equals("CHE")) { return new Country(Code.CH); }
            if (code.Equals("CHL")) { return new Country(Code.CL); }
            if (code.Equals("CHN")) { return new Country(Code.CN); }
            if (code.Equals("CIV")) { return new Country(Code.CI); }
            if (code.Equals("CMR")) { return new Country(Code.CM); }
            if (code.Equals("COD")) { return new Country(Code.CD); }
            if (code.Equals("COG")) { return new Country(Code.CG); }
            if (code.Equals("COK")) { return new Country(Code.CK); }
            if (code.Equals("COL")) { return new Country(Code.CO); }
            if (code.Equals("COM")) { return new Country(Code.KM); }
            if (code.Equals("CPV")) { return new Country(Code.CV); }
            if (code.Equals("CRI")) { return new Country(Code.CR); }
            if (code.Equals("CUB")) { return new Country(Code.CU); }
            if (code.Equals("CXR")) { return new Country(Code.CX); }
            if (code.Equals("CYM")) { return new Country(Code.KY); }
            if (code.Equals("CYP")) { return new Country(Code.CY); }
            if (code.Equals("CZE")) { return new Country(Code.CZ); }
            if (code.Equals("DEU")) { return new Country(Code.DE); }
            if (code.Equals("DJI")) { return new Country(Code.DJ); }
            if (code.Equals("DMA")) { return new Country(Code.DM); }
            if (code.Equals("DNK")) { return new Country(Code.DK); }
            if (code.Equals("DOM")) { return new Country(Code.DO); }
            if (code.Equals("DZA")) { return new Country(Code.DZ); }
            if (code.Equals("ECU")) { return new Country(Code.EC); }
            if (code.Equals("EGY")) { return new Country(Code.EG); }
            if (code.Equals("ERI")) { return new Country(Code.ER); }
            if (code.Equals("ESH")) { return new Country(Code.EH); }
            if (code.Equals("ESP")) { return new Country(Code.ES); }
            if (code.Equals("EST")) { return new Country(Code.EE); }
            if (code.Equals("ETH")) { return new Country(Code.ET); }
            if (code.Equals("FIN")) { return new Country(Code.FI); }
            if (code.Equals("FJI")) { return new Country(Code.FJ); }
            if (code.Equals("FLK")) { return new Country(Code.FK); }
            if (code.Equals("FRA")) { return new Country(Code.FR); }
            if (code.Equals("FRO")) { return new Country(Code.FO); }
            if (code.Equals("FSM")) { return new Country(Code.FM); }
            if (code.Equals("GAB")) { return new Country(Code.GA); }
            if (code.Equals("GBR")) { return new Country(Code.GB); }
            if (code.Equals("GEO")) { return new Country(Code.GE); }
            if (code.Equals("GGY")) { return new Country(Code.GG); }
            if (code.Equals("GHA")) { return new Country(Code.GH); }
            if (code.Equals("GIB")) { return new Country(Code.GI); }
            if (code.Equals("GIN")) { return new Country(Code.GN); }
            if (code.Equals("GLP")) { return new Country(Code.GP); }
            if (code.Equals("GMB")) { return new Country(Code.GM); }
            if (code.Equals("GNB")) { return new Country(Code.GW); }
            if (code.Equals("GNQ")) { return new Country(Code.GQ); }
            if (code.Equals("GRC")) { return new Country(Code.GR); }
            if (code.Equals("GRD")) { return new Country(Code.GD); }
            if (code.Equals("GRL")) { return new Country(Code.GL); }
            if (code.Equals("GTM")) { return new Country(Code.GT); }
            if (code.Equals("GUF")) { return new Country(Code.GF); }
            if (code.Equals("GUM")) { return new Country(Code.GU); }
            if (code.Equals("GUY")) { return new Country(Code.GY); }
            if (code.Equals("HKG")) { return new Country(Code.HK); }
            if (code.Equals("HMD")) { return new Country(Code.HM); }
            if (code.Equals("HND")) { return new Country(Code.HN); }
            if (code.Equals("HRV")) { return new Country(Code.HR); }
            if (code.Equals("HTI")) { return new Country(Code.HT); }
            if (code.Equals("HUN")) { return new Country(Code.HU); }
            if (code.Equals("IDN")) { return new Country(Code.ID); }
            if (code.Equals("IMN")) { return new Country(Code.IM); }
            if (code.Equals("IND")) { return new Country(Code.IN); }
            if (code.Equals("IOT")) { return new Country(Code.IO); }
            if (code.Equals("IRL")) { return new Country(Code.IE); }
            if (code.Equals("IRN")) { return new Country(Code.IR); }
            if (code.Equals("IRQ")) { return new Country(Code.IQ); }
            if (code.Equals("ISL")) { return new Country(Code.IS); }
            if (code.Equals("ISR")) { return new Country(Code.IL); }
            if (code.Equals("ITA")) { return new Country(Code.IT); }
            if (code.Equals("JAM")) { return new Country(Code.JM); }
            if (code.Equals("JEY")) { return new Country(Code.JE); }
            if (code.Equals("JOR")) { return new Country(Code.JO); }
            if (code.Equals("JPN")) { return new Country(Code.JP); }
            if (code.Equals("KAZ")) { return new Country(Code.KZ); }
            if (code.Equals("KEN")) { return new Country(Code.KE); }
            if (code.Equals("KGZ")) { return new Country(Code.KG); }
            if (code.Equals("KHM")) { return new Country(Code.KH); }
            if (code.Equals("KIR")) { return new Country(Code.KI); }
            if (code.Equals("KNA")) { return new Country(Code.KN); }
            if (code.Equals("KOR")) { return new Country(Code.KR); }
            if (code.Equals("KWT")) { return new Country(Code.KW); }
            if (code.Equals("LAO")) { return new Country(Code.LA); }
            if (code.Equals("LBN")) { return new Country(Code.LB); }
            if (code.Equals("LBR")) { return new Country(Code.LR); }
            if (code.Equals("LBY")) { return new Country(Code.LY); }
            if (code.Equals("LCA")) { return new Country(Code.LC); }
            if (code.Equals("LIE")) { return new Country(Code.LI); }
            if (code.Equals("LKA")) { return new Country(Code.LK); }
            if (code.Equals("LSO")) { return new Country(Code.LS); }
            if (code.Equals("LTU")) { return new Country(Code.LT); }
            if (code.Equals("LUX")) { return new Country(Code.LU); }
            if (code.Equals("LVA")) { return new Country(Code.LV); }
            if (code.Equals("MAC")) { return new Country(Code.MO); }
            if (code.Equals("MAF")) { return new Country(Code.MF); }
            if (code.Equals("MAR")) { return new Country(Code.MA); }
            if (code.Equals("MCO")) { return new Country(Code.MC); }
            if (code.Equals("MDA")) { return new Country(Code.MD); }
            if (code.Equals("MDG")) { return new Country(Code.MG); }
            if (code.Equals("MDV")) { return new Country(Code.MV); }
            if (code.Equals("MEX")) { return new Country(Code.MX); }
            if (code.Equals("MHL")) { return new Country(Code.MH); }
            if (code.Equals("MKD")) { return new Country(Code.MK); }
            if (code.Equals("MLI")) { return new Country(Code.ML); }
            if (code.Equals("MLT")) { return new Country(Code.MT); }
            if (code.Equals("MMR")) { return new Country(Code.MM); }
            if (code.Equals("MNE")) { return new Country(Code.ME); }
            if (code.Equals("MNG")) { return new Country(Code.MN); }
            if (code.Equals("MNP")) { return new Country(Code.MP); }
            if (code.Equals("MOZ")) { return new Country(Code.MZ); }
            if (code.Equals("MRT")) { return new Country(Code.MR); }
            if (code.Equals("MSR")) { return new Country(Code.MS); }
            if (code.Equals("MTQ")) { return new Country(Code.MQ); }
            if (code.Equals("MUS")) { return new Country(Code.MU); }
            if (code.Equals("MWI")) { return new Country(Code.MW); }
            if (code.Equals("MYS")) { return new Country(Code.MY); }
            if (code.Equals("MYT")) { return new Country(Code.YT); }
            if (code.Equals("NAM")) { return new Country(Code.NA); }
            if (code.Equals("NCL")) { return new Country(Code.NC); }
            if (code.Equals("NER")) { return new Country(Code.NE); }
            if (code.Equals("NFK")) { return new Country(Code.NF); }
            if (code.Equals("NGA")) { return new Country(Code.NG); }
            if (code.Equals("NIC")) { return new Country(Code.NI); }
            if (code.Equals("NIU")) { return new Country(Code.NU); }
            if (code.Equals("NLD")) { return new Country(Code.NL); }
            if (code.Equals("NOR")) { return new Country(Code.NO); }
            if (code.Equals("NPL")) { return new Country(Code.NP); }
            if (code.Equals("NRU")) { return new Country(Code.NR); }
            if (code.Equals("NZL")) { return new Country(Code.NZ); }
            if (code.Equals("OMN")) { return new Country(Code.OM); }
            if (code.Equals("PAK")) { return new Country(Code.PK); }
            if (code.Equals("PAN")) { return new Country(Code.PA); }
            if (code.Equals("PCN")) { return new Country(Code.PN); }
            if (code.Equals("PER")) { return new Country(Code.PE); }
            if (code.Equals("PHL")) { return new Country(Code.PH); }
            if (code.Equals("PLW")) { return new Country(Code.PW); }
            if (code.Equals("PNG")) { return new Country(Code.PG); }
            if (code.Equals("POL")) { return new Country(Code.PL); }
            if (code.Equals("PRI")) { return new Country(Code.PR); }
            if (code.Equals("PRK")) { return new Country(Code.KP); }
            if (code.Equals("PRT")) { return new Country(Code.PT); }
            if (code.Equals("PRY")) { return new Country(Code.PY); }
            if (code.Equals("PSE")) { return new Country(Code.PS); }
            if (code.Equals("PYF")) { return new Country(Code.PF); }
            if (code.Equals("QAT")) { return new Country(Code.QA); }
            if (code.Equals("REU")) { return new Country(Code.RE); }
            if (code.Equals("ROU")) { return new Country(Code.RO); }
            if (code.Equals("RUS")) { return new Country(Code.RU); }
            if (code.Equals("RWA")) { return new Country(Code.RW); }
            if (code.Equals("SAU")) { return new Country(Code.SA); }
            if (code.Equals("SDN")) { return new Country(Code.SD); }
            if (code.Equals("SEN")) { return new Country(Code.SN); }
            if (code.Equals("SGP")) { return new Country(Code.SG); }
            if (code.Equals("SGS")) { return new Country(Code.GS); }
            if (code.Equals("SHN")) { return new Country(Code.SH); }
            if (code.Equals("SJM")) { return new Country(Code.SJ); }
            if (code.Equals("SLB")) { return new Country(Code.SB); }
            if (code.Equals("SLE")) { return new Country(Code.SL); }
            if (code.Equals("SLV")) { return new Country(Code.SV); }
            if (code.Equals("SMR")) { return new Country(Code.SM); }
            if (code.Equals("SOM")) { return new Country(Code.SO); }
            if (code.Equals("SPM")) { return new Country(Code.PM); }
            if (code.Equals("SRB")) { return new Country(Code.RS); }
            if (code.Equals("STP")) { return new Country(Code.ST); }
            if (code.Equals("SUR")) { return new Country(Code.SR); }
            if (code.Equals("SVK")) { return new Country(Code.SK); }
            if (code.Equals("SVN")) { return new Country(Code.SI); }
            if (code.Equals("SWE")) { return new Country(Code.SE); }
            if (code.Equals("SWZ")) { return new Country(Code.SZ); }
            if (code.Equals("SYC")) { return new Country(Code.SC); }
            if (code.Equals("SYR")) { return new Country(Code.SY); }
            if (code.Equals("TCA")) { return new Country(Code.TC); }
            if (code.Equals("TCD")) { return new Country(Code.TD); }
            if (code.Equals("TGO")) { return new Country(Code.TG); }
            if (code.Equals("THA")) { return new Country(Code.TH); }
            if (code.Equals("TJK")) { return new Country(Code.TJ); }
            if (code.Equals("TKL")) { return new Country(Code.TK); }
            if (code.Equals("TKM")) { return new Country(Code.TM); }
            if (code.Equals("TLS")) { return new Country(Code.TL); }
            if (code.Equals("TON")) { return new Country(Code.TO); }
            if (code.Equals("TTO")) { return new Country(Code.TT); }
            if (code.Equals("TUN")) { return new Country(Code.TN); }
            if (code.Equals("TUR")) { return new Country(Code.TR); }
            if (code.Equals("TUV")) { return new Country(Code.TV); }
            if (code.Equals("TWN")) { return new Country(Code.TW); }
            if (code.Equals("TZA")) { return new Country(Code.TZ); }
            if (code.Equals("UGA")) { return new Country(Code.UG); }
            if (code.Equals("UKR")) { return new Country(Code.UA); }
            if (code.Equals("UMI")) { return new Country(Code.UM); }
            if (code.Equals("URY")) { return new Country(Code.UY); }
            if (code.Equals("USA")) { return new Country(Code.US); }
            if (code.Equals("UZB")) { return new Country(Code.UZ); }
            if (code.Equals("VAT")) { return new Country(Code.VA); }
            if (code.Equals("VCT")) { return new Country(Code.VC); }
            if (code.Equals("VEN")) { return new Country(Code.VE); }
            if (code.Equals("VGB")) { return new Country(Code.VG); }
            if (code.Equals("VIR")) { return new Country(Code.VI); }
            if (code.Equals("VNM")) { return new Country(Code.VN); }
            if (code.Equals("VUT")) { return new Country(Code.VU); }
            if (code.Equals("WLF")) { return new Country(Code.WF); }
            if (code.Equals("WSM")) { return new Country(Code.WS); }
            if (code.Equals("YEM")) { return new Country(Code.YE); }
            if (code.Equals("ZAF")) { return new Country(Code.ZA); }
            if (code.Equals("ZMB")) { return new Country(Code.ZM); }
            if (code.Equals("ZWE")) { return new Country(Code.ZW); }
            throw new ArgumentException("Illegal country code");
        }

        /// <summary>
        /// Get the alpha3 code of this country
        /// </summary>
        /// <returns>alphe3 code</returns>
        public String toAlpha3Code() {
                switch (_code) {
                    case Code.AD: return "AND";
                    case Code.AE: return "ARE";
                    case Code.AF: return "AFG";
                    case Code.AG: return "ATG";
                    case Code.AI: return "AIA";
                    case Code.AL: return "ALB";
                    case Code.AM: return "ARM";
                    case Code.AN: return "ANT";
                    case Code.AO: return "AGO";
                    case Code.AQ: return "ATA";
                    case Code.AR: return "ARG";
                    case Code.AS: return "ASM";
                    case Code.AT: return "AUT";
                    case Code.AU: return "AUS";
                    case Code.AW: return "ABW";
                    case Code.AX: return "ALA";
                    case Code.AZ: return "AZE";
                    case Code.BA: return "BIH";
                    case Code.BB: return "BRB";
                    case Code.BD: return "BGD";
                    case Code.BE: return "BEL";
                    case Code.BF: return "BFA";
                    case Code.BG: return "BGR";
                    case Code.BH: return "BHR";
                    case Code.BI: return "BDI";
                    case Code.BJ: return "BEN";
                    case Code.BL: return "BLM";
                    case Code.BM: return "BMU";
                    case Code.BN: return "BRN";
                    case Code.BO: return "BOL";
                    case Code.BR: return "BRA";
                    case Code.BS: return "BHS";
                    case Code.BT: return "BTN";
                    case Code.BV: return "BVT";
                    case Code.BW: return "BWA";
                    case Code.BY: return "BLR";
                    case Code.BZ: return "BLZ";
                    case Code.CA: return "CAN";
                    case Code.CC: return "CCK";
                    case Code.CD: return "COD";
                    case Code.CF: return "CAF";
                    case Code.CG: return "COG";
                    case Code.CH: return "CHE";
                    case Code.CI: return "CIV";
                    case Code.CK: return "COK";
                    case Code.CL: return "CHL";
                    case Code.CM: return "CMR";
                    case Code.CN: return "CHN";
                    case Code.CO: return "COL";
                    case Code.CR: return "CRI";
                    case Code.CU: return "CUB";
                    case Code.CV: return "CPV";
                    case Code.CX: return "CXR";
                    case Code.CY: return "CYP";
                    case Code.CZ: return "CZE";
                    case Code.DE: return "DEU";
                    case Code.DJ: return "DJI";
                    case Code.DK: return "DNK";
                    case Code.DM: return "DMA";
                    case Code.DO: return "DOM";
                    case Code.DZ: return "DZA";
                    case Code.EC: return "ECU";
                    case Code.EE: return "EST";
                    case Code.EG: return "EGY";
                    case Code.EH: return "ESH";
                    case Code.ER: return "ERI";
                    case Code.ES: return "ESP";
                    case Code.ET: return "ETH";
                    case Code.FI: return "FIN";
                    case Code.FJ: return "FJI";
                    case Code.FK: return "FLK";
                    case Code.FM: return "FSM";
                    case Code.FO: return "FRO";
                    case Code.FR: return "FRA";
                    case Code.GA: return "GAB";
                    case Code.GB: return "GBR";
                    case Code.GD: return "GRD";
                    case Code.GE: return "GEO";
                    case Code.GF: return "GUF";
                    case Code.GG: return "GGY";
                    case Code.GH: return "GHA";
                    case Code.GI: return "GIB";
                    case Code.GL: return "GRL";
                    case Code.GM: return "GMB";
                    case Code.GN: return "GIN";
                    case Code.GP: return "GLP";
                    case Code.GQ: return "GNQ";
                    case Code.GR: return "GRC";
                    case Code.GS: return "SGS";
                    case Code.GT: return "GTM";
                    case Code.GU: return "GUM";
                    case Code.GW: return "GNB";
                    case Code.GY: return "GUY";
                    case Code.HK: return "HKG";
                    case Code.HM: return "HMD";
                    case Code.HN: return "HND";
                    case Code.HR: return "HRV";
                    case Code.HT: return "HTI";
                    case Code.HU: return "HUN";
                    case Code.ID: return "IDN";
                    case Code.IE: return "IRL";
                    case Code.IL: return "ISR";
                    case Code.IM: return "IMN";
                    case Code.IN: return "IND";
                    case Code.IO: return "IOT";
                    case Code.IQ: return "IRQ";
                    case Code.IR: return "IRN";
                    case Code.IS: return "ISL";
                    case Code.IT: return "ITA";
                    case Code.JE: return "JEY";
                    case Code.JM: return "JAM";
                    case Code.JO: return "JOR";
                    case Code.JP: return "JPN";
                    case Code.KE: return "KEN";
                    case Code.KG: return "KGZ";
                    case Code.KH: return "KHM";
                    case Code.KI: return "KIR";
                    case Code.KM: return "COM";
                    case Code.KN: return "KNA";
                    case Code.KP: return "PRK";
                    case Code.KR: return "KOR";
                    case Code.KW: return "KWT";
                    case Code.KY: return "CYM";
                    case Code.KZ: return "KAZ";
                    case Code.LA: return "LAO";
                    case Code.LB: return "LBN";
                    case Code.LC: return "LCA";
                    case Code.LI: return "LIE";
                    case Code.LK: return "LKA";
                    case Code.LR: return "LBR";
                    case Code.LS: return "LSO";
                    case Code.LT: return "LTU";
                    case Code.LU: return "LUX";
                    case Code.LV: return "LVA";
                    case Code.LY: return "LBY";
                    case Code.MA: return "MAR";
                    case Code.MC: return "MCO";
                    case Code.MD: return "MDA";
                    case Code.ME: return "MNE";
                    case Code.MF: return "MAF";
                    case Code.MG: return "MDG";
                    case Code.MH: return "MHL";
                    case Code.MK: return "MKD";
                    case Code.ML: return "MLI";
                    case Code.MM: return "MMR";
                    case Code.MN: return "MNG";
                    case Code.MO: return "MAC";
                    case Code.MP: return "MNP";
                    case Code.MQ: return "MTQ";
                    case Code.MR: return "MRT";
                    case Code.MS: return "MSR";
                    case Code.MT: return "MLT";
                    case Code.MU: return "MUS";
                    case Code.MV: return "MDV";
                    case Code.MW: return "MWI";
                    case Code.MX: return "MEX";
                    case Code.MY: return "MYS";
                    case Code.MZ: return "MOZ";
                    case Code.NA: return "NAM";
                    case Code.NC: return "NCL";
                    case Code.NE: return "NER";
                    case Code.NF: return "NFK";
                    case Code.NG: return "NGA";
                    case Code.NI: return "NIC";
                    case Code.NL: return "NLD";
                    case Code.NO: return "NOR";
                    case Code.NP: return "NPL";
                    case Code.NR: return "NRU";
                    case Code.NU: return "NIU";
                    case Code.NZ: return "NZL";
                    case Code.OM: return "OMN";
                    case Code.PA: return "PAN";
                    case Code.PE: return "PER";
                    case Code.PF: return "PYF";
                    case Code.PG: return "PNG";
                    case Code.PH: return "PHL";
                    case Code.PK: return "PAK";
                    case Code.PL: return "POL";
                    case Code.PM: return "SPM";
                    case Code.PN: return "PCN";
                    case Code.PR: return "PRI";
                    case Code.PS: return "PSE";
                    case Code.PT: return "PRT";
                    case Code.PW: return "PLW";
                    case Code.PY: return "PRY";
                    case Code.QA: return "QAT";
                    case Code.RE: return "REU";
                    case Code.RO: return "ROU";
                    case Code.RS: return "SRB";
                    case Code.RU: return "RUS";
                    case Code.RW: return "RWA";
                    case Code.SA: return "SAU";
                    case Code.SB: return "SLB";
                    case Code.SC: return "SYC";
                    case Code.SD: return "SDN";
                    case Code.SE: return "SWE";
                    case Code.SG: return "SGP";
                    case Code.SH: return "SHN";
                    case Code.SI: return "SVN";
                    case Code.SJ: return "SJM";
                    case Code.SK: return "SVK";
                    case Code.SL: return "SLE";
                    case Code.SM: return "SMR";
                    case Code.SN: return "SEN";
                    case Code.SO: return "SOM";
                    case Code.SR: return "SUR";
                    case Code.ST: return "STP";
                    case Code.SV: return "SLV";
                    case Code.SY: return "SYR";
                    case Code.SZ: return "SWZ";
                    case Code.TC: return "TCA";
                    case Code.TD: return "TCD";
                    case Code.TF: return "ATF";
                    case Code.TG: return "TGO";
                    case Code.TH: return "THA";
                    case Code.TJ: return "TJK";
                    case Code.TK: return "TKL";
                    case Code.TL: return "TLS";
                    case Code.TM: return "TKM";
                    case Code.TN: return "TUN";
                    case Code.TO: return "TON";
                    case Code.TR: return "TUR";
                    case Code.TT: return "TTO";
                    case Code.TV: return "TUV";
                    case Code.TW: return "TWN";
                    case Code.TZ: return "TZA";
                    case Code.UA: return "UKR";
                    case Code.UG: return "UGA";
                    case Code.UM: return "UMI";
                    case Code.US: return "USA";
                    case Code.UY: return "URY";
                    case Code.UZ: return "UZB";
                    case Code.VA: return "VAT";
                    case Code.VC: return "VCT";
                    case Code.VE: return "VEN";
                    case Code.VG: return "VGB";
                    case Code.VI: return "VIR";
                    case Code.VN: return "VNM";
                    case Code.VU: return "VUT";
                    case Code.WF: return "WLF";
                    case Code.WS: return "WSM";
                    case Code.YE: return "YEM";
                    case Code.YT: return "MYT";
                    case Code.ZA: return "ZAF";
                    case Code.ZM: return "ZMB";
                    case Code.ZW: return "ZWE";
                default: throw new Exception("Unknown country");
                }
            }
	
        /// <summary>
        /// Get the name of this country.
        /// </summary>
        /// <returns>Name of this country.</returns>
        public String getName() {
                switch (_code) 
                {
                case Code.AD: return "Andorra";
                case Code.AE: return "United Arab Emirates";
                case Code.AF: return "Afghanistan";
                case Code.AG: return "Antigua and Barbuda";
                case Code.AI: return "Anguilla";
                case Code.AL: return "Albania";
                case Code.AM: return "Armenia";
                case Code.AN: return "Netherlands Antilles";
                case Code.AO: return "Angola";
                case Code.AQ: return "Antarctica";
                case Code.AR: return "Argentina";
                case Code.AS: return "American Samoa";
                case Code.AT: return "Austria";
                case Code.AU: return "Australia";
                case Code.AW: return "Aruba";
                case Code.AX: return "Aland Islands";
                case Code.AZ: return "Azerbaijan";
                case Code.BA: return "Bosnia and Herzegovina";
                case Code.BB: return "Barbados";
                case Code.BD: return "Bangladesh";
                case Code.BE: return "Belgium";
                case Code.BF: return "Burkina Faso";
                case Code.BG: return "Bulgaria";
                case Code.BH: return "Bahrain";
                case Code.BI: return "Burundi";
                case Code.BJ: return "Benin";
                case Code.BL: return "Saint Barthlemy";
                case Code.BM: return "Bermuda";
                case Code.BN: return "Brunei Darussalam";
                case Code.BO: return "Bolivia";
                case Code.BR: return "Brazil";
                case Code.BS: return "Bahamas";
                case Code.BT: return "Bhutan";
                case Code.BV: return "Bouvet Island";
                case Code.BW: return "Botswana";
                case Code.BY: return "Belarus";
                case Code.BZ: return "Belize";
                case Code.CA: return "Canada";
                case Code.CC: return "Cocos (Keeling) Islands";
                case Code.CD: return "Congo, the Democratic Republic of the";
                case Code.CF: return "Central African Republic";
                case Code.CG: return "Congo";
                case Code.CH: return "Switzerland";
                case Code.CI: return "Cote d'Ivoire";
                case Code.CK: return "Cook Islands";
                case Code.CL: return "Chile";
                case Code.CM: return "Cameroon";
                case Code.CN: return "China";
                case Code.CO: return "Colombia";
                case Code.CR: return "Costa Rica";
                case Code.CU: return "Cuba";
                case Code.CV: return "Cape Verde";
                case Code.CX: return "Christmas Island";
                case Code.CY: return "Cyprus";
                case Code.CZ: return "Czech Republic";
                case Code.DE: return "Germany";
                case Code.DJ: return "Djibouti";
                case Code.DK: return "Denmark";
                case Code.DM: return "Dominica";
                case Code.DO: return "Dominican Republic";
                case Code.DZ: return "Algeria";
                case Code.EC: return "Ecuador";
                case Code.EE: return "Estonia";
                case Code.EG: return "Egypt";
                case Code.EH: return "Western Sahara";
                case Code.ER: return "Eritrea";
                case Code.ES: return "Spain";
                case Code.ET: return "Ethiopia";
                case Code.FI: return "Finland";
                case Code.FJ: return "Fiji";
                case Code.FK: return "Falkland Islands (Malvinas)";
                case Code.FM: return "Micronesia, Federated States of";
                case Code.FO: return "Faroe Islands";
                case Code.FR: return "France";
                case Code.GA: return "Gabon";
                case Code.GB: return "United Kingdom";
                case Code.GD: return "Grenada";
                case Code.GE: return "Georgia";
                case Code.GF: return "French Guiana";
                case Code.GG: return "Guernsey";
                case Code.GH: return "Ghana";
                case Code.GI: return "Gibraltar";
                case Code.GL: return "Greenland";
                case Code.GM: return "Gambia";
                case Code.GN: return "Guinea";
                case Code.GP: return "Guadeloupe";
                case Code.GQ: return "Equatorial Guinea";
                case Code.GR: return "Greece";
                case Code.GS: return "South Georgia and the South Sandwich Islands";
                case Code.GT: return "Guatemala";
                case Code.GU: return "Guam";
                case Code.GW: return "Guinea-Bissau";
                case Code.GY: return "Guyana";
                case Code.HK: return "Hong Kong";
                case Code.HM: return "Heard Island and McDonald Islands";
                case Code.HN: return "Honduras";
                case Code.HR: return "Croatia";
                case Code.HT: return "Haiti";
                case Code.HU: return "Hungary";
                case Code.ID: return "Indonesia";
                case Code.IE: return "Ireland";
                case Code.IL: return "Israel";
                case Code.IM: return "Isle of Man";
                case Code.IN: return "India";
                case Code.IO: return "British Indian Ocean Territory";
                case Code.IQ: return "Iraq";
                case Code.IR: return "Iran, Islamic Republic of";
                case Code.IS: return "Iceland";
                case Code.IT: return "Italy";
                case Code.JE: return "Jersey";
                case Code.JM: return "Jamaica";
                case Code.JO: return "Jordan";
                case Code.JP: return "Japan";
                case Code.KE: return "Kenya";
                case Code.KG: return "Kyrgyzstan";
                case Code.KH: return "Cambodia";
                case Code.KI: return "Kiribati";
                case Code.KM: return "Comoros";
                case Code.KN: return "Saint Kitts and Nevis";
                case Code.KP: return "Korea, Democratic People's Republic of";
                case Code.KR: return "Korea, Republic of";
                case Code.KW: return "Kuwait";
                case Code.KY: return "Cayman Islands";
                case Code.KZ: return "Kazakhstan";
                case Code.LA: return "Lao People's Democratic Republic";
                case Code.LB: return "Lebanon";
                case Code.LC: return "Saint Lucia";
                case Code.LI: return "Liechtenstein";
                case Code.LK: return "Sri Lanka";
                case Code.LR: return "Liberia";
                case Code.LS: return "Lesotho";
                case Code.LT: return "Lithuania";
                case Code.LU: return "Luxembourg";
                case Code.LV: return "Latvia";
                case Code.LY: return "Libyan Arab Jamahiriya";
                case Code.MA: return "Morocco";
                case Code.MC: return "Monaco";
                case Code.MD: return "Moldova";
                case Code.ME: return "Montenegro";
                case Code.MF: return "Saint Martin (French part)";
                case Code.MG: return "Madagascar";
                case Code.MH: return "Marshall Islands";
                case Code.MK: return "Macedonia, the former Yugoslav Republic of";
                case Code.ML: return "Mali";
                case Code.MM: return "Myanmar";
                case Code.MN: return "Mongolia";
                case Code.MO: return "Macao";
                case Code.MP: return "Northern Mariana Islands";
                case Code.MQ: return "Martinique";
                case Code.MR: return "Mauritania";
                case Code.MS: return "Montserrat";
                case Code.MT: return "Malta";
                case Code.MU: return "Mauritius";
                case Code.MV: return "Maldives";
                case Code.MW: return "Malawi";
                case Code.MX: return "Mexico";
                case Code.MY: return "Malaysia";
                case Code.MZ: return "Mozambique";
                case Code.NA: return "Namibia";
                case Code.NC: return "New Caledonia";
                case Code.NE: return "Niger";
                case Code.NF: return "Norfolk Island";
                case Code.NG: return "Nigeria";
                case Code.NI: return "Nicaragua";
                case Code.NL: return "Netherlands";
                case Code.NO: return "Norway";
                case Code.NP: return "Nepal";
                case Code.NR: return "Nauru";
                case Code.NU: return "Niue";
                case Code.NZ: return "New Zealand";
                case Code.OM: return "Oman";
                case Code.PA: return "Panama";
                case Code.PE: return "Peru";
                case Code.PF: return "French Polynesia";
                case Code.PG: return "Papua New Guinea";
                case Code.PH: return "Philippines";
                case Code.PK: return "Pakistan";
                case Code.PL: return "Poland";
                case Code.PM: return "Saint Pierre and Miquelon";
                case Code.PN: return "Pitcairn";
                case Code.PR: return "Puerto Rico";
                case Code.PS: return "Palestinian Territory, Occupied";
                case Code.PT: return "Portugal";
                case Code.PW: return "Palau";
                case Code.PY: return "Paraguay";
                case Code.QA: return "Qatar";
                case Code.RE: return "Reunion";
                case Code.RO: return "Romania";
                case Code.RS: return "Serbia";
                case Code.RU: return "Russian Federation";
                case Code.RW: return "Rwanda";
                case Code.SA: return "Saudi Arabia";
                case Code.SB: return "Solomon Islands";
                case Code.SC: return "Seychelles";
                case Code.SD: return "Sudan";
                case Code.SE: return "Sweden";
                case Code.SG: return "Singapore";
                case Code.SH: return "Saint Helena";
                case Code.SI: return "Slovenia";
                case Code.SJ: return "Svalbard and Jan Mayen";
                case Code.SK: return "Slovakia";
                case Code.SL: return "Sierra Leone";
                case Code.SM: return "San Marino";
                case Code.SN: return "Senegal";
                case Code.SO: return "Somalia";
                case Code.SR: return "Suriname";
                case Code.ST: return "Sao Tome and Principe";
                case Code.SV: return "El Salvador";
                case Code.SY: return "Syrian Arab Republic";
                case Code.SZ: return "Swaziland";
                case Code.TC: return "Turks and Caicos Islands";
                case Code.TD: return "Chad";
                case Code.TF: return "French Southern Territories";
                case Code.TG: return "Togo";
                case Code.TH: return "Thailand";
                case Code.TJ: return "Tajikistan";
                case Code.TK: return "Tokelau";
                case Code.TL: return "Timor-Leste";
                case Code.TM: return "Turkmenistan";
                case Code.TN: return "Tunisia";
                case Code.TO: return "Tonga";
                case Code.TR: return "Turkey";
                case Code.TT: return "Trinidad and Tobago";
                case Code.TV: return "Tuvalu";
                case Code.TW: return "Taiwan, Province of China";
                case Code.TZ: return "Tanzania, United Republic of";
                case Code.UA: return "Ukraine";
                case Code.UG: return "Uganda";
                case Code.UM: return "United States Minor Outlying Islands";
                case Code.US: return "United States";
                case Code.UY: return "Uruguay";
                case Code.UZ: return "Uzbekistan";
                case Code.VA: return "Holy See (Vatican City State)";
                case Code.VC: return "Saint Vincent and the Grenadines";
                case Code.VE: return "Venezuela";
                case Code.VG: return "Virgin Islands, British";
                case Code.VI: return "Virgin Islands, U.S.";
                case Code.VN: return "Viet Nam";
                case Code.VU: return "Vanuatu";
                case Code.WF: return "Wallis and Futuna";
                case Code.WS: return "Samoa";
                case Code.YE: return "Yemen";
                case Code.YT: return "Mayotte";
                case Code.ZA: return "South Africa";
                case Code.ZM: return "Zambia";
                case Code.ZW: return "Zimbabwe";
                default: throw new Exception("Unknown country");
                }
            }
    }
}
