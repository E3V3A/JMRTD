package sos.mrtd;

import java.io.InputStream;

public class CVCAFile extends PassportFile {

    public static final byte CAR_TAG = 0x42;
    public static final int LENGTH = 36;

    private String caReference = null;

    private String altCaReference = null;

    /**
     * Constructs a new CVCA file from the data contained in <code>in</code>.
     * 
     * @param input
     *            stream with the data to be parsed
     */
    public CVCAFile(InputStream in) {
        try {
            int tag = in.read();
            if (tag != CAR_TAG) {
                throw new IllegalArgumentException("Wrong tag.");
            }
            int len = in.read();
            if (len > 16) {
                throw new IllegalArgumentException("Wrong length.");
            }
            byte[] data = new byte[len];
            in.read(data);
            caReference = new String(data);
            tag = in.read();
            if (tag != 0) {
                if (tag != CAR_TAG) {
                    throw new IllegalArgumentException("Wrong tag.");
                }
                len = in.read();
                if (len > 16) {
                    throw new IllegalArgumentException("Wrong length.");
                }
                data = new byte[len];
                in.read(data);
                altCaReference = new String(data);
                tag = in.read();
            }
            while (tag != -1) {
                if (tag != 0) {
                    throw new IllegalArgumentException("Bad file padding.");
                }
                tag = in.read();
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new IllegalArgumentException("Malformed input data");
        }
    }

    /**
     * Constructs a new CVCA file with the given certificate references
     * 
     * @param caReference
     *            main CA certificate reference
     * @param altCaReference
     *            second (alternative) CA certificate reference
     */
    public CVCAFile(String caReference, String altCaReference) {
        if (caReference == null || caReference.length() > 16
                || (altCaReference != null && altCaReference.length() > 16)) {
            throw new IllegalArgumentException();
        }
        this.caReference = caReference;
        this.altCaReference = altCaReference;
    }

    /**
     * Constructs a new CVCA file with the given certificate reference
     * 
     * @param caReference
     *            main CA certificate reference
     */
    public CVCAFile(String caReference) {
        this(caReference, null);
    }

    public byte[] getEncoded() {
        byte[] result = new byte[LENGTH];
        result[0] = CAR_TAG;
        result[1] = (byte) caReference.length();
        System.arraycopy(caReference.getBytes(), 0, result, 2, result[1]);
        if (altCaReference != null) {
            int index = result[1] + 2;
            result[index] = CAR_TAG;
            result[index + 1] = (byte) altCaReference.length();
            System.arraycopy(altCaReference.getBytes(), 0, result, index + 2,
                    result[index + 1]);
        }
        return result;
    }

    /**
     * Returns the CA Certificate identifier
     * 
     * @return the CA Certificate identifier
     */
    public String getCAReference() {
        return caReference;
    }

    /**
     * Returns the second (alternative) CA Certificate identifier, null if none
     * exists.
     * 
     * @return the second (alternative) CA Certificate identifier
     */
    public String getAltCAReference() {
        return altCaReference;
    }

}
