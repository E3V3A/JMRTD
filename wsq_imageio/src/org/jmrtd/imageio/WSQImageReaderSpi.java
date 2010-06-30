package org.jmrtd.imageio;

import java.io.*;
import java.util.*;
import javax.imageio.*;
import javax.imageio.spi.*;
import javax.imageio.stream.*;

public class WSQImageReaderSpi extends ImageReaderSpi
{
   private static final String VENDOR_NAME = "JMRTD";
   private static final String VERSION = "0.0.1";
   private static final String READER_CLASS_NAME = "org.jmrtd.imageio.WSQImageReader";
   private static final String[] NAMES = { "WSQ FBI" };
   private static final String[] SUFFIXES = { "wsq" };
   private static final String[] MIME_TYPES = { "images/x-wsq" };

   public WSQImageReaderSpi() {
      super();
   }

   public String getDescription(Locale locale) {
      return "Description goes here";
   }

   public boolean canDecodeInput(Object input) throws IOException {
      if (!(input instanceof ImageInputStream)) {
         return false;
      }
      ImageInputStream stream = (ImageInputStream)input;
      byte[] header = new byte[2]; // FIXME 2?
      try {
         stream.mark();
         stream.readFully(header);
         stream.reset();
      } catch (IOException ioe) {
         return false;
      }
      // FIXME check header
      return true;
   }

   public ImageReader createReaderInstance(Object extension) {
      return new WSQImageReader(this);
   }
}

