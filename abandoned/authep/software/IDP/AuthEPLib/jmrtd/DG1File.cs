using System;
using System.Collections.Generic;
using System.IO;
using System.Text;

namespace nl.telin.authep.lib
{
    /// <summary>
    /// File structure for the EF_DG1 file. Datagroup 1 contains the Machine Readable Zone information.
    /// </summary>
    public class DG1File : IDGFile
    {
        private const int MRZ_INFO_TAG = 0x5F1F;

        private MRZInfo _mrz;

        /// <summary>
        /// Gets the MRZ information stored in this file.
        /// </summary>
        public MRZInfo MRZ { get { return _mrz; } }

        /// <summary>
        /// Constructs a new EF_DG1 file.
        /// </summary>
        /// <param name="data">bytes of the EF_DG1 file</param>
        public DG1File(byte[] data)
        {
            dgNumber = 1;
            raw = new byte[data.Length];
            Array.Copy(data,RawBytes,data.Length);
            MemoryStream dg1MemStream = new MemoryStream(data);
            BERTLVInputStream dg1Stream = new BERTLVInputStream(dg1MemStream);
            int tag = dg1Stream.readTag();
            if (tag != IDGFile.EF_DG1_TAG) throw new ArgumentException("Expected EF_DG1_TAG");
            int dg1Length = dg1Stream.readLength();
            dg1Stream.skipToTag(MRZ_INFO_TAG);
            dg1Stream.readLength();
            _mrz = new MRZInfo(dg1MemStream);
        }
    }
}
