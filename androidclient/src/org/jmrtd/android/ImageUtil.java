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

import icc.ICCProfileException;
import icc.ICCProfiler;

import java.io.ByteArrayInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Vector;

import jj2000.j2k.codestream.HeaderInfo;
import jj2000.j2k.codestream.reader.BitstreamReaderAgent;
import jj2000.j2k.codestream.reader.HeaderDecoder;
import jj2000.j2k.decoder.DecoderSpecs;
import jj2000.j2k.entropy.decoder.EntropyDecoder;
import jj2000.j2k.fileformat.reader.FileFormatReader;
import jj2000.j2k.image.BlkImgDataSrc;
import jj2000.j2k.image.Coord;
import jj2000.j2k.image.DataBlkInt;
import jj2000.j2k.image.ImgDataConverter;
import jj2000.j2k.image.invcomptransf.InvCompTransf;
import jj2000.j2k.io.RandomAccessIO;
import jj2000.j2k.quantization.dequantizer.Dequantizer;
import jj2000.j2k.roi.ROIDeScaler;
import jj2000.j2k.util.FacilityManager;
import jj2000.j2k.util.ISRandomAccessIO;
import jj2000.j2k.util.MsgLogger;
import jj2000.j2k.util.ParameterList;
import jj2000.j2k.wavelet.synthesis.InverseWT;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import colorspace.ColorSpace;
import colorspace.ColorSpace.CSEnum;
import colorspace.ColorSpaceException;

public class ImageUtil {

	public static String
	JPEG_MIME_TYPE = "image/jpeg",
	JPEG2000_MIME_TYPE = "image/jp2",
	JPEG2000_ALT_MIME_TYPE = "image/jpeg2000",
	WSQ_MIME_TYPE = "image/x-wsq";
	
	private ImageUtil() {
	}

	/* Needed to get default settings... */
	private final static String[][] pinfo = {
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

	public static Bitmap getImage(byte[] in, String mimeType) throws IOException {
		if (JPEG2000_MIME_TYPE.equalsIgnoreCase(mimeType) || JPEG2000_ALT_MIME_TYPE.equalsIgnoreCase(mimeType)) {
			return decode(new ISRandomAccessIO(new ByteArrayInputStream(in)));
		} else {
			return BitmapFactory.decodeByteArray(in, 0, in.length);
		}
	}

	public static Bitmap getImage(InputStream in, String mimeType) throws IOException {
		if (JPEG2000_MIME_TYPE.equalsIgnoreCase(mimeType) || JPEG2000_ALT_MIME_TYPE.equalsIgnoreCase(mimeType)) {
			return decode(new ISRandomAccessIO(in));
		} else {
			return BitmapFactory.decodeStream(in);
		}
	}

	private static Bitmap decode(RandomAccessIO in) throws IOException {
		String[][] pinfo = getAllParameters();
		ParameterList defpl = new ParameterList();
		for (int i = pinfo.length - 1; i >= 0; i--) {
			if (pinfo[i][3] != null)
				defpl.put(pinfo[i][0], pinfo[i][3]);
		}
		ParameterList pl = new ParameterList(defpl);

		pl.setProperty("rate", "3");
		// pl.setProperty("debug", "on");

		HeaderInfo hi;
		int res; // resolution level to reconstruct
		FileFormatReader ff;
		BitstreamReaderAgent breader;
		HeaderDecoder hd;
		EntropyDecoder entdec;
		ROIDeScaler roids;
		Dequantizer deq;
		InverseWT invWT;
		InvCompTransf ictransf;
		ImgDataConverter converter;
		DecoderSpecs decSpec = null;
		BlkImgDataSrc palettized;
		BlkImgDataSrc channels;
		BlkImgDataSrc resampled;
		BlkImgDataSrc color;
		int i;
		int depth[];

		ColorSpace csMap = null;

		// **** File Format ****
		// If the codestream is wrapped in the jp2 fileformat, Read the
		// file format wrapper
		ff = new FileFormatReader(in);
		ff.readFileFormat();
		if (ff.JP2FFUsed) {
			in.seek(ff.getFirstCodeStreamPos());
		}

		// +----------------------------+
		// | Instantiate decoding chain |
		// +----------------------------+

		// **** Header decoder ****
		// Instantiate header decoder and read main header
		hi = new HeaderInfo();
		try {
			hd = new HeaderDecoder(in, pl, hi);
		} catch (EOFException e) {
			throw new IOException("Codestream too short or bad header, unable to decode.");
		}

		int nCompCod = hd.getNumComps();
		/* int nTiles = */ hi.siz.getNumTiles();
		decSpec = hd.getDecoderSpecs();

		// Get demixed bitdepths
		depth = new int[nCompCod];
		for (i = 0; i < nCompCod; i++) {
			depth[i] = hd.getOriginalBitDepth(i);
		}

		// **** Bit stream reader ****
		breader = BitstreamReaderAgent.createInstance(in, hd, pl, decSpec,
				pl.getBooleanParameter("cdstr_info"), hi);

		// **** Entropy decoder ****
		entdec = hd.createEntropyDecoder(breader, pl);

		// **** ROI de-scaler ****
		roids = hd.createROIDeScaler(entdec, pl, decSpec);

		// **** Dequantizer ****
		deq = hd.createDequantizer(roids, depth, decSpec);

		// **** Inverse wavelet transform ***
		// full page inverse wavelet transform
		invWT = InverseWT.createInstance(deq, decSpec);

		res = breader.getImgRes();
		invWT.setImgResLevel(res);

		// **** Data converter **** (after inverse transform module)
		converter = new ImgDataConverter(invWT, 0);

		// **** Inverse component transformation ****
		ictransf = new InvCompTransf(converter, decSpec, depth, pl);

		// **** Color space mapping ****
		if (ff.JP2FFUsed && pl.getParameter("nocolorspace").equals("off")) {
			try {
				csMap = null;
				csMap = new ColorSpace(in, hd, pl);
				channels = hd.createChannelDefinitionMapper(ictransf, csMap);
				resampled = hd.createResampler(channels, csMap);
				palettized = hd.createPalettizedColorSpaceMapper(resampled, csMap);
				color = hd.createColorSpaceMapper(palettized, csMap);

				if (csMap.debugging()) {
					FacilityManager.getMsgLogger().printmsg(MsgLogger.ERROR, "" + csMap);
					FacilityManager.getMsgLogger().printmsg(MsgLogger.ERROR, "" + channels);
					FacilityManager.getMsgLogger().printmsg(MsgLogger.ERROR, "" + resampled);
					FacilityManager.getMsgLogger().printmsg(MsgLogger.ERROR, "" + palettized);
					FacilityManager.getMsgLogger().printmsg(MsgLogger.ERROR, "" + color);
				}
			} catch (ColorSpaceException e) {
				throw new IOException("error processing jp2 colorspace information: " + e.getMessage());
			} catch (ICCProfileException ie) {
				throw new IOException("error processing jp2 colorspace information: " + ie.getMessage());
			}
		} else { // Skip colorspace mapping
			color = ictransf;
		}

		// This is the last image in the decoding chain and should be
		// assigned by the last transformation:
		BlkImgDataSrc decodedImage = color;
		if (color == null) {
			decodedImage = ictransf;
		}
		int nCompImg = decodedImage.getNumComps();

		// **** Create image writers/image display ****

		DataBlkInt[] blk = new DataBlkInt[nCompImg];
		int[] depths = new int[nCompImg];

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

				System.out.println("ulx =  " + ulx);
				System.out.println("uly = " + uly);
				System.out.println("width = " + width);
				System.out.println("height = " + height);
				System.out.println("nCompImg = " + nCompImg);

				for (int c = 0; c < nCompImg; c++) {
					blk[c] = new DataBlkInt(ulx, uly, width, height);
					blk[c].data = null;
					blk[c] = (DataBlkInt)decodedImage.getInternCompData(blk[c], c);
					depths[c] = decodedImage.getNomRangeBits(c);
				}
			}
		}

		if (csMap == null) {
			throw new IOException("csMap is null");
		}
		CSEnum cs = csMap.getColorSpace();
		if (cs.equals(ColorSpace.sRGB)) {
			int[] colors = decodeSignedRGB(blk, depths);
			return Bitmap.createBitmap(colors, 0, imgWidth, imgWidth, imgHeight, Bitmap.Config.ARGB_8888);
		} else if (cs.equals(ColorSpace.GreyScale)) {
			int[] colors = decodeGrayScale(blk, depths);
			return Bitmap.createBitmap(colors, 0, imgWidth, imgWidth, imgHeight, Bitmap.Config.ARGB_8888);
		}
		throw new IOException("Unsupported color space type.");
	}

	private static int[] decodeSignedRGB(DataBlkInt[] blk, int[] depths) {
		if (blk == null || blk.length != 3) {
			throw new IllegalArgumentException("number of bands should be 3");
		}
		if (depths == null || depths.length != 3) {
			throw new IllegalArgumentException("number of bands should be 3");
		}

		int[] rData = blk[0].getDataInt();
		int[] gData = blk[1].getDataInt();
		int[] bData = blk[2].getDataInt();

		if (rData.length != gData.length || gData.length != bData.length || bData.length != rData.length) {
			throw new IllegalArgumentException("different dimensions for bands");
		}

		if (depths[0] != depths[1] || depths[1] != depths[2] || depths[2] != depths[0]) {
			throw new IllegalArgumentException("different depths for bands");
		}

		int depth = depths[0];

		int[] pixels = new int[rData.length];
//		int minR = Integer.MAX_VALUE, maxR = Integer.MIN_VALUE;
//		int minG = Integer.MAX_VALUE, maxG = Integer.MIN_VALUE;
//		int minB = Integer.MAX_VALUE, maxB = Integer.MIN_VALUE;

		for (int j = 0; j < rData.length; j++) {
			
			/* Signed values, should be in [-128 .. 127] (for depth = 8). */
			int r = rData[j];
			int g = gData[j];
			int b = bData[j];
		
			/* Determine min and max per band. For debugging. Turns out values outside [-128 .. 127] are possible in samples!?! FIXME: check with spec. */
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

	public static void error(Object obj) {
		System.out.println(obj);
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static String[][] getAllParameters() {
		Vector vec = new Vector();
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
			
		str = pinfo;
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
}
