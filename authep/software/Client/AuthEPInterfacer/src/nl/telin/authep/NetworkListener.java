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
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

/**
 * The NetworkListener will listen and handle incoming requests from the
 * AuthEP Identity Provider. A very simple protocol is used for this.
 *
 * @author Dirk-jan.vanDijk
 *
 * @see NetworkProtocol
 */
public class NetworkListener extends Thread {

	private Socket _socket;
	private InputStream _inputStream;
	private OutputStream _outputStream;
	private boolean _running = false;

	/**
	 * Create a new instance of NetworkListener using the specified socket.
	 * @param socket Socket to listen to for incoming commands.
	 * @throws IOException
	 */
	public NetworkListener(Socket socket) throws IOException
	{
		_socket = socket;
		_outputStream = _socket.getOutputStream();
		_inputStream = _socket.getInputStream();

	}

	/**
	 * Start listening for incoming commands.
	 * (This method will block and therefore the Start method should be used in order to
	 * get it running in a separate thread.)
	 */
	@Override
	public void run() {
		_running = true;
		while(_running)
		{
			try {
				int command = _inputStream.read();
				switch(command)
				{
				case NetworkProtocol.SEND_BAC:
					String docNumber = readString();
					String dateOfBirth = readString();
					String dateOfExpiry = readString();
					Interfacer.getLogger().log("BAC Received: "+docNumber+"<<<"+dateOfBirth+"<<<"+dateOfExpiry);
					Interfacer.getPassportLink().doBac(docNumber,dateOfBirth,dateOfExpiry);
					break;
				case NetworkProtocol.REQUEST_FILE:
					int dgTag = _inputStream.read();
					Interfacer.getLogger().log("REQUEST_FILE: TAG-"+dgTag);
					byte[] data = Interfacer.getPassportLink().getDG(dgTag);
					Interfacer.getLogger().log("REQUEST_FILE: SIZE-"+data.length);
					writeBytes(data);
					break;
				case NetworkProtocol.SEND_CHALLENGE:
					int challengeLength = _inputStream.read();
					byte[] challenge = new byte[challengeLength];
					_inputStream.read(challenge,0,challenge.length);
					Interfacer.getLogger().log("SEND_CHALLENGE RECEIVED.");
					byte[] response = Interfacer.getPassportLink().signWithAA(challenge);
					writeBytes(response);
					Interfacer.getLogger().log("SEND_CHALLENGE RESPONSE SEND.");
					break;
				case NetworkProtocol.CLOSE:
					_running = false;
					break;
				}
			} catch (Exception e) {
				e.printStackTrace();
				_running = false;
			}
		}

		try
		{
			System.out.println("Passport closed");
			Interfacer.getPassportLink().Close();
			System.out.println("Connection closed.");
			_socket.close();
		}
		catch(Exception e) {}
	}

	private String readString() throws IOException
	{
		int length = _inputStream.read();
		byte[] data = new byte[length];
		_inputStream.read(data,0,data.length);
		String result = new String(data,"utf-8");
		return result;
	}

	private void writeBytes(byte[] data) throws IOException
	{
		_outputStream.write(intToByteArray(data.length));
		_outputStream.write(data);
		_outputStream.flush();
	}

	@SuppressWarnings("unused")
	private void writeString(String dataString) throws IOException
	{		
		byte[] dataBytes = dataString.getBytes("UTF-8");
		_outputStream.write(intToByteArray(dataBytes.length));
		_outputStream.write(dataBytes);
		_outputStream.flush();
	}

	/**
	 * Convert an integer to a LSB byte array.
	 * @param value Integer value to convert
	 * @return a LSB ordered byte array
	 */
	public static final byte[] intToByteArray(int value) 
	{
		return new byte[] {
				(byte)value,
				(byte)(value >>> 8),
				(byte)(value >>> 16),
				(byte) (value >>> 24)};
	}

}
