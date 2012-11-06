/*
 *  JMRTD Tests.
 *
 *  Copyright (C) 2009  The JMRTD team
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

package org.jmrtd.test.api.lds;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Arrays;

import junit.framework.TestCase;

import org.jmrtd.lds.CVCAFile;

public class CVCAFileTest extends TestCase {

    public void test1() {
        String name1 = "CAReference00001";
        CVCAFile f = new CVCAFile(name1);
        assertEquals(name1, f.getCAReference().getName());
        assertEquals(null, f.getAltCAReference());
    }

    public void test2() {
        String name1 = "CAReference00001";
        String name2 = "CAReference00002";
        CVCAFile f = new CVCAFile(name1, name2);
        assertEquals(name1, f.getCAReference().getName());
        assertEquals(name2, f.getAltCAReference().getName());
    }

    public void testReflexive1() {
        String name1 = "CAReference00001";
        String name2 = "CAReference00002";
        CVCAFile f = new CVCAFile(name1, name2);
        InputStream in = new ByteArrayInputStream(f.getEncoded());
        CVCAFile f2 = new CVCAFile(in);
        assertTrue(Arrays.equals(f.getEncoded(), f2.getEncoded()));
    }

    public void testReflexive2() {
        String name1 = "CAReference00001";
        CVCAFile f = new CVCAFile(name1);
        InputStream in = new ByteArrayInputStream(f.getEncoded());
        CVCAFile f2 = new CVCAFile(in);
        assertTrue(Arrays.equals(f.getEncoded(), f2.getEncoded()));
    }
}
