/*
 *  AuthEP - Interfacer.
 *
 *  Copyright (C) 2009  Telematica Instituut
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
 */

package nl.telin.authep;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.Provider;
import java.security.PublicKey;
import java.security.Security;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Vector;

import javax.smartcardio.CardTerminal;

import net.sourceforge.scuba.smartcards.CardFileInputStream;
import net.sourceforge.scuba.smartcards.CardManager;
import net.sourceforge.scuba.smartcards.CardService;
import net.sourceforge.scuba.smartcards.CardServiceException;
import net.sourceforge.scuba.smartcards.TerminalCardService;

import org.jmrtd.BACKeySpec;
import org.jmrtd.PassportService;
import org.jmrtd.lds.DG15File;
import org.jmrtd.lds.PassportFile;

/**
 * PassportLink handles communication between the chip on the passport
 * and the program. It's able to e.g. retrieve data files from the passport
 * or let the passport sign a message.
 *
 * @author Dirk-jan.vanDijk
 */
public class PassportLink {

	private static final Provider PROVIDER =
		new org.bouncycastle.jce.provider.BouncyCastleProvider();

	private static final SimpleDateFormat SDF = new SimpleDateFormat("ddMMyy");

	private CardManager _cardManager;
	private Collection<CardTerminal> _cardTerminals;
	private CardTerminal _cardTerminal;
	private CardTerminal _activeCardTerminal;
	private CardService _activeCardService;
	private PassportService _activePassportService;

	/**
	 * Create a new instance of PassportLink. All CardTerminals
	 * will be listed. Upon a request all terminals will be checked
	 * for the presence of a passport. The first terminal to respond
	 * will be chosen for as long as the passport is present.
	 */
	public PassportLink()
	{
		Security.insertProviderAt(PROVIDER, 4);

		_cardManager = CardManager.getInstance();
		_cardTerminals = _cardManager.getTerminals();
		_activeCardTerminal = null;
		_cardTerminal = null;
		_activeCardService = null;
		_activePassportService = null;

		Vector<CardTerminal> terminalsToRemove = new Vector<CardTerminal>();
		for (CardTerminal cardTerminalLoop : _cardTerminals) {
			if(cardTerminalLoop.getName().toUpperCase().contains("EMULATOR"))
			{
				Interfacer.getLogger().log("Removing emulator terminal.");
				terminalsToRemove.add(cardTerminalLoop);
			}
			Interfacer.getLogger().log("terminal name: "+cardTerminalLoop.getName());
		}

		for (CardTerminal removeTerminal : terminalsToRemove)
			_cardManager.getTerminals().remove(removeTerminal);
	}

	/**
	 * Create a new instance of PassportLink specifying the name
	 * of the CardTerminal to use.
	 * @param terminalToUse Name of CardTerminal to use.
	 */
	public PassportLink(String terminalToUse)
	{
		this();
		for (CardTerminal cardTerminalLoop : _cardTerminals) {
			if(cardTerminalLoop.getName().equals(terminalToUse))
				_cardTerminal = cardTerminalLoop;
		}
	}

	private synchronized boolean activateTerminal()
	{
		Interfacer.getLogger().log("ACTIVATE TERMINAL");
		// select which terminal to use
		if(_cardTerminal != null)
			_activeCardTerminal = _cardTerminal;
		else
		{
			while(_activeCardTerminal == null)
			{
				try 
				{
					for (CardTerminal cardTerminalLoop : _cardTerminals)
					{
						if(cardTerminalLoop.isCardPresent() && !cardTerminalLoop.getName().toUpperCase().contains("EMULATOR"))
						{
							_activeCardTerminal = cardTerminalLoop;
							break;
						}
					}
					if(_activeCardTerminal == null)
					{
						Interfacer.getLogger().log("INSERT CARD (s)");
						Thread.sleep(1000);
					}
				}
				catch(Exception e) {}
			}
		}
		Interfacer.getLogger().log("ACTIVE TERMINAL: "+_activeCardTerminal.getName());
		try 
		{
			// wait for a card to be put in the terminal
			while(!_activeCardTerminal.isCardPresent() && !_activeCardTerminal.getName().toUpperCase().contains("EMULATOR"))
			{
				Interfacer.getLogger().log("INSERT CARD (s)");
				Thread.sleep(1000);
			}

			if(_activePassportService == null)
			{
				_activeCardService = new TerminalCardService(_activeCardTerminal);
				_activePassportService = new PassportService(_activeCardService);
				_activePassportService.open();
			}
			Interfacer.getLogger().log("CARD INSERTED AT: "+_activeCardTerminal.getName());
			return true;
		} catch (Exception e) {
			e.printStackTrace();
		} 

		// something went wrong if we reached this point, clear the selected terminal
		Interfacer.getLogger().log("NO TERMINAL COULD BE ACTIVATED");
		return false;
	}

	/**
	 * Close the session with the current card and cardreader.
	 */
	public void Close()
	{
		if(_activePassportService != null)
		{
			_activePassportService.close();
			_activePassportService = null;
			_activeCardService = null;
			_activeCardTerminal = null;
		}
	}

	/**
	 * Performs the Basic Access Control protocol.
	 * @param docNumber the document number
	 * @param dateOfBirth card holder's birth date
	 * @param dateOfExpiry document's expiry date
	 * @throws CardServiceException if authentication failed
	 * @throws ParseException if at least one of the dates could not be parsed
	 */
	public void doBac(String docNumber, String dateOfBirth, String dateOfExpiry) throws CardServiceException, ParseException
	{
		if(activateTerminal())
		{
			_activePassportService.doBAC(new BACKeySpec(docNumber, SDF.parse(dateOfBirth), SDF.parse(dateOfExpiry)));
		}
	}

	/**
	 * Retrieve a DG file from the passport.
	 * @param dgTag Tag of the DG file to retrieve.
	 * @return The bytes of the DG file.
	 * @throws CardServiceException Gets thrown when there's a problem communicating with the passport.
	 * @throws IOException Gets thrown when there's a problem reading the file from the passport.
	 */
	public byte[] getDG(int dgTag) throws CardServiceException, IOException
	{
		if(activateTerminal())
		{
			short dgFID = PassportFile.lookupFIDByTag(dgTag);
			CardFileInputStream dgStream = _activePassportService.readFile(dgFID);
			byte[] data = new byte[dgStream.getFileLength()];
			int read = dgStream.read(data, 0, data.length);
			if(read == data.length) 
				return data;
		}
		return new byte[0];
	}

	/**
	 * Let the passport sign a message using the passports own private key.
	 * @param message The message to sign (can be any size)
	 * @return A signature, the response from the passport.
	 * @throws CardServiceException Gets thrown when there's a problem communicating with the passport.
	 * @throws NoSuchAlgorithmException Gets thrown when there's no SHA1 provider present.
	 */
	public byte[] signWithAA(byte[] message) throws CardServiceException, NoSuchAlgorithmException {
		if(activateTerminal())
		{
			DG15File dg15 = new DG15File(_activePassportService.readFile(PassportService.EF_DG15));
			PublicKey publicKey = dg15.getPublicKey();
			MessageDigest digest = MessageDigest.getInstance("SHA1");
			byte[] digestedMessage = digest.digest(message);
			byte[] m2 = new byte[8];
			System.arraycopy(digestedMessage, 0, m2, 0, m2.length);
			return _activePassportService.sendAA(publicKey, m2);
		}
		return new byte[0];
	}

	public synchronized String getStatus() {
		if (activateTerminal()) {
			try {
				if(_activeCardTerminal.isCardPresent())
					return "Card present";
				else
					return "No card present";
			} catch (Exception e) {
				return e.toString();
			}
		}
		else
			return "Not connected";

	}
}
