/*
 * JMRTD - A Java API for accessing machine readable travel documents.
 *
 * Copyright (C) 2006  SoS group, Radboud University
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
 * $Id$
 */

package sos.mrtd;

import java.security.PublicKey;
import java.util.EventObject;

/**
 * Event to indicate AA protocol was executed.
 * 
 * @author Martijn Oostdijk (martijno@cs.ru.nl)
 *
 * @version $Revision$
 */
public class AAEvent extends EventObject
{	
   private PublicKey pubkey;
   private byte[] m1;
   private byte[] m2;
   private boolean success;
   
   public AAEvent(Object src, PublicKey pubkey, byte[] m1, byte[] m2, boolean success) {
	  super(src);
	  this.pubkey = pubkey;
	  this.m1 = m1;
	  this.m2 = m2;
	  this.success = success;
   }

	public PublicKey getPubkey() {
		return pubkey;
	}

	public byte[] getM1() {
		return m1;
	}

	public byte[] getM2() {
		return m2;
	}

	public boolean isSuccess() {
		return success;
	}
}
