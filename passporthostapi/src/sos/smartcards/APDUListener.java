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
 * $Id: APDUListener.java,v 1.6 2006/06/08 14:52:30 martijno Exp $
 */

package sos.smartcards;

import java.util.EventListener;

/**
 * Specifies an event handler type to react to apdu events.
 *
 * @author Engelbert Hubbers (hubbers@cs.ru.nl)
 * @author Martijn Oostdijk (martijno@cs.ru.nl)
 *
 * @version $Revision: 1.6 $
 */
public interface APDUListener extends EventListener
{
   /**
    * Is called after a session (re)starts.
    */
   void startedAPDUSession();

   /**
    * Is called after an apdu was exchanged.
    *
    * @param capdu a Command apdu
    * @param rapdu the Response apdu
    */
   void exchangedAPDU(Apdu capdu, byte[] rapdu);

   /**
    * Is called after a session stops.
    */
   void stoppedAPDUSession();
}

