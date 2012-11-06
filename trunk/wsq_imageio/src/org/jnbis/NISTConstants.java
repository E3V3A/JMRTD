/*
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
 * --
 * This code is based on NBIS (NIST, public domain) and / or
 * JNBIS 1.0.3 which was licensed under Apache License 2.0.
 * 
 * $Id: $
 */

package org.jnbis;

public interface NISTConstants {
	/* From nistcom.h */
	static final String NCM_EXT      =   "ncm";
	static final String NCM_HEADER  =    "NIST_COM";        /* mandatory */
	static final String NCM_PIX_WIDTH =  "PIX_WIDTH";       /* mandatory */
	static final String NCM_PIX_HEIGHT = "PIX_HEIGHT";      /* mandatory */
	static final String NCM_PIX_DEPTH =  "PIX_DEPTH";       /* 1,8,24 (mandatory)*/
	static final String NCM_PPI     =    "PPI";             /* -1 if unknown (mandatory)*/
	static final String NCM_COLORSPACE = "COLORSPACE";      /* RGB,YCbCr,GRAY */
	static final String NCM_N_CMPNTS =   "NUM_COMPONENTS";  /* [1..4] (mandatory w/hv_factors)*/
	static final String NCM_HV_FCTRS =   "HV_FACTORS";      /* H0,V0:H1,V1:...*/
	static final String NCM_INTRLV =     "INTERLEAVE";      /* 0,1 (mandatory w/depth=24) */
	static final String NCM_COMPRESSION = "COMPRESSION";     /* NONE,JPEGB,JPEGL,WSQ */
	static final String NCM_JPEGB_QUAL = "JPEGB_QUALITY";   /* [20..95] */
	static final String NCM_JPEGL_PREDICT = "JPEGL_PREDICT"; /* [1..7] */
	static final String NCM_WSQ_RATE =   "WSQ_BITRATE";     /* ex. .75,2.25 (-1.0 if unknown)*/
	static final String NCM_LOSSY =      "LOSSY";           /* 0,1 */

	static final String NCM_HISTORY =    "HISTORY";         /* ex. SD historical data */
	static final String NCM_FING_CLASS = "FING_CLASS";      /* ex. A,L,R,S,T,W */
	static final String NCM_SEX =        "SEX";             /* m,f */
	static final String NCM_SCAN_TYPE =  "SCAN_TYPE";       /* l,i */
	static final String NCM_FACE_POS =   "FACE_POS";        /* f,p */
	static final String NCM_AGE =        "AGE";
	static final String NCM_SD_ID =      "SD_ID";           /* 4,9,10,14,18 */
}
