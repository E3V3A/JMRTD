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

/**
 * Listener for authentication events.
 *
 * FIXME: perhaps add some more details to the events (which nonces were
 * exchanged, for instance) and wrap those into an actual event object.
 * 
 * @author Martijn Oostdijk (martijno@cs.ru.nl)
 * 
 * @version $Revision$
 */
public interface AuthListener {

   /**
    * Called when an attempt was made to perform the BAC protocol.
    *
    * @param wrapper the resulting wrapper or <code>null</code>
    */
   void performedBAC(SecureMessagingWrapper wrapper);
   
   /**
    * Called when an attempt was made to perform the AA protocol.
    *
    * @param pubkey the key used
    * @param success indicates whether the AA protocol run was successfull
    */
   void performedAA(PublicKey pubkey, boolean success);
}

