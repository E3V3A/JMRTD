/*
 * JMRTD - A Java API for accessing machine readable travel documents.
 *
 * Copyright (C) 2006 - 2008  The JMRTD team
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
 * $Id: MRZKeyListener.java 894 2009-03-23 15:50:46Z martijno $
 */

package org.jmrtd.app;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.jmrtd.BACKeySpec;
import org.jmrtd.lds.MRZInfo;


/**
 * A key listener that will recognize relevant parts of MRZ
 * data being entered.
 * 
 * @author Martijn Oostdijk (martijn.oostdijk@gmail.com)
 * 
 * @version $Revision: 894 $
 */
public class MRZKeyListener implements KeyListener, BACEntrySource
{
	private static final SimpleDateFormat SDF = new SimpleDateFormat("yyMMdd");

	private static final int TIMEOUT = 3000;
	private BACStorePanel store;
	private char[] buffer;
	private int indexInBuffer;
	private long heartBeat;

	public MRZKeyListener(BACStorePanel store) {
		buffer = new char[256];
		resetBuffer();
		this.store = store;
	}

	public void keyPressed(KeyEvent e) {
		/* NOTE: ignore */
	}

	public void keyReleased(KeyEvent e) {
		/* NOTE: ignore */
	}

	public void keyTyped(KeyEvent e) {
		char c = e.getKeyChar();
		appendToBuffer(c);
		heartBeat = System.currentTimeMillis();
		System.out.print(Character.toString(c));
		tryAndAddBACStoreEntry();
	}

	private void resetBuffer() {
		indexInBuffer = 0;
		heartBeat = System.currentTimeMillis();
	}

	private void appendToBuffer(char c) {
		c = Character.toUpperCase(c);
		if (indexInBuffer >= buffer.length) {
			resetBuffer();
		} else if (System.currentTimeMillis() - heartBeat > TIMEOUT) {
			resetBuffer();
		}
		buffer[indexInBuffer++] = c;
	}

	private void tryAndAddBACStoreEntry() {
		tryAndAddBACStoreEntry(buffer, 0, indexInBuffer);
	}

	private void tryAndAddBACStoreEntry(char[] buffer, int offset, int length) {
		if (length < 28) { return; }

		/* ID 1
		 * I<NNNPPPPPPPPPC<<<<<<<<<<<<<<<
		 * BBBBBBCGEEEEEECNNN<<<<<<<<<<<C
		 * NAME<<NAME<NAME<NAME<<<<<<<<<<
		 */
		if (length >= 40) {
			char doeCheckDigit = buffer[offset + length - 1];
			char[] doeChars = new char[6];
			System.arraycopy(buffer, offset + length - 7, doeChars, 0, 6);
			String doeString = new String(doeChars);
			char gender = buffer[offset + length - 8];
			char dobCheckDigit = buffer[offset + length - 9];
			char[] dobChars = new char[6];
			System.arraycopy(buffer, offset + length - 15, dobChars, 0, 6);
			String dobString = new String(dobChars);
			char docNumberCheckDigit = buffer[offset + length - 31];
			char[] docNumberChars = new char[9];
			System.arraycopy(buffer, offset + length - 40, docNumberChars, 0, 9);
			String docNumber = new String(docNumberChars);
			try {
				Date dob = SDF.parse(dobString);
				Date doe = SDF.parse(doeString);
				if (dobCheckDigit == MRZInfo.checkDigit(dobString) &&
						doeCheckDigit == MRZInfo.checkDigit(doeString) &&
						docNumberCheckDigit == MRZInfo.checkDigit(docNumber) &&
						(gender == 'M' || gender == 'F' || gender == '<')) {
					store.addEntry(new BACKeySpec(docNumber, dob, doe));
					resetBuffer();
					return;
				}
			} catch (ParseException pe) {
				/* NOTE: Do nothing, apparently not ID 1, maybe it's ID 3. */
			}
		}

		/* ID 3
		 * P<NNNNAME<<NAME<NAME<<<<<<<<<<<<<<<<<<<<<<<<
		 * PPPPPPPPPCNNNBBBBBBCGEEEEEEC<<<<<<<<<<<<<<CC
		 */
		if (length >= 28) {
			char doeCheckDigit = buffer[offset + length - 1];
			char[] doeChars = new char[6];
			System.arraycopy(buffer, offset + length - 7, doeChars, 0, 6);
			String doeString = new String(doeChars);
			char gender = buffer[offset + length - 8];
			char dobCheckDigit = buffer[offset + length - 9];
			char[] dobChars = new char[6];
			System.arraycopy(buffer, offset + length - 15, dobChars, 0, 6);
			String dobString = new String(dobChars);
			char docNumberCheckDigit = buffer[offset + length - 19];
			char[] docNumberChars = new char[9];
			System.arraycopy(buffer, offset + length - 28, docNumberChars, 0, 9);
			String docNumber = new String(docNumberChars);
			try {
				Date dob = SDF.parse(dobString);
				Date doe = SDF.parse(doeString);
				if (dobCheckDigit == MRZInfo.checkDigit(dobString) &&
						doeCheckDigit == MRZInfo.checkDigit(doeString) &&
						docNumberCheckDigit == MRZInfo.checkDigit(docNumber) &&
						(gender == 'M' || gender == 'F' || gender == '<')) {	
					store.addEntry(new BACKeySpec(docNumber, dob, doe));
					resetBuffer();
				}
			} catch (ParseException pe) {
				/* NOTE: do nothing, no BACEntry found. */
			}
		}
	}
}
