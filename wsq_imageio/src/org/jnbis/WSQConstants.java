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

public interface WSQConstants {

	/*used to "mask out" n number of bits from data stream*/
	static int[] BITMASK = { 0x00, 0x01, 0x03, 0x07, 0x0f, 0x1f, 0x3f, 0x7f, 0xff };

	static final int MAX_DHT_TABLES = 8;
	static final int MAX_HUFFBITS = 16;
	static final int MAX_HUFFCOUNTS_WSQ = 256;

	static final int MAX_HUFFCOEFF =       74;  /* -73 .. +74 */
	static final int MAX_HUFFZRUN  =      100;

	static final int MAX_HIFILT = 7;
	static final int MAX_LOFILT = 9;

	static final int W_TREELEN = 20;
	static final int Q_TREELEN = 64;

	/* WSQ Marker Definitions */
	static final int SOI_WSQ = 0xffa0;
	static final int EOI_WSQ = 0xffa1;
	static final int SOF_WSQ = 0xffa2;
	static final int SOB_WSQ = 0xffa3;
	static final int DTT_WSQ = 0xffa4;
	static final int DQT_WSQ = 0xffa5;
	static final int DHT_WSQ = 0xffa6;
	static final int DRT_WSQ = 0xffa7;
	static final int COM_WSQ = 0xffa8;

	static final int STRT_SUBBAND_2 = 19;
	static final int STRT_SUBBAND_3 = 52;
	static final int MAX_SUBBANDS = 64;
	static final int NUM_SUBBANDS = 60;
	static final int STRT_SUBBAND_DEL = NUM_SUBBANDS;
	static final int STRT_SIZE_REGION_2 = 4;
	static final int STRT_SIZE_REGION_3 = 51;

	static final int COEFF_CODE = 0;
	static final int RUN_CODE = 1;

	static final float VARIANCE_THRESH = 1.01f;

	/* Case for getting ANY marker. */
	static final int ANY_WSQ = 0xffff;
	static final int TBLS_N_SOF = 2;
	static final int TBLS_N_SOB = TBLS_N_SOF + 2;
}
