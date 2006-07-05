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

package sos.smartcards;

/**
 * Service for communicating with a smart card.
 *
 * @author Martijn Oostdijk (martijno@cs.ru.nl)
 *
 * @version $Revision: 1.7 $
 */
public interface CardService
{
   /**
    * Gives a list of terminals (card accepting devices) accessible by
    * this service.
    *
    * @return a list of terminal names
    */
   String[] getTerminals();
   
   /**
    * Adds a listener.
    *
    * @param l the listener to add
    */
   void addAPDUListener(APDUListener l);

   /**
    * Removes the listener <code>l</code>, if present.
    *
    * @param l the listener to remove
    */
   void removeAPDUListener(APDUListener l);

   /**
    * Opens a session with the card in the default terminal.
    */
   void open();

   /**
    * Opens a session with the card designated by <code>id</code>.
    * 
    * @param id some identifier (typically the name of a card terminal)
    */
   void open(String id);
   
   /**
    * Sends an apdu to the card.
    *
    * @param capdu the command apdu to send.
    *
    * @return the response from the card, including the status word.
    */
   byte[] sendAPDU(Apdu capdu);

   /**
    * Closes the session with the card.
    */
   void close();
}

