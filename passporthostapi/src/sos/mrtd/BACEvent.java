/*
 * JMRTD - A Java API for accessing machine readable travel documents.
 *
 * Copyright (C) 2006  SoS group, ICIS, Radboud University
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

import java.util.EventObject;

/**
 * Event to indicate BAC protocol was executed.
 * 
 * @author Martijn Oostdijk (martijno@cs.ru.nl)
 *
 * @version $Revision$
 */
public class BACEvent extends EventObject
{	
   private PassportAuthService service;
   private boolean success;
   private byte[] rndICC, rndIFD, kICC, kIFD;
   
   /**
    * Constructs a new event.
    * 
    * @param src event source
    * @param wrapper resulting wrapper
    * @param rndICC nonce sent by ICC
    * @param rndIFD nonce sent by IFD
    * @param kICC key material provided by ICC
    * @param kIFD key material provided by IFD
    * @param success status of protocol
    */
   public BACEvent(PassportAuthService service,
		   byte[] rndICC, byte[] rndIFD, byte[] kICC, byte[] kIFD,
		   boolean success) {
	   super(service);
	   this.service = service;
	   this.rndICC = rndICC;
	   this.rndIFD = rndIFD;
	   this.kICC = kICC;
	   this.kIFD = kIFD;
	   this.success = success;
   }

   /**
    * Gets the resulting wrapper.
    * 
    * @return the resulting wrapper
    */
   public SecureMessagingWrapper getWrapper() {
      return service.getWrapper();
   }
	
	/**
     * Gets the status of the executed BAC protocol run.
     * 
	 * @return status of the BAC protocol run.
	 */
	public boolean isSuccess() {
		return success;
	}

    /**
     * Gets the kICC key.
     * 
     * @return the kICC key material
     */
   public byte[] getKICC() {
      return kICC;
   }
   
   /**
    * Gets the kIFD key.
    * 
    * @return the kIFD key material
    */
   public byte[] getKIFD() {
      return kIFD;
   }

   /**
    * Gets the random nonce sent by the ICC during
    * this BAC protocol run.
    * 
    * @return a random nonce
    */
   public byte[] getRndICC() {
      return rndICC;
   }

   /**
    * Gets the random nonce sent by the IFD during
    * this BAC protocol run.
    * 
    * @return a random nonce
    */
   public byte[] getRndIFD() {
      return rndIFD;
   }

   public PassportAuthService getService() {
      return service;
   }
}
