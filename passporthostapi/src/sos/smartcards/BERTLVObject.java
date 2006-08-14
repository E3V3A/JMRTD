/*
 * JMRTD - A Java API for accessing machine readable travel documents.
 *
 * Copyright (C) 2006  SoS group, ICIS, Radboud University
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA
 *
 * $Id$
 */

package sos.smartcards;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;

import sos.util.Hex;

/**
 * Generic data structure for storing Tag Length Value (TLV) objects encoded
 * according to the Basic Encoding Rules (BER). Written by Martijn Oostdijk (MO)
 * and Cees-Bart Breunesse (CB) of the Security of Systems group (SoS) of the
 * Institute of Computing and Information Sciences (ICIS) at Radboud University
 * (RU).
 * 
 * Based on ISO 7816-4 Annex D (which apparently is based on ISO 8825 and/or
 * X.208, X.209, X.680, X.690). See <a
 * href="http://en.wikipedia.org/wiki/ASN.1">ASN.1</a>.
 * 
 * @author Martijn Oostdijk (martijno@cs.ru.nl)
 * @author Cees-Bart Breunesse (ceesb@cs.ru.nl)
 * 
 * @version $Revision$
 */
public class BERTLVObject {
    private static final SimpleDateFormat SDF = new SimpleDateFormat("yyMMddhhmmss'Z'");

    /** Universal tag class. */
    public static final int UNIVERSAL_CLASS = 0;
    /** Application tag class. */
    public static final int APPLICATION_CLASS = 1;
    /** Context specific tag class. */
    public static final int CONTEXT_SPECIFIC_CLASS = 2;
    /** Private tag class. */
    public static final int PRIVATE_CLASS = 3;

    /** Universal tag type. */
    public static final int BOOLEAN_TYPE_TAG = 0x01,
                            INTEGER_TYPE_TAG = 0x02,
                            BIT_STRING_TYPE_TAG = 0x03,
                            OCTET_STRING_TYPE_TAG = 0x04,
                            NULL_TYPE_TAG = 0x05,
                            OBJECT_IDENTIFIER_TYPE_TAG = 0x06,
                            OBJECT_DESCRIPTOR_TYPE_TAG = 0x07,
                            EXTERNAL_TYPE_TAG = 0x08,
                            REAL_TYPE_TAG = 0x09,
                            ENUMERATED_TYPE_TAG = 0x0A,
                            EMBEDDED_PDV_TYPE_TAG = 0x0B,
                            UTF8_STRING_TYPE_TAG = 0x0C,
                            SEQUENCE_TYPE_TAG = 0x10,
                            SET_TYPE_TAG = 0x11,
                            NUMERIC_STRING_TYPE_TAG = 0x12,
                            PRINTABLE_STRING_TYPE_TAG = 0x13,
                            T61_STRING_TYPE_TAG = 0x14,
                            IA5_STRING_TYPE_TAG = 0x16,
                            UTC_TIME_TYPE_TAG = 0x17,
                            GENERALIZED_TIME_TYPE_TAG = 0x18,
                            GRAPHIC_STRING_TYPE_TAG = 0x19,
                            VISIBLE_STRING_TYPE_TAG = 0x1A,
                            GENERAL_STRING_TYPE_TAG = 0x1B,
                            UNIVERSAL_STRING_TYPE_TAG = 0x1C,
                            BMP_STRING_TYPE_TAG = 0x1E;

    private int tagClass;
    private boolean isPrimitive;

    /** Tag. */
    private int tag;

    /** Value, is usually just a byte[]. */
    private Object value;

    /**
     * Creates a new TLV object by parsing <code>in</code>.
     * 
     * @param in
     *            a binary representation of the TLV object
     * 
     * @return a TLV object
     * 
     * @throws IOException
     *             if something goes wrong
     */
    public static BERTLVObject getInstance(InputStream in) throws IOException {
        return new BERTLVObject(new DataInputStream(in));
    }

    /**
     * Constructs a new TLV object with tag <code>tag</code> containing data
     * <code>value</code>.
     * 
     * @param tagByte
     *            tag of TLV object
     * @param value
     *            data of TLV object
     * 
     * @throws IOException
     *             if something goes wrong.
     */
    public BERTLVObject(int tagBytes, Object value) throws IOException {
        byte[] tag = { (byte) ((tagBytes >>> 24) & 0xff), 
                       (byte) ((tagBytes >>> 16) & 0xFF),
                       (byte) ((tagBytes >>> 8) & 0xff),
                       (byte) (tagBytes & 0xff) };
        readTag(new DataInputStream(new ByteArrayInputStream(tag)));
        if (isPrimitive) {
            this.value = value;
        } else {
            // arrays are interpreted (maybe remove this?)
            if (value instanceof byte[]) {
                byte[] valueBytes = (byte[]) value;
                readValue(new DataInputStream(new ByteArrayInputStream(valueBytes)),
                          valueBytes.length);
            }
            // BERTLVObjects are added as a child
            else if (value instanceof BERTLVObject) {
                this.value = new BERTLVObject[1];
                ((BERTLVObject[]) this.value)[0] = (BERTLVObject) value;
            } else if (value instanceof Integer){
                this.value = new BERTLVObject[1];
                ((BERTLVObject[]) this.value)[0] = new BERTLVObject(INTEGER_TYPE_TAG, value);
            } else {
                throw new IllegalArgumentException("Cannot encode value of type: "
                                                   + value.getClass());
            }

        }

    }

    /**
     * Constructs a new TLV object by parsing input <code>in</code>.
     * 
     * @param in
     *            a TLV object
     * @throws IOException
     *             if something goes wrong
     */
    private BERTLVObject(DataInputStream in) throws IOException {
        readTag(in);
        int length = readLength(in);
        readValue(in, length);
    }

    private void readTag(DataInputStream in) throws IOException {
        int b = in.readUnsignedByte();
        while (b == 0x00000000 || b == 0x000000FF) {
            // throw new IllegalArgumentException("00 or FF tag not allowed");
            b = in.readUnsignedByte(); /* skip 00 and FF */
        }
        switch (b & 0x000000C0) {
        case 0x00000000:
            tagClass = UNIVERSAL_CLASS;
            break;
        case 0x00000040:
            tagClass = APPLICATION_CLASS;
            break;
        case 0x00000080:
            tagClass = CONTEXT_SPECIFIC_CLASS;
            break;
        case 0x000000C0:
            tagClass = PRIVATE_CLASS;
            break;
        }
        switch (b & 0x00000020) {
        case 0x00000000:
            isPrimitive = true;
            break;
        case 0x00000020:
            isPrimitive = false;
            break;
        }
        switch (b & 0x0000001F) {
        case 0x0000001F:
            tag = b; /* We store the first byte including LHS nibble */
            b = in.readUnsignedByte();
            while ((b & 0x00000080) == 0x00000080) {
                tag <<= 8;
                tag |= (b & 0x0000007F);
                b = in.readUnsignedByte();
            }
            tag <<= 8;
            tag |= (b & 0x0000007F); /*
                                         * Byte with MSB set is last byte of
                                         * tag...
                                         */
            break;
        default:
            tag = b;
            break;
        }
    }

    private int readLength(DataInputStream in) throws IOException {
        int length = 0;
        int b = in.readUnsignedByte();
        if ((b & 0x00000080) == 0x00000000) {
            /* short form */
            length = b;
        } else {
            /* long form */
            int count = b & 0x0000007F;
            length = 0;
            for (int i = 0; i < count; i++) {
                b = in.readUnsignedByte();
                length <<= 8;
                length += b;
            }
        }
        return length;
    }

    private void readValue(DataInputStream in, int length) throws IOException {
        value = new byte[length];
        in.readFully((byte[]) value);
        if (isPrimitive) {
            /*
             * Primitive, the value consists of 0 or more Simple-TLV objects, or
             * just (application-dependent) bytes. If tag is not known (or
             * universal) we assume the value is just bytes.
             */
            if (tagClass == UNIVERSAL_CLASS)
                switch (tag) {
                case INTEGER_TYPE_TAG:
                    break;
                case BIT_STRING_TYPE_TAG:
                    break;
                case OCTET_STRING_TYPE_TAG:
                    break;
                case NULL_TYPE_TAG:
                    break;
                case OBJECT_IDENTIFIER_TYPE_TAG:
                    break;
                case UTF8_STRING_TYPE_TAG:
                case PRINTABLE_STRING_TYPE_TAG:
                case T61_STRING_TYPE_TAG:
                case IA5_STRING_TYPE_TAG:
                case VISIBLE_STRING_TYPE_TAG:
                case GENERAL_STRING_TYPE_TAG:
                case UNIVERSAL_STRING_TYPE_TAG:
                case BMP_STRING_TYPE_TAG:
                    value = new String((byte[]) value);
                    break;
                case UTC_TIME_TYPE_TAG:
                    try {
                        value = SDF.parse(new String((byte[]) value));
                        break;
                    } catch (ParseException pe) {
                    }
                default:
                }
        } else {
            /*
             * Not primitive, the value itself consists of 0 or more BER-TLV
             * objects.
             */
            in = new DataInputStream(new ByteArrayInputStream((byte[]) value));
            value = readSubObjects(in);
        }
    }

    /***************************************************************************
     * Adds
     * <code>object</object> as subobject of <code>this</code> TLV object when
     * <code>this</code> is not a primitive object.
     * 
     * @param object to add as a subobject.
     */
    public void addSubObject(BERTLVObject object) {
        ArrayList subObjects;

        if (!isPrimitive) {
            if (value != null) {
                subObjects = new ArrayList(Arrays.asList((BERTLVObject[]) value));
            } else {
                subObjects = new ArrayList();
                value = new BERTLVObject[1];
            }
            subObjects.add(object);
            value = subObjects.toArray((BERTLVObject[]) value);
        }
    }

    private static BERTLVObject[] readSubObjects(DataInputStream in)
            throws IOException {
        ArrayList subObjects = new ArrayList();
        while (in.available() > 0) {
            subObjects.add(new BERTLVObject(in));
        }
        BERTLVObject[] result = new BERTLVObject[subObjects.size()];
        subObjects.toArray(result);
        return result;
    }

    /**
     * The tag bytes of this object.
     * 
     * @return the tag bytes of this object.
     */
    public byte[] getTagAsBytes() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        int byteCount = (int) (Math.log(tag) / Math.log(256)) + 1;
        for (int i = 0; i < byteCount; i++) {
            int pos = 8 * (byteCount - i - 1);
            out.write((tag & (0xFF << pos)) >> pos);
        }
        byte[] tagBytes = out.toByteArray();
        switch (tagClass) {
        case APPLICATION_CLASS:
            tagBytes[0] |= 0x40;
            break;
        case CONTEXT_SPECIFIC_CLASS:
            tagBytes[0] |= 0x80;
            break;
        case PRIVATE_CLASS:
            tagBytes[0] |= 0xC0;
            break;
        }
        if (!isPrimitive) {
            tagBytes[0] |= 0x20;
        }
        return tagBytes;
    }

    /**
     * The length of the encoded value.
     * 
     * @return the length of the encoded value
     */
    public int getLength()  { // TODO: Why not simply 'return this.length'? -- MO
        return getValueAsBytes().length;
    }

    /**
     * The length bytes of this object.
     * 
     * @return length of encoded value as bytes
     */
    public byte[] getLengthAsBytes() {
        int length = getLength();

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        if (length < 0x00000080) {
            /* short form */
            out.write(length);
        } else {
            int byteCount = (int) (Math.log(length) / Math.log(256)) + 1;
            out.write(0x80 | byteCount);
            for (int i = 0; i < byteCount; i++) {
                int pos = 8 * (byteCount - i - 1);
                out.write((length & (0xFF << pos)) >> pos);
            }
        }
        return out.toByteArray();
    }

    /**
     * The encoded value.
     * 
     * @return the encoded value
     */
    public Object getValue() {
        return value;
    }

    /**
     * The value of this object as a byte array. Almost the same as
     * getEncoded(), but this one skips the tag and length of <code>this</code>
     * BERTLVObject.
     * 
     * @return the value of this object as a byte array
     */
    public byte[] getValueAsBytes() {
        if (isPrimitive) {
            if (value instanceof byte[]) {
                return (byte[]) value;
            } else if (value instanceof String) {
                return ((String) value).getBytes();
            } else if (value instanceof Date) {
                return SDF.format((Date) value).getBytes();
            } else if (value instanceof Integer) {
                int intValue = ((Integer)value).intValue(); 
                int byteCount = Integer.bitCount(intValue)/8 + 1;
                byte[] result = new byte[byteCount];
                for (int i = 0; i < byteCount; i++) {
                    int pos = 8 * (byteCount - i - 1);
                    result[i] = (byte)((intValue & (0xFF << pos)) >> pos);
                }
                return result;
            }
            throw new IllegalStateException("Cannot decode value of type: "
                    + value.getClass());
        } else {
            ByteArrayOutputStream result = new ByteArrayOutputStream();
            BERTLVObject[] children = (BERTLVObject[]) getValue();
            for (int i = 0; i < children.length; i++) {
                try {
                    result.write(children[i].getEncoded());
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
            return result.toByteArray();
        }
    }

    /**
     * The tag class.
     * 
     * @return the tag class
     */
    public int getTagClass() {
        return tagClass;
    }

    /**
     * Indicates whether this tag is primitive.
     * 
     * @return whether this tag is primitive
     */
    public boolean isPrimitive() {
        return isPrimitive;
    }

    /**
     * This object, including tag and length, as byte array.
     * 
     * @return this object, including tag and length, as byte array
     */
    public byte[] getEncoded() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            out.write(getTagAsBytes());
            out.write(getLengthAsBytes());
            out.write(getValueAsBytes());
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
        return out.toByteArray();
    }

    /**
     * Gets the first sub-object (including this object) whose tag equals
     * <code>tag</code>.
     * 
     * @param tag
     *            the tag to search for
     * @return the first
     */
    public BERTLVObject getSubObject(int tag) {
        if (this.tag == tag) {
            return this;
        } else if (value instanceof BERTLVObject[]) {
            BERTLVObject[] children = (BERTLVObject[]) value;
            for (int i = 0; i < children.length; i++) {
                BERTLVObject child = children[i];
                BERTLVObject candidate = child.getSubObject(tag);
                if (candidate != null) {
                    return candidate;
                }
            }
        }
        return null;
    }

    /**
     * Gets the first sub-object (including this object) following the tags in
     * tagPath.
     * 
     * @param tagPath
     *            the path to follow
     * @param offset
     *            in the tagPath
     * @param length
     *            of the tagPath
     * @return the first
     */
    public BERTLVObject getSubObject(int[] tagPath, int offset, int length) {
        if (length == 0) {
            return this;
        } else {
            BERTLVObject child = getSubObject(tagPath[offset]);
            if (child != null) {
                return child.getSubObject(tagPath, offset + 1, length - 1);
            }
        }
        return null;
    }

    /***************************************************************************
     * Returns the indexed child (starting from 0) or null otherwise.
     * 
     * @param index
     * @return the object pointed to by index.
     */
    public BERTLVObject getChildByIndex(int index) {

        if (value instanceof BERTLVObject[]) {
            BERTLVObject[] children = (BERTLVObject[]) value;
            return children[index];
        }

        return null;
    }

    /**
     * A textual (nested tree-like) representation of this object. Always ends
     * in newline character, no need to add it yourself.
     * 
     * @return a textual representation of this object.
     * 
     * @see java.lang.Object#toString()
     */
    public String toString() {
        return toString(0);
    }

    private String toString(int indent) {
        byte[] prefixBytes = new byte[indent];
        Arrays.fill(prefixBytes, (byte) ' ');
        String prefix = new String(prefixBytes);
        StringBuffer result = new StringBuffer();
        result.append(prefix);
        result.append(tagToString());
        result.append(" ");
        result.append(Integer.toString(getLength()));
        result.append(" ");
        if (value instanceof byte[]) {
            byte[] valueData = (byte[]) value;
            result.append("'0x");
            if (indent + 2 * valueData.length <= 60) {
                result.append(Hex.bytesToHexString(valueData));
            } else {
                result.append(Hex.bytesToHexString(valueData,
                                                   0,
                                                   (50 - indent) / 2));
                result.append("...");
            }
            result.append("'\n");
        } else if (value instanceof BERTLVObject[]) {
            result.append("{\n");
            BERTLVObject[] subObjects = (BERTLVObject[]) value;
            for (int i = 0; i < subObjects.length; i++) {
                result.append(subObjects[i].toString(indent + 3));
            }
            result.append(prefix);
            result.append("}\n");
        } else {
            result.append("\"");
            result.append(value != null ? value.toString() : "null");
            result.append("\"\n");
        }
        return result.toString();
    }

    private String tagToString() {
        if (getTagClass() == UNIVERSAL_CLASS) {
            if (isPrimitive()) {
                switch (tag & 0x1F) {
                case BOOLEAN_TYPE_TAG:
                    return "BOOLEAN";
                case INTEGER_TYPE_TAG:
                    return "INTEGER";
                case BIT_STRING_TYPE_TAG:
                    return "BIT_STRING";
                case OCTET_STRING_TYPE_TAG:
                    return "OCTET_STRING";
                case NULL_TYPE_TAG:
                    return "NULL";
                case OBJECT_IDENTIFIER_TYPE_TAG:
                    return "OBJECT_IDENTIFIER";
                case REAL_TYPE_TAG:
                    return "REAL";
                case UTF8_STRING_TYPE_TAG:
                    return "UTF_STRING";
                case PRINTABLE_STRING_TYPE_TAG:
                    return "PRINTABLE_STRING";
                case T61_STRING_TYPE_TAG:
                    return "T61_STRING";
                case IA5_STRING_TYPE_TAG:
                    return "IA5_STRING";
                case VISIBLE_STRING_TYPE_TAG:
                    return "VISIBLE_STRING";
                case GENERAL_STRING_TYPE_TAG:
                    return "GENERAL_STRING";
                case UNIVERSAL_STRING_TYPE_TAG:
                    return "UNIVERSAL_STRING";
                case BMP_STRING_TYPE_TAG:
                    return "BMP_STRING";
                case UTC_TIME_TYPE_TAG:
                    return "UTC_TIME";
                case GENERALIZED_TIME_TYPE_TAG:
                    return "GENERAL_TIME";
                }
            } else {
                switch (tag & 0x1F) {
                case ENUMERATED_TYPE_TAG:
                    return "ENUMERATED";
                case SEQUENCE_TYPE_TAG:
                    return "SEQUENCE";
                case SET_TYPE_TAG:
                    return "SET";
                }
            }
        }
        return "'0x" + Hex.intToHexString(tag) + "'";
    }
    
    /* For testing */
    public static void main(String[] args) {
        FileInputStream in=null;
        try {
            in = new FileInputStream(args[0]);
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        BERTLVObject object=null;
        try {
            object = new BERTLVObject(new DataInputStream(in));
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        System.out.println(object);
    }
    
    
}
