/*
 *  JMRTD Tests.
 *
 *  Copyright (C) 2010  The JMRTD team
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *  
 *  $Id: $
 */

package org.jmrtd.test.api;

import java.security.Provider;
import java.security.Security;

import junit.framework.TestCase;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.jmrtd.MRTDTrustStore;
import org.jmrtd.Passport;
import org.jmrtd.lds.MRZInfo;

public class PassportTest extends TestCase
{
	Provider BC_PROV = new BouncyCastleProvider();

	public PassportTest(String name) {
		super(name);
		Security.addProvider(BC_PROV);
	}

	public void testInitNoException() {
		try {
			Passport passport = new Passport(MRZInfo.DOC_TYPE_ID3, new MRTDTrustStore());
			assertNotNull(passport);
		} catch (Exception e) {
			fail(e.getMessage());
		}
	}
	
	public void testNewMRTDVerify() {
		try {
			Passport passport = new Passport(MRZInfo.DOC_TYPE_ID3, new MRTDTrustStore());
			passport.verifySecurity();
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}
}
