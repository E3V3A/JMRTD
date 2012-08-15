/*
 * JMRTD - A Java API for accessing machine readable travel documents.
 *
 * Copyright (C) 2006 - 2012  The JMRTD team
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
 * $Id: $
 */

package org.jmrtd.android;

import icc.ICCProfiler;

import java.io.ByteArrayInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Vector;

import jj2000.j2k.codestream.HeaderInfo;
import jj2000.j2k.codestream.reader.BitstreamReaderAgent;
import jj2000.j2k.codestream.reader.HeaderDecoder;
import jj2000.j2k.codestream.writer.HeaderEncoder;
import jj2000.j2k.codestream.writer.PktEncoder;
import jj2000.j2k.decoder.DecoderSpecs;
import jj2000.j2k.entropy.decoder.EntropyDecoder;
import jj2000.j2k.entropy.encoder.EntropyCoder;
import jj2000.j2k.entropy.encoder.PostCompRateAllocator;
import jj2000.j2k.fileformat.reader.FileFormatReader;
import jj2000.j2k.image.BlkImgDataSrc;
import jj2000.j2k.image.Coord;
import jj2000.j2k.image.DataBlkInt;
import jj2000.j2k.image.ImgDataConverter;
import jj2000.j2k.image.forwcomptransf.ForwCompTransf;
import jj2000.j2k.image.invcomptransf.InvCompTransf;
import jj2000.j2k.io.RandomAccessIO;
import jj2000.j2k.quantization.dequantizer.Dequantizer;
import jj2000.j2k.quantization.quantizer.Quantizer;
import jj2000.j2k.roi.ROIDeScaler;
import jj2000.j2k.roi.encoder.ROIScaler;
import jj2000.j2k.util.ISRandomAccessIO;
import jj2000.j2k.util.ParameterList;
import jj2000.j2k.wavelet.analysis.AnWTFilter;
import jj2000.j2k.wavelet.analysis.ForwardWT;
import jj2000.j2k.wavelet.synthesis.InverseWT;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import colorspace.ColorSpace;
import colorspace.ColorSpace.CSEnum;

public class ImageUtil {

	public static String
	JPEG_MIME_TYPE = "image/jpeg",
	JPEG2000_MIME_TYPE = "image/jp2",
	JPEG2000_ALT_MIME_TYPE = "image/jpeg2000",
	WSQ_MIME_TYPE = "image/x-wsq";
	
	private ImageUtil() {
	}

	private final static String[][] DECODER_PINFO = {
		{ "u", "[on|off]", "", "off" },
		{ "v", "[on|off]", "", "off" },
		{ "verbose", "[on|off]", "", "off" },
		{ "pfile", "", "", null },
		{ "res", "", "", null },
		{ "i", "", "", null },
		{ "o", "", "", null },
		{ "rate", "", "", "3" },
		{ "nbytes", "", "", "-1" },
		{ "parsing", null, "", "on" },
		{ "ncb_quit", "", "", "-1" },
		{ "l_quit", "", "", "-1" },
		{ "m_quit", "", "", "-1" },
		{ "poc_quit", null, "", "off" },
		{ "one_tp", null, "", "off" },
		{ "comp_transf", null, "", "on" },
		{ "debug", null, "", "off" },
		{ "cdstr_info", null, "", "off" },
		{ "nocolorspace", null, "", "off" },
		{ "colorspace_debug", null, "", "off" } };

	private final static String[][] ENCODER_PINFO = {
		{ "debug", null, "", "off" },
		{ "disable_jp2_extension", "[on|off]", "", "off" },
		{ "file_format", "[on|off]", "", "off" },
		{ "pph_tile", "[on|off]", "", "off" },
		{ "pph_main", "[on|off]", "", "off" },
		{ "pfile", "<filename of arguments file>", "", null },
		{ "tile_parts", "", "0" },
		{ "tiles", "<nominal tile width> <nominal tile height>", "", "0 0" },
		{ "ref", "<x> <y>", "", "0 0" },
		{ "tref", "<x> <y>",  "", "0 0" },
		{ "rate", "<output bitrate in bpp>", "", "3" },
		{ "lossless", "[on|off]", "", "off" },
		{ "i", "<image file> [,<image file> [,<image file> ... ]]",  "", null },
		{ "o", "<file name>",  "", null },
		{ "verbose", null,  "", "off" },
		{ "v", "[on|off]", "", "off" },
		{ "u", "[on|off]", "", "off" } };

	public static Bitmap read(byte[] in, String mimeType) throws IOException {
		if (JPEG2000_MIME_TYPE.equalsIgnoreCase(mimeType) || JPEG2000_ALT_MIME_TYPE.equalsIgnoreCase(mimeType)) {
			return decode(new ISRandomAccessIO(new ByteArrayInputStream(in)));
		} else {
			return BitmapFactory.decodeByteArray(in, 0, in.length);
		}
	}

	public static Bitmap read(InputStream in, String mimeType) throws IOException {
		if (JPEG2000_MIME_TYPE.equalsIgnoreCase(mimeType) || JPEG2000_ALT_MIME_TYPE.equalsIgnoreCase(mimeType)) {
			return decode(new ISRandomAccessIO(in));
		} else {
			return BitmapFactory.decodeStream(in);
		}
	}

	/* ONLY PRIVATE METHODS BELOW. */
	
	private static Bitmap decode(RandomAccessIO in) throws IOException {
		String[][] pinfo = getAllDecoderParameters();
		ParameterList defpl = new ParameterList();
		for (int i = pinfo.length - 1; i >= 0; i--) {
			if (pinfo[i][3] != null)
				defpl.put(pinfo[i][0], pinfo[i][3]);
		}
		ParameterList pl = new ParameterList(defpl);

		// The codestream should be wrapped in the jp2 fileformat, Read the
		// file format wrapper
		FileFormatReader fileFormatReader = new FileFormatReader(in);
		fileFormatReader.readFileFormat();
		if (!fileFormatReader.JP2FFUsed) {
			throw new IOException("Was expecting JP2 file format");
		}
		in.seek(fileFormatReader.getFirstCodeStreamPos());

		// Instantiate header decoder and read main header
		HeaderInfo headerInfo = new HeaderInfo();
		HeaderDecoder headerDecoder = null;
		try {
			headerDecoder = new HeaderDecoder(in, pl, headerInfo);
		} catch (EOFException e) {
			throw new IOException("Codestream too short or bad header, unable to decode");
		}

		int originalComponentCount = headerDecoder.getNumComps();
		/* int nTiles = */ headerInfo.siz.getNumTiles();
		DecoderSpecs decoderSpecs = headerDecoder.getDecoderSpecs();

		// Get demixed bitdepths
		int[] originalBitDepths = new int[originalComponentCount];
		for (int i = 0; i < originalComponentCount; i++) {
			originalBitDepths[i] = headerDecoder.getOriginalBitDepth(i);
		}

		BitstreamReaderAgent bitStreamReader = BitstreamReaderAgent.createInstance(in, headerDecoder, pl, decoderSpecs, pl.getBooleanParameter("cdstr_info"), headerInfo);
		EntropyDecoder entropyDecoder = headerDecoder.createEntropyDecoder(bitStreamReader, pl);

		ROIDeScaler roiDeScaler = headerDecoder.createROIDeScaler(entropyDecoder, pl, decoderSpecs);

		Dequantizer dequantizer = headerDecoder.createDequantizer(roiDeScaler, originalBitDepths, decoderSpecs);

		// full page inverse wavelet transform
		InverseWT inverseWT = InverseWT.createInstance(dequantizer, decoderSpecs);

		// resolution level to reconstruct
		int imgRes = bitStreamReader.getImgRes();
		inverseWT.setImgResLevel(imgRes);

		ImgDataConverter imgDataConverter = new ImgDataConverter(inverseWT, 0);

		InvCompTransf invCompTransf = new InvCompTransf(imgDataConverter, decoderSpecs, originalBitDepths, pl);

		// **** Color space mapping ****
		ColorSpace colorSpace = null;
		BlkImgDataSrc color = null;
		try {
			colorSpace = new ColorSpace(in, headerDecoder, pl);
			BlkImgDataSrc channels = headerDecoder.createChannelDefinitionMapper(invCompTransf, colorSpace);
			BlkImgDataSrc resampled = headerDecoder.createResampler(channels, colorSpace);
			BlkImgDataSrc palettized = headerDecoder.createPalettizedColorSpaceMapper(resampled, colorSpace);
			color = headerDecoder.createColorSpaceMapper(palettized, colorSpace);
		} catch (Exception e) {
			throw new IOException("Error processing jp2 colorspace information: " + e.getMessage());
		}

		// This is the last image in the decoding chain and should be
		// assigned by the last transformation:
		BlkImgDataSrc decodedImage = color;
		if (color == null) {
			decodedImage = invCompTransf;
		}
		int imgComponentCount = decodedImage.getNumComps();

		// **** Create image writers/image display ****

		DataBlkInt[] blk = new DataBlkInt[imgComponentCount];
		int[] imgBitDepths = new int[imgComponentCount];


		int imgWidth = decodedImage.getImgWidth();
		int imgHeight = decodedImage.getImgHeight();

		// Find the list of tile to decode.
		Coord nT = decodedImage.getNumTiles(null);

		// Loop on vertical tiles
		for (int y = 0; y < nT.y; y++) {
			// Loop on horizontal tiles
			for (int x = 0; x < nT.x; x++) {
				decodedImage.setTile(x, y);

				int width = decodedImage.getImgWidth();
				int height = decodedImage.getImgHeight();
				int ulx = decodedImage.getImgULX();
				int uly = decodedImage.getImgULY();

				for (int i = 0; i < imgComponentCount; i++) {
					blk[i] = new DataBlkInt(ulx, uly, width, height);
					blk[i].data = null;
					blk[i] = (DataBlkInt)decodedImage.getInternCompData(blk[i], i);
					imgBitDepths[i] = decodedImage.getNomRangeBits(i);
				}
			}
		}

		CSEnum colorSpaceType = colorSpace.getColorSpace();
		if (colorSpaceType.equals(ColorSpace.sRGB)) {
			int[] colors = decodeSignedRGB(blk, imgBitDepths);
			return Bitmap.createBitmap(colors, 0, imgWidth, imgWidth, imgHeight, Bitmap.Config.ARGB_8888);
		} else if (colorSpaceType.equals(ColorSpace.GreyScale)) {
			int[] colors = decodeGrayScale(blk, imgBitDepths);
			return Bitmap.createBitmap(colors, 0, imgWidth, imgWidth, imgHeight, Bitmap.Config.ARGB_8888);
		}
		throw new IOException("Unsupported color space type.");
	}

	/**
	 * Decodes n-bit signed to 8-bit unsigned.
	 * 
	 * @param blk
	 * @param depths
	 * @return
	 */
	private static int[] decodeSignedRGB(DataBlkInt[] blk, int[] depths) {
		if (blk == null || blk.length != 3) {
			throw new IllegalArgumentException("Was expecting 3 bands");
		}
		if (depths == null || depths.length != 3) {
			throw new IllegalArgumentException("Was expecting 3 bands");
		}
		if (depths[0] != depths[1] || depths[1] != depths[2] || depths[2] != depths[0]) {
			throw new IllegalArgumentException("Different depths for bands");
		}

		int depth = depths[0];

		int[] rData = blk[0].getDataInt();
		int[] gData = blk[1].getDataInt();
		int[] bData = blk[2].getDataInt();

		if (rData.length != gData.length || gData.length != bData.length || bData.length != rData.length) {
			throw new IllegalArgumentException("Different dimensions for bands");
		}

		int[] pixels = new int[rData.length];
		//		int minR = Integer.MAX_VALUE, maxR = Integer.MIN_VALUE;
		//		int minG = Integer.MAX_VALUE, maxG = Integer.MIN_VALUE;
		//		int minB = Integer.MAX_VALUE, maxB = Integer.MIN_VALUE;

		for (int j = 0; j < rData.length; j++) {

			/* Signed values, should be in [-128 .. 127] (for depth = 8). */
			int r = rData[j];
			int g = gData[j];
			int b = bData[j];

			/* Determine min and max per band. For debugging. Turns out values outside [-128 .. 127] are possible in samples!?! */
			//			if (r < minR) { minR = r; } if (r > maxR) { maxR = r; }
			//			if (g < minG) { minG = g; } if (g > maxG) { maxG = g; }
			//			if (b < minB) { minB = b; } if (b > maxB) { maxB = b; }			

			/* Transform by adding 127 (for depth = 8) to get values in [0 .. 255]. Inputs from outside [-128 .. 127] are rounded up or down to 0 resp. 255. */
			if (r < -(1 << (depth - 1))) { r = 0x00; } else if (r > ((1 << (depth - 1)) - 1)) { r = (1 << depth) - 1; } else { r += (1 << (depth -1)); }
			if (g < -(1 << (depth - 1))) { g = 0x00; } else if (g > ((1 << (depth - 1)) - 1)) { g = (1 << depth) - 1; } else { g += (1 << (depth -1)); }
			if (b < -(1 << (depth - 1))) { b = 0x00; } else if (b > ((1 << (depth - 1)) - 1)) { b = (1 << depth) - 1; } else { b += (1 << (depth -1)); }

			pixels[j] = 0xFF000000 | ((r & 0xFF) << (2 * depth)) | ((g & 0xFF) << depth) | (b & 0xFF);
		}
		return pixels;
	}

	/**
	 * Decodes 8-bit gray scale to 8-bit unsigned RGB.
	 * 
	 * @param blk
	 * @param depths
	 * @return
	 */
	private static int[] decodeGrayScale(DataBlkInt[] blk, int[] depths) {
		if (blk.length != 1) {
			throw new IllegalArgumentException("Was expecting 1 band");
		}
		if (depths.length != 1) {
			throw new IllegalArgumentException("Was expecting 1 band");
		}
		int[] data = blk[0].getDataInt();
		int[] pixels = new int[data.length];
		for (int j = 0; j < data.length; j++) {
			pixels[j] = 0xFF000000 | ((data[j] & 0xFF) << 16) | ((data[j] & 0xFF) << 8) | (data[j] & 0xFF);
		}
		return pixels;
	}

	private static String[][] getAllDecoderParameters() {
		Vector<String[]> vec = new Vector<String[]>();
		int i;

		String[][] str = BitstreamReaderAgent.getParameterInfo();
		if (str != null) {
			for (i = str.length - 1; i >= 0; i--) {
				vec.addElement(str[i]);
			}
		}

		str = EntropyDecoder.getParameterInfo();
		if (str != null) {
			for (i = str.length - 1; i >= 0; i--) {
				vec.addElement(str[i]);
			}
		}

		str = ROIDeScaler.getParameterInfo();
		if (str != null) {
			for (i = str.length - 1; i >= 0; i--) {
				vec.addElement(str[i]);
			}
		}

		str = Dequantizer.getParameterInfo();
		if (str != null) {
			for (i = str.length - 1; i >= 0; i--) {
				vec.addElement(str[i]);
			}
		}

		str = InvCompTransf.getParameterInfo();
		if (str != null) {
			for (i = str.length - 1; i >= 0; i--) {
				vec.addElement(str[i]);
			}
		}

		str = HeaderDecoder.getParameterInfo();
		if (str != null) {
			for (i = str.length - 1; i >= 0; i--) {
				vec.addElement(str[i]);
			}
		}

		str = ICCProfiler.getParameterInfo();
		if (str != null) {
			for (i = str.length - 1; i >= 0; i--) {
				vec.addElement(str[i]);
			}
		}

		str = DECODER_PINFO;
		if (str != null) {
			for (i = str.length - 1; i >= 0; i--) {
				vec.addElement(str[i]);
			}
		}

		str = new String[vec.size()][4];
		if (str != null) {
			for (i = str.length - 1; i >= 0; i--) {
				str[i] = (String[]) vec.elementAt(i);
			}
		}

		return str;
	}

	private static String[][] getAllEncoderParameters() {
		Vector<String[]> vec = new Vector<String[]>();

		String[][] str = ENCODER_PINFO;
		if(str!=null)
			for(int i=str.length-1; i>=0; i--)
				vec.addElement(str[i]);

		str = ForwCompTransf.getParameterInfo();
		if(str!=null)
			for(int i=str.length-1; i>=0; i--)
				vec.addElement(str[i]);

		str = AnWTFilter.getParameterInfo();
		if(str!=null)
			for(int i=str.length-1; i>=0; i--)
				vec.addElement(str[i]);

		str = ForwardWT.getParameterInfo();
		if(str!=null)
			for(int i=str.length-1; i>=0; i--)
				vec.addElement(str[i]);

		str = Quantizer.getParameterInfo();
		if(str!=null)
			for(int i=str.length-1; i>=0; i--)
				vec.addElement(str[i]);

		str = ROIScaler.getParameterInfo();
		if(str!=null)
			for(int i=str.length-1; i>=0; i--)
				vec.addElement(str[i]);

		str = EntropyCoder.getParameterInfo();
		if(str!=null)
			for(int i=str.length-1; i>=0; i--)
				vec.addElement(str[i]);

		str = HeaderEncoder.getParameterInfo();
		if(str!=null)
			for(int i=str.length-1; i>=0; i--)
				vec.addElement(str[i]);

		str = PostCompRateAllocator.getParameterInfo();
		if(str!=null)
			for(int i=str.length-1; i>=0; i--)
				vec.addElement(str[i]);

		str = PktEncoder.getParameterInfo();
		if(str!=null)
			for(int i=str.length-1; i>=0; i--)
				vec.addElement(str[i]);

		str = new String[vec.size()][4];
		if(str!=null)
			for(int i=str.length-1; i>=0; i--)
				str[i] = (String[])vec.elementAt(i);

		return str;
	}
}
