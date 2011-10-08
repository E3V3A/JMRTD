/*
 * JMRTD - A Java API for accessing machine readable travel documents.
 *
 * Copyright (C) 2006 - 2011  The JMRTD team
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

package org.jmrtd.cbeff;

import java.io.IOException;
import java.io.OutputStream;

/**
 * Interface to be implemented by client code to encode BDB implementations.
 * 
 * @author The JMRTD team (info@jmrtd.org)
 * 
 * @version $Revision: $
 * 
 * @param <B> the type of BDB implementation that is encoded by this encoder
 */
public interface BiometricDataBlockEncoder<B extends BiometricDataBlock> {

	void encode(B bdb, OutputStream out) throws IOException;
}
