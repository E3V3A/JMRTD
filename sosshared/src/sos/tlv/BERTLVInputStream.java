/*
 * $Id: $
 */

package sos.tlv;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;

public class BERTLVInputStream extends InputStream
{
   /** Tag. */
   private int tag;

   /** Length. */
   private int length;

   /** Carrier. */
   private DataInputStream in;

   private static final int
   STATE_INIT = 0,
   STATE_TAG_READ = 1,
   STATE_LENGTH_READ = 2,
   STATE_UNDETERMINED = 3;

   private int state;

   /**
    * Constructs a new TLV stream based on another stream.
    * 
    * @param in a TLV object
    */
   public BERTLVInputStream(InputStream in) {
      this.in = (in instanceof DataInputStream) ? (DataInputStream)in: new DataInputStream(in);
      state = STATE_INIT;
   }

   public int readTag() throws IOException {
      if (state != STATE_INIT) {
         throw new IllegalStateException("Tag already read");
      }
      try {
         int b = in.readUnsignedByte();
         while (b == 0x00 || b == 0xFF) {
            // throw new IllegalArgumentException("00 or FF tag not allowed");
            b = in.readUnsignedByte(); /* skip 00 and FF */
         }
         switch (b & 0x1F) {
            case 0x1F:
               tag = b; /* We store the first byte including LHS nibble */
               b = in.readUnsignedByte();
               while ((b & 0x80) == 0x80) {
                  tag <<= 8;
                  tag |= (b & 0x7F);
                  b = in.readUnsignedByte();
               }
               tag <<= 8;
               tag |= (b & 0x7F); /*
                * Byte with MSB set is last byte of
                * tag...
                */
               break;
            default:
               tag = b;
            break;
         }
         state = STATE_TAG_READ;
         return tag;
      } catch (IOException e) {
         state = STATE_UNDETERMINED;
         throw e;
      }
   }

   public int readLength() throws IOException {
      if (state != STATE_TAG_READ) {
         throw new IllegalStateException("Tag not yet read or length already read");
      }
      try {
         length = 0;
         int b = in.readUnsignedByte();
         if ((b & 0x80) == 0x00) {
            /* short form */
            length = b;
         } else {
            /* long form */
            int count = b & 0x7F;
            length = 0;
            for (int i = 0; i < count; i++) {
               b = in.readUnsignedByte();
               length <<= 8;
               length |= b;
            }
         }
         state = STATE_LENGTH_READ;
         return length;
      } catch (IOException e) {
         state = STATE_UNDETERMINED;
         throw e;
      }
   }

   public byte[] readValue() throws IOException {
      if (state != STATE_LENGTH_READ) {
         throw new IllegalStateException("Length not yet read");
      }
      try {
         byte[] value = new byte[length];
         in.readFully(value);
         state = STATE_INIT;
         return value;
      } catch (IOException e) {
         state = STATE_UNDETERMINED;
         throw e;
      }
   }

   public void skipValue() throws IOException {
      if (state != STATE_LENGTH_READ) {
         throw new IllegalStateException("Tag and length not yet read");
      }
      try {
         in.skip(length);
         state = STATE_INIT;
      } catch (IOException e) {
         state = STATE_UNDETERMINED;
         throw e;
      }
   }

   public int available() throws IOException {
      return in.available();
   }

   /**
    * Warning: will set state to undetermined.
    */
   public int read() throws IOException {
      int result = in.read();
      state = STATE_UNDETERMINED;
      return result;
   }

   /**
    * Warning: will set state to undetermined.
    */
   public long skip(long n) throws IOException {
      long result = in.skip(n);
      state = STATE_UNDETERMINED;
      return result;
   }

   public synchronized void mark(int readLimit) {
   }

   public boolean markSupported() {
      return false;
   }

   public synchronized void reset() throws IOException {
      state = STATE_UNDETERMINED;
      throw new IOException("mark/reset not supported");
   }

   public void close() throws IOException {
      in.close();
   }

   private static boolean isPrimitive(int tag) {
      int i = 3;
      for (; i >= 0; i--) {
         int mask = (0xFF << (8 * i));
         if ((tag & mask) != 0x00) { break; }
      }
      int msByte = (((tag & (0xFF << (8 * i))) >> (8 * i)) & 0xFF);
      boolean result = ((msByte & 0x20) == 0x00);
      return result;
   }

   private void searchValue(int tag) throws IOException {
      while (true) {
         switch (state) {
            case STATE_INIT: readTag();
            case STATE_TAG_READ: readLength();
            case STATE_LENGTH_READ: break;
            default: throw new IllegalStateException("Cannot search value from undetermined state");
         }
         if (tag == this.tag) {
            return;
         }
         if (isPrimitive(this.tag)) {
            skipValue();
         }
      }
   }
}
