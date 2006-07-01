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
 * $Id: JCOPEmulatorService.java,v 1.8 2006/06/14 12:30:24 martijno Exp $
 */

package sos.smartcards;

import com.ibm.jc.JCException;
import com.ibm.jc.terminal.RemoteJCTerminal;

/**
 * JCOPEmulatorService
 * 
 * @author Wojciech Mostowski (woj@cs.ru.nl)
 * 
 * @version $Revision: 1.8 $
 */
public class JCOPEmulatorService extends AbstractCardService {

	private RemoteJCTerminal terminal;
	
	public void open() {
		try {
			terminal = new RemoteJCTerminal();
			terminal.init("localhost:8050");
			terminal.open();
			terminal.waitForCard(1000);
		} catch(JCException jce){
			throw new IllegalStateException("Couldn't establish connection to the emulator: "+terminal.getErrorMessage());
		}
		state = SESSION_STARTED_STATE;
	    notifyStartedAPDUSession();
	}

	public byte[] sendAPDU(Apdu apdu) {
		if(terminal == null) {
			throw new IllegalStateException("Terminal session seems not to be opened.");			
		}
		try {
			byte[] buffer = apdu.getCommandApduBuffer();
			buffer = terminal.send(0, buffer, 0, buffer.length);
			apdu.setResponseApduBuffer(buffer);
			notifyExchangedAPDU(apdu);
			return buffer;
		}catch(JCException jce){
			throw new IllegalStateException("Send APDU failed: "+terminal.getErrorMessage());			
		}
	}

	public void close() {
		try {
			terminal.close();
			terminal = null;
		} catch(JCException jce){
			throw new IllegalStateException("Couldn't establish connection to the emulator: "+terminal.getErrorMessage());
		}
        state = SESSION_STOPPED_STATE;
        notifyStoppedAPDUSession();	
	}

}
