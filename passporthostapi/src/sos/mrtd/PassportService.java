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

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.GeneralSecurityException;

import sos.smartcards.CardService;

/**
 * High level card passport service for using the passport.
 * Defines high-level commands to access the information on the passport.
 * Based on ICAO-TR-LDS.
 * 
 * Usage:
 *    <pre>
 *       &lt;&lt;create&gt;&gt; ==&gt; open() ==&gt;
 *       doBAC(...) ==&gt; readBlahFile() ==&gt; close()
 *    </pre> 
 *
 * @author Martijn Oostdijk (martijno@cs.ru.nl)
 *
 * @version $Revision$
 */
public class PassportService extends PassportAuthService
{
   private PassportFileService passportFileService;

   /**
    * Creates a new passport service for accessing the passport.
    * 
    * @param service another service which will deal
    *        with sending the apdus to the card.
    *
    * @throws GeneralSecurityException when the available JCE providers
    *         cannot provide the necessary cryptographic primitives.
    */
   public PassportService(CardService service)
   throws GeneralSecurityException, UnsupportedEncodingException {
      super(service);
      if (service instanceof PassportService) {
         this.passportFileService =
            ((PassportService)service).passportFileService;
      } else {
         this.passportFileService = new PassportFileService(service);
      }
      addAuthenticationListener(passportFileService);
   }

   /**
    * Gets the data group presence list.
    * 
    * @return the file containing the data group presence list
    * @throws IOException if the file cannot be read
    */
   public COMFile readCOMFile() throws IOException {
      return (COMFile)getFile(PassportFile.EF_COM_TAG);
   }

   /**
    * Gets the data group indicated by <code>tag</code>.
    * 
    * @param tag should be a valid ICAO datagroup tag
    * 
    * @return the data group file
    * 
    * @throws IOException if the file cannot be read
    */
   public DataGroup readDataGroup(int tag) throws IOException {
      return (DataGroup)getFile(tag);
   }
   
   /**
    * Gets DG1.
    * 
    * @return the data group file
    * 
    * @throws IOException if the file cannot be read
    */
   public DG1File readDG1() throws IOException {
      return (DG1File)readDataGroup(PassportFile.EF_DG1_TAG);
   }
   
   /**
    * Gets DG2.
    * 
    * @return the data group file
    * 
    * @throws IOException if the file cannot be read
    */   
   public DG2File readDG2() throws IOException {
      return (DG2File)readDataGroup(PassportFile.EF_DG2_TAG);
   }
   
   /**
    * Gets DG15.
    * 
    * @return the data group file
    * 
    * @throws IOException if the file cannot be read
    */
   public DG15File readDG15() throws IOException {
      return (DG15File)readDataGroup(PassportFile.EF_DG15_TAG);
   }
   
   /**
    * Gets the document security object.
    * 
    * @return the document security object
    * 
    * @throws IOException if the file cannot be read
    */
   public SODFile getSODFile() throws IOException {
      return (SODFile)getFile(PassportFile.EF_SOD_TAG);
   }

   /**
    * Gets the file indicated by tag.
    * 
    * @param tag ICAO file tag
    * 
    * @return the file
    * 
    * @throws IOException if the file cannot be read
    */
   private PassportFile getFile(int tag) throws IOException {
      short fid = PassportFile.lookupFIDByTag(tag);
      return PassportFile.getInstance(passportFileService.readFile(fid));
   }
}
