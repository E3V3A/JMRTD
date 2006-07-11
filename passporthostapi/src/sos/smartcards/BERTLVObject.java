/*
 * JMRTD - A Java API for accessing machine readable travel documents.
 *
 * Copyright (C) 2006  SoS group, Radboud University
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
import java.io.DataInputStream;
import java.io.FileInputStream;
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
 * according to the Basic Encoding Rules (BER). Written by Martijn Oostdijk
 * (MO) of the Security of Systems group (SoS) of the Institute of Computing and
 * Information Sciences (ICIS) at Radboud University (RU).
 *
 * Based on ISO 7816-4 Annex D (which apparently is based on ISO 8825).
 *
 * @author Martijn Oostdijk (martijno@cs.ru.nl)
 *
 * @version $Revision$
 */
public class BERTLVObject
{
   /** Universal tag class. */
   public static final int UNIVERSAL_CLASS = 0;
   
   /** Application tag class. */
   public static final int APPLICATION_CLASS = 1;
   
   /** Context specific tag class. */
   public static final int CONTEXT_SPECIFIC_CLASS = 2;
   
   /** Private tag class. */
   public static final int PRIVATE_CLASS = 3;

   public static final int INTEGER_TYPE_TAG = 0x02;
   public static final int BIT_STRING_TYPE_TAG = 0x03;
   public static final int OCTET_STRING_TYPE_TAG = 0x04;
   public static final int NULL_TYPE_TAG = 0x05;
   public static final int OBJECT_IDENTIFIER_TYPE_TAG = 0x06;
   public static final int SEQUENCE_TYPE_TAG = 0x10;
   public static final int SET_TYPE_TAG = 0x11;
   public static final int PRINTABLE_STRING_TYPE_TAG = 0x13;
   public static final int T61_STRING_TYPE_TAG = 0x14;
   public static final int IA5_STRING_TYPE_TAG = 0x16;
   public static final int UTC_TIME_TYPE_TAG = 0x17;
   
   private int tagClass;
   private boolean isPrimitive;

   /** Tag. */
   private byte[] tag;

   /** Length in bytes, at most 65535. */
   private int length;

   /** Value, is usually just a byte[]. */
   private Object value;
   private byte[] valueBytes;

   /**
    * Creates a new TLV object by parsing <code>in</code>.
    * 
    * @param in a binary representation of the TLV object
    * @return a TLV object
    * @throws IOException if something goes wrong
    */
   public static BERTLVObject getInstance(InputStream in) throws IOException {
      return new BERTLVObject(new DataInputStream(in));
   }
   
   /**
    * Constructs a new TLV object by parsing input <code>in</code>.
    * 
    * @param in a TLV object
    * @throws IOException if something goes wrong
    */
   private BERTLVObject(DataInputStream in) throws IOException {
      readTag(in);
      readLength(in);
      readValue(in);
   }

   private void readTag(DataInputStream in) throws IOException {
      int b = in.readUnsignedByte();
      if (b == 0x00000000 || b == 0x000000FF) {
         throw new IllegalArgumentException("00 or FF tag not allowed");
      }
      switch (b & 0x000000C0) {
         case 0x00000000: tagClass = UNIVERSAL_CLASS; break;
         case 0x00000040: tagClass = APPLICATION_CLASS; break;
         case 0x00000080: tagClass = CONTEXT_SPECIFIC_CLASS; break;
         case 0x000000C0: tagClass = PRIVATE_CLASS; break;
      }
      switch (b & 0x00000020) {
         case 0x00000000: isPrimitive = true; break;
         case 0x00000020: isPrimitive = false; break;
      }
      switch (b & 0x0000001F) {
         case 0x0000001F:
            ArrayList tagBytes = new ArrayList();
            tagBytes.add(new Integer(b));
            b = in.readUnsignedByte();
            while ((b & 0x00000080) == 0x00000080) {
               tagBytes.add(new Integer(b));
            }
            tagBytes.add(new Integer(b));
            tag = new byte[tagBytes.size()];
            for (int i = 0; i < tagBytes.size(); i++) {
               tag[i] = (byte)(((Integer)tagBytes.get(i)).intValue());
            }
            break;
         default:
            tag = new byte[1]; tag[0] = (byte)b; break;
      }
      if (tag == null) {
         throw new NumberFormatException("Could not read tag");
      }
   }

   private void readLength(DataInputStream in) throws IOException {
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
   }

   private void readValue(DataInputStream in) throws IOException {
      valueBytes = new byte[length];
      in.readFully(valueBytes);
      if (isPrimitive) {
         /*
          * Primitive, the value consists of 0 or more Simple-TLV objects,
          * or just (application-dependent) bytes. If tag is not known
          * (or universal) we assume the value is just bytes.
          */
    	  
    	  if (tagClass == UNIVERSAL_CLASS)
    	  switch (tag[0]) {
    	     case INTEGER_TYPE_TAG: value = valueBytes; break;
    	     case BIT_STRING_TYPE_TAG: value = valueBytes; break;
       	     case OCTET_STRING_TYPE_TAG: value = valueBytes; break;
    	     case NULL_TYPE_TAG: value = null; break;
    	     case OBJECT_IDENTIFIER_TYPE_TAG: value = valueBytes; break;
    	     case SEQUENCE_TYPE_TAG: value = valueBytes; break;
    	     case SET_TYPE_TAG: value = valueBytes; break;
             case 0x0C: /* FIXME: find out what 12 is... */
    	     case PRINTABLE_STRING_TYPE_TAG:
    	     case T61_STRING_TYPE_TAG:
    	     case IA5_STRING_TYPE_TAG: value = new String(valueBytes); break;
    	     case UTC_TIME_TYPE_TAG: value = parseUTCTime(new String(valueBytes)); break;
    	     default: value = valueBytes;
    	  } else {
             value = valueBytes;   
          }
      } else {
         /*
          * Not primitive, the value itself consists of 0 or more
          * BER-TLV objects.
          */
         in = new DataInputStream(new ByteArrayInputStream(valueBytes));
         value = readSubObjects(in);
      }
   }
   
   private static Date parseUTCTime(String in) {
	   try {
	      SimpleDateFormat sdf = new SimpleDateFormat("yyMMddhhmmss'Z'");
	      return sdf.parse(in);
	   } catch (ParseException pe) {
		  return null;
	   }
   }
   
   private BERTLVObject[] readSubObjects(DataInputStream in)
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
   public byte[] getTag() {
      return tag;
   }

   /**
    * The length of the encoded value.
    * 
    * @return the length of the encoded value.
    */
   public int getLength() {
      return length;
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
    * The value of this object as a byte array.
    * 
    * @return the value of this object as a byte array
    */
   public byte[] getValueAsBytes() {
      return valueBytes;
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
    * Gets the first sub-object (including this object) whose
    * tag equals <code>tag</code>.
    * 
    * @param tag the tag to search for
    * @return the first
    */
   public BERTLVObject getChild(byte[] tag) {
      if (Arrays.equals(this.tag, tag)) {
         return this;
      } else if (value instanceof BERTLVObject[]) {
         BERTLVObject[] children = (BERTLVObject[])value;
         for (int i = 0; i < children.length; i++) {
            BERTLVObject child = children[i];
            BERTLVObject candidate = child.getChild(tag);
            if (candidate != null) {
               return candidate;
            }
         }
      }
      return null;
   }
   
   /**
    * A textual (nested tree-like) representation of this object.
    * Always ends in newline character, no need to add it yourself.
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
      Arrays.fill(prefixBytes, (byte)' ');
      String prefix = new String(prefixBytes);
      StringBuffer result = new StringBuffer();
      result.append(prefix); result.append("'0x");
      result.append(Hex.bytesToHexString(tag)); result.append("' ");
      result.append(Integer.toString(length)); result.append(" ");
      if (value instanceof byte[]) {
         byte[] valueData = (byte[])value;
         result.append("'0x");
         if (indent + 2 * valueData.length <= 60) {
            result.append(Hex.bytesToHexString(valueData));
         } else {
            result.append(Hex.bytesToHexString(valueData,
                                               0, (50 - indent) / 2));
            result.append("...");
         }
         result.append("'\n");
      } else if (value instanceof BERTLVObject[]) {
         result.append("\n");
         BERTLVObject[] subObjects = (BERTLVObject[])value;
         for (int i = 0; i < subObjects.length; i++) {
            result.append(subObjects[i].toString(indent + 3));
         }
         result.append(prefix);
         result.append("\n");
      } else if (value instanceof String) {
    	  result.append("\"");
          result.append(value.toString());
    	  result.append("\"\n");
      } else {
         result.append(value != null ? value.toString() : "null");
         result.append("\n");
      }
      return result.toString();
   }
   
   public static final void main(String[] arg) {
	   try {
	      FileInputStream in = new FileInputStream("/tmp/ef011D.bin");
	      BERTLVObject obj = BERTLVObject.getInstance(in);
	      System.out.println(obj);
	   } catch (Exception e) {
		   e.printStackTrace();
	   }
   }
}

