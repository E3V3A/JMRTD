using System;
using System.IO;
using System.Collections.Generic;

namespace nl.telin.authep.lib
{
    /// <summary>
    /// TLV input stream.
    /// </summary>
    public class BERTLVInputStream
    {
        /** Carrier. */
        private Stream _stream;

        private State state;

        /// <summary>
        /// Constructs a new TLV stream based on another stream.
        /// </summary>
        /// <param name="stream">A TLV Stream</param>
        public BERTLVInputStream(Stream stream)
        {
            _stream = stream;
            state = new State();
        }

        /// <summary>
        /// Read tag of the next object.
        /// </summary>
        /// <returns>Type of the object.</returns>
        public int readTag()
        {
            int tag = -1;
            int bytesRead = 0;
            try
            {
                int b = _stream.ReadByte(); bytesRead++;
                while (b == 0x00 || b == 0xFF)
                {
                    b = _stream.ReadByte(); bytesRead++; /* skip 00 and FF */
                }
                switch (b & 0x1F)
                {
                    case 0x1F:
                        tag = b; /* We store the first byte including LHS nibble */
                        b = _stream.ReadByte(); bytesRead++;
                        while ((b & 0x80) == 0x80)
                        {
                            tag <<= 8;
                            tag |= (b & 0x7F);
                            b = _stream.ReadByte(); bytesRead++;
                        }
                        tag <<= 8;
                        tag |= (b & 0x7F);
                        /*
                         * Byte with MSB set is last byte of
                         * tag...
                         */
                        break;
                    default:
                        tag = b;
                        break;
                }
                state.setTagRead(tag, bytesRead);
                return tag;
            }
            catch (IOException e)
            {
                throw e;
            }
        }

        /// <summary>
        /// Read the length of the next object.
        /// </summary>
        /// <returns>Length of the next object.</returns>
        public int readLength()
        {
            try
            {
                if (!state.isAtStartOfLength()) { throw new Exception("Not at start of length"); }
                int bytesRead = 0;
                int length = 0;
                int b = _stream.ReadByte(); bytesRead++;
                if ((b & 0x80) == 0x00)
                {
                    /* short form */
                    length = b;
                }
                else
                {
                    /* long form */
                    int count = b & 0x7F;
                    length = 0;
                    for (int i = 0; i < count; i++)
                    {
                        b = _stream.ReadByte(); bytesRead++;
                        length <<= 8;
                        length |= b;
                    }
                }
                state.setLengthRead(length, bytesRead);
                return length;
            }
            catch (IOException e)
            {
                throw e;
            }
        }

        /// <summary>
        /// Read the value of the next object.
        /// </summary>
        /// <returns>TLV object bytes.</returns>
        public byte[] readValue()
        {
            try
            {
                int length = state.getLength();
                byte[] value = new byte[length];
                _stream.Read(value, 0, value.Length);
                state.updateValueBytesRead(length);
                return value;
            }
            catch (IOException e)
            {
                throw e;
            }
        }

        private long skipValue()
        {
            if (state.isAtStartOfTag()) { return 0; }
            if (state.isAtStartOfLength()) { return 0; }
            int bytesLeft = state.getValueBytesLeft();
            return skip(bytesLeft);
        }


        
        /// <summary>
        /// Skips in this stream until a given tag is found (depth first).
        /// The stream is positioned right after the first occurrence of the tag.
        /// </summary>
        /// <param name="searchTag">The tag to search for.</param>
        public void skipToTag(int searchTag)
        {
            while (true)
            {
                /* Get the next tag. */
                int tag = -1;
                if (state.isAtStartOfTag())
                {
                    /* Nothing. */
                }
                else if (state.isAtStartOfLength())
                {
                    readLength();
                    if (isPrimitive(state.getTag())) { skipValue(); }
                }
                else
                {
                    if (isPrimitive(state.getTag())) { skipValue(); }

                }
                tag = readTag();
                if (tag == searchTag) { return; }

                if (isPrimitive(tag))
                {
                    int length = readLength();
                    int skippedBytes = (int)skipValue();
                    if (skippedBytes >= length)
                    {
                        /* Now at next tag. */
                        continue;
                    }
                    else
                    {
                        /* Could only skip less than length bytes,
                         * we're lost, probably at EOF. */
                        break;
                    }
                }
            }
        }

        /// <summary>
        /// Returns the amount of bytes that are still available.
        /// </summary>
        /// <returns>Amount of bytes available.</returns>
        public int available()
        {
            return (int)(_stream.Length - _stream.Position);
        }

        /// <summary>
        /// Read a single byte.
        /// </summary>
        /// <returns>Byte read.</returns>
        public int read()
        {
            int result = _stream.ReadByte();
            if (result < 0) { return -1; }
            state.updateValueBytesRead(1);
            return result;
        }

        /// <summary>
        /// Advance the stream position with a given number of bytes.
        /// </summary>
        /// <param name="n">Number of bytes to skip.</param>
        /// <returns>The new position in the stream.</returns>
        public long skip(long n)
        {
            if (n <= 0) { return 0; }
            long result = _stream.Position;
            result = _stream.Seek(n, SeekOrigin.Current) - result;
            state.updateValueBytesRead((int)result);
            return result;
        }

        /// <summary>
        /// Close the BERTLVInputStream and it's underlying base stream.
        /// </summary>
        public void close()
        {
            _stream.Close();
        }

        private static bool isPrimitive(int tag)
        {
            int i = 3;
            for (; i >= 0; i--)
            {
                int mask = (0xFF << (8 * i));
                if ((tag & mask) != 0x00) { break; }
            }
            int msByte = (((tag & (0xFF << (8 * i))) >> (8 * i)) & 0xFF);
            bool result = ((msByte & 0x20) == 0x00);
            return result;
        }

        private class State
        {
            private Stack<TLStruct> state;
            private bool _isAtStartOfTag;

            public State()
            {
                state = new Stack<TLStruct>();
                _isAtStartOfTag = true;
            }

            public bool isAtStartOfTag()
            { /* FIXME: wrong */
                return _isAtStartOfTag;
                //			if (state.isEmpty()) { return true; }
                //			TLStruct currentObject = state.peek();
                //			return (currentObject.getLength() >= 0 && currentObject.getBytesRead() == 0);
            }

            public bool isAtStartOfLength()
            {
                if (state.Count == 0) { return false; }
                TLStruct currentObject = state.Peek();
                return currentObject.getLength() < 0;
            }

            public int getTag()
            {
                if (state.Count == 0)
                {
                    throw new Exception("Tag not yet read.");
                }
                TLStruct currentObject = state.Peek();
                return currentObject.getTag();
            }

            public int getLength()
            {
                if (state.Count == 0)
                {
                    throw new Exception("Length not yet read.");
                }
                TLStruct currentObject = state.Peek();
                int length = currentObject.getLength();
                if (length < 0)
                {
                    throw new Exception("Length not yet read.");
                }
                return length;
            }

            public int getValueBytesLeft()
            {
                if (state.Count == 0)
                {
                    throw new Exception("Not yet reading value.");
                }
                TLStruct currentObject = state.Peek();
                int currentLength = currentObject.getLength();
                if (currentLength < 0)
                {
                    throw new Exception("Not yet reading value.");
                }
                int currentBytesRead = currentObject.getBytesRead();
                return currentLength - currentBytesRead;
            }

            public void setTagRead(int tag, int bytesRead)
            {
                TLStruct obj = new TLStruct(tag, -1);
                if (state.Count != 0)
                {
                    TLStruct parent = state.Peek();
                    parent.updateValueBytesRead(bytesRead);
                }
                state.Push(obj);
                _isAtStartOfTag = false;
            }

            public void setLengthRead(int length, int bytesRead)
            {
                if (length < 0)
                {
                    throw new ArgumentException("Cannot set negative length (length = " + length + ").");
                }
                TLStruct obj = state.Pop();
                if (state.Count != 0)
                {
                    TLStruct parent = state.Peek();
                    parent.updateValueBytesRead(bytesRead);
                }
                obj.setLength(length);
                state.Push(obj);
                _isAtStartOfTag = false;
            }

            public void updateValueBytesRead(int n)
            {
                if (state.Count == 0) { return; }
                TLStruct currentObject = state.Peek();
                int bytesLeft = currentObject.getLength() - currentObject.getBytesRead();
                if (n > bytesLeft)
                {
                    throw new ArgumentException("Cannot read " + n + " bytes! Only " + bytesLeft + " bytes left in this TLV object " + currentObject);
                }
                currentObject.updateValueBytesRead(n);
                int currentLength = currentObject.getLength();
                if (currentObject.getBytesRead() == currentLength)
                {
                    state.Pop();
                    /* Recursively update parent. */
                    updateValueBytesRead(currentLength);
                    _isAtStartOfTag = true;
                }
                else
                {
                    _isAtStartOfTag = false;
                }
            }

            private class TLStruct : ICloneable
            {
                private int tag, length, bytesRead;

                public TLStruct(int tag, int length)
                {
                    this.tag = tag; this.length = length; this.bytesRead = 0;
                }

                public void setLength(int length)
                {
                    this.length = length;
                }

                public int getTag() { return tag; }

                public int getLength() { return length; }

                public int getBytesRead() { return bytesRead; }

                public void updateValueBytesRead(int n)
                {
                    this.bytesRead += n;
                }

                public Object Clone()
                {
                    TLStruct result = new TLStruct(tag, length);
                    result.bytesRead = bytesRead;
                    return result;
                }

                public String toString() { return "[TLStruct " + tag.ToString("X") + ", " + length + ", " + bytesRead + "]"; }
            }
        }
    }
}