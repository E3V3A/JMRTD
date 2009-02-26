package sos.mrtd;

import org.bouncycastle.asn1.DERObjectIdentifier;

/**
 * This class storos different OIDs for EAC, version 1.11. See TR-03110 Version 1.11.
 * Some of these are probably also defined elsewhere (eg. the ca-cvcert library), but
 * we want to stay partly independent.
 * 
 * @author Wojciech Mostowski <woj@cs.ru.nl>
 *
 */
public interface EACObjectIdentifiers {

    /** The prefix */
    public static final String STRING_BSI_DE = "0.4.0.127.0.7";

    public static final String STRING_ID_PK = STRING_BSI_DE + ".2.2.1";
    public static final String STRING_ID_PK_DH = STRING_ID_PK + ".1";
    public static final String STRING_ID_PK_ECDH = STRING_ID_PK + ".2";
    
    public static final DERObjectIdentifier OID_ID_PK_DH = new DERObjectIdentifier(STRING_ID_PK_DH);
    public static final DERObjectIdentifier OID_ID_PK_ECDH = new DERObjectIdentifier(STRING_ID_PK_ECDH);
    
    public static final String STRING_ID_CA = STRING_BSI_DE + ".2.2.3";
    public static final String STRING_ID_CA_DH = STRING_ID_CA + ".1";
    public static final String STRING_ID_CA_ECDH = STRING_ID_CA + ".2";
    
    public static final String STRING_ID_CA_DH_3DES_CBC_CBC = STRING_ID_CA_DH + ".1";
    public static final String STRING_ID_CA_ECDH_3DES_CBC_CBC = STRING_ID_CA_ECDH + ".1";
    
    public static final DERObjectIdentifier OID_ID_CA_DH_3DES_CBC_CBC = new DERObjectIdentifier(STRING_ID_CA_DH_3DES_CBC_CBC);
    public static final DERObjectIdentifier OID_ID_CA_ECDH_3DES_CBC_CBC = new DERObjectIdentifier(STRING_ID_CA_ECDH_3DES_CBC_CBC);

    public static final String STRING_ID_TA = STRING_BSI_DE + ".2.2.2";
    
    public static final DERObjectIdentifier OID_ID_TA = new DERObjectIdentifier(STRING_ID_TA);
    
    public static final String STRING_ID_TA_RSA = STRING_ID_TA + ".1";
    public static final String STRING_ID_TA_ECDSA = STRING_ID_TA + ".2";
    
    public static final String STRING_ID_TA_RSA_PKCS15_SHA1 = STRING_ID_TA_RSA + ".1";
    public static final String STRING_ID_TA_RSA_PKCS15_SHA256 = STRING_ID_TA_RSA + ".2";
    public static final String STRING_ID_TA_RSA_PSS_SHA1 = STRING_ID_TA_RSA + ".3";
    public static final String STRING_ID_TA_RSA_PSS_SHA256 = STRING_ID_TA_RSA + ".4";
    
    public static final String STRING_ID_TA_ECDSA_SHA1 = STRING_ID_TA_ECDSA + ".1";
    public static final String STRING_ID_TA_ECDSA_SHA224 = STRING_ID_TA_ECDSA + ".2";
    public static final String STRING_ID_TA_ECDSA_SHA256 = STRING_ID_TA_ECDSA + ".3";

}
