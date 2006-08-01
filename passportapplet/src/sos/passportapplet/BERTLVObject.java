package sos.passportapplet;

/***
 * This class stores a BERTLV structure as a tree. The child pointer goes down
 * the tree. The next pointer visits BERTLV objects on the same level.
 * 
 * @author Cees-Bart Breunesse (ceesb@cs.ru.nl)
 * 
 * @see sos.mrtd.smartcards.BERTLVObject
 * 
 */
public class BERTLVObject {
    /** Universal tag class. */
    public static final int UNIVERSAL_CLASS = 0;
    /** Application tag class. */
    public static final int APPLICATION_CLASS = 1;
    /** Context specific tag class. */
    public static final int CONTEXT_SPECIFIC_CLASS = 2;
    /** Private tag class. */
    public static final int PRIVATE_CLASS = 3;

    short tag;
    short tagClass;
    boolean isPrimitive;
//    public byte[] value;
    short valueOffset;
    short valueLength;

    BERTLVObject next;
    BERTLVObject child;

    private BERTLVObject() {
    }

    private short readBERTLVObject(byte[] in, short offset, short length) {
//        value = in;
        short in_p = offset;
        short len = length;
        in_p = readTag(in, in_p, len);
        len = (short) (length - (in_p - offset));
        readLength(in, in_p, len);
        in_p = readValue(in);
        return in_p;
    }

    /**
     * Returns the root of a tree of BERTLV objects. Does not copy contents of
     * in buffer, but only stores pointers.
     * 
     * @param in
     *            buffer containing a BERTLV structure
     * @param offset
     *            in buffer
     * @param length
     *            of buffer
     * @return root of BERTLV tree
     */
    public static BERTLVObject readObjects(byte[] in, short offset, short length) {
        short in_p = offset;
        short len = length;
        BERTLVObject first = null;
        BERTLVObject current, previous = null;

        while (in_p < (short) (offset + length)) {
            current = new BERTLVObject();
            if (previous != null) {
                previous.next = current;
            }

            if (in_p == offset) {
                first = current;
            }

            in_p = current.readBERTLVObject(in, in_p, len);
            len = (short) (length - (in_p - offset));
            previous = current;
        }
        return first;

    }

    private short readTag(byte[] in, short offset, short length) {
        short in_p = offset;
        short b = (short) (in[in_p] & 0xff);
        while (b == 0 || b == 0xff) {
            in_p++;
            b = in[in_p]; /* skip 00 and FF */
        }
        switch (b & 0xC0) {
        case 0:
            tagClass = UNIVERSAL_CLASS;
            break;
        case 0x40:
            tagClass = APPLICATION_CLASS;
            break;
        case 0x80:
            tagClass = CONTEXT_SPECIFIC_CLASS;
            break;
        case 0xC0:
            tagClass = PRIVATE_CLASS;
            break;
        }
        switch (b & 0x20) {
        case 0:
            isPrimitive = true;
            break;
        case 0x20:
            isPrimitive = false;
            break;
        }
        switch (b & 0x1F) {
        case 0x1F:
            tag = b;
            in_p++;
            b = in[in_p];
            while ((b & 0x80) == 0x80) {
                tag <<= 8;
                tag |= (b & 0x7F);
                in_p++;
                b = in[in_p];
            }
            tag <<= 8;
            tag |= (b & 0x7F);
            break;
        default:
            tag = b;
            break;
        }
        in_p++;
        return in_p;
    }

    private short readLength(byte[] in, short offset, short length) {
        short in_p = offset;
        short b = (short) (in[offset] & 0xff);
        if ((b & 0x80) == 0) {
            /* short form */
            valueLength = b;
        } else {
            /* long form */
            short count = (short) (b & 0x7F);
            valueLength = 0;
            for (short i = 0; i < count; i++) {
                in_p++;
                b = (short) (in[in_p] & 0xff);
                ;
                valueLength <<= 8;
                valueLength += b;
            }
        }
        valueOffset = (short) (in_p + 1);
        return valueOffset;
    }

    private short readValue(byte[] in) {
        if (!isPrimitive) {
            child = readObjects(in, valueOffset, valueLength); 
                                                               
        } else {
            child = null;
        }
        return (short) (valueOffset + valueLength);
    }

}
