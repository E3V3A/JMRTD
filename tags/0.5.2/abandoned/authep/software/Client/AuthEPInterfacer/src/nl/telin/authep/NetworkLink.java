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
import java.net.ServerSocket;
import java.net.Socket;

/**
 * The NetworkLink handles communication between the AuthEPInterfacer and the
 * AuthEP identity provider. The NetworkLink will try to accept incoming connections
 * from the IdP and create NetworkListener's for every incoming call.
 *
 * @author Dirk-jan.vanDijk
 *
 * @see NetworkListener
 */
public class NetworkLink extends Thread
{

	private ServerSocket _serverSocket;
	private boolean _running = false;

	/**
	 * Create a new instance of NetworkLink.
	 * @throws IOException Gets thrown when it's not possible to create a listening socket.
	 */
	public NetworkLink() throws IOException
	{
		//_serverSocket = new ServerSocket(9303);
		Socket s = new Socket("authep.nl",9303);
		NetworkListener listener = new NetworkListener(s);
		listener.start();
		Interfacer.getLogger().log("Connection to IDP open.");
	}

	/**
	 * Stop accepting new clients.
	 */
	public void stopAccept()
	{
		try { if (_serverSocket != null) { _serverSocket.close(); } } catch (IOException ioe) { /* At least we tried... */ }
		_serverSocket = null;
		_running = false;
	}

	/**
	 * Will be listening for incoming connections for as long as the networklink is running.
	 * (This method will block and therefore the Start method should be used in order to
	 * get it running in a separate thread.)
	 */
	@Override
	public void run() 
	{
		_running = true;
		while(_running)
		{
			try
			{
				if (_serverSocket == null) { _serverSocket = new ServerSocket(9303); }
				Socket newClient = _serverSocket.accept();
				System.out.println("connected to: "+newClient.getRemoteSocketAddress().toString());
				// TODO: Only accept if IP is from the IdP
				NetworkListener newListener = new NetworkListener(newClient);
				newListener.start();
			}
			catch(IOException e)
			{
				e.printStackTrace();
			}
		}
	}

	public String getStatus() {
		if (!_running) {
			return "Stopped";
		}
		return _serverSocket.toString();
	}
}
