/*
 * ACR 120 USB driver for javax.smartcardio framework.

 * Copyright (C) 2008  Martijn Oostdijk
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * $Id: $
 */

package sos.smartcards;

import java.util.ArrayList;
import java.util.List;

import javax.smartcardio.CardException;
import javax.smartcardio.CardTerminal;
import javax.smartcardio.CardTerminals;
import javax.smartcardio.TerminalFactorySpi;

/**
 * This creates CREFCardTerminal instances.
 *
 * @author Martijn Oostdijk (martijn.oostdijk@gmail.com)
 * @version $Revision: $
 */
public class CREFEmulatorTerminalFactorySpi  extends TerminalFactorySpi
{
	private static final List<CardTerminal> TERMINAL_LIST = new ArrayList<CardTerminal>();
	private static final CardTerminals TERMINALS = new CardTerminals() {
		public List<CardTerminal> list(State state) throws CardException {
			return TERMINAL_LIST;
		}

		public boolean waitForChange(long timeout) throws CardException {
			return false;
		}		
	};

	public CREFEmulatorTerminalFactorySpi(Object parameter) {
		if (!parameter.getClass().equals(String.class)) { throw new IllegalArgumentException("Invalid parameter"); } 
		try {
			String paramString = (String)parameter;
			int colonIndex = paramString.indexOf(':');
			String host = paramString.substring(0, colonIndex).trim();
			int port = Integer.parseInt(paramString.substring(colonIndex + 1).trim());
			TERMINAL_LIST.add(new CREFEmulatorTerminal(host, port));
		} catch (NumberFormatException nfe) {
			throw new IllegalArgumentException("Invalid parameter");
		}
	}

	protected CardTerminals engineTerminals() {
		return TERMINALS;
	}
}
