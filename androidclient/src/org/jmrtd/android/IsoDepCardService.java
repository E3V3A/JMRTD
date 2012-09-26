/*
 * JMRTD Android client
 * 
 * Copyright (C) 2006 - 2012  The JMRTD team
 * 
 * Originally based on:
 * 
 * aJMRTD - An Android Client for JMRTD, a Java API for accessing machine readable travel documents.
 *
 * Max Guenther, max.math.guenther@googlemail.com
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
 */

package org.jmrtd.android;

import java.io.IOException;

import net.sourceforge.scuba.smartcards.CardService;
import net.sourceforge.scuba.smartcards.CardServiceException;
import net.sourceforge.scuba.smartcards.ICommandAPDU;
import net.sourceforge.scuba.smartcards.IResponseAPDU;
import net.sourceforge.scuba.smartcards.ResponseAPDU;
import android.nfc.tech.IsoDep;

public class IsoDepCardService extends CardService {

	private static final long serialVersionUID = -3632172111055888506L;

	private boolean isOpen = false;
	private IsoDep picc;

	public IsoDepCardService(IsoDep picc) {
		super();
		this.picc = picc;
	}

	@Override
	public void open() throws CardServiceException {
		try {
			picc.connect();
			isOpen = true;
		} catch (IOException e) {
			e.printStackTrace();
			throw new CardServiceException("IOException");
		}

	}

	@Override
	public boolean isOpen() {
		return isOpen;
	}

	@Override
	public IResponseAPDU transmit(ICommandAPDU apdu) throws CardServiceException {
		try {
			return new ResponseAPDU(picc.transceive(apdu.getBytes()));
		} catch (IOException e) {
			e.printStackTrace();
			throw new CardServiceException("could not transmit");
		}
	}

	@Override
	public void close() {
		try {
			picc.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		isOpen = false;
	}

}
