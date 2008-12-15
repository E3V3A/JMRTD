package nl.telin.authep;

import java.io.IOException;

/**
 * The Interfacer will communicate between the client and IdP. The Interfacer
 * will basically perform actions on the digital passport on behalf of the IdP.
 * These actions will consist of sending data files across and performing advanced
 * authentication.
 * @author Dirk-jan.vanDijk
 *
 */
public class Interfacer 
{
	private static PassportLink _passportLink;
	private static NetworkLink _networkLink;
	private static ILogger _logger;
	private boolean isRunning = false;
	
	/**
	 * Create a new instance of the Interfacer.
	 * @throws IOException This exception gets thrown when an error occurs upon creating
	 * the network link.
	 */
	public Interfacer(ILogger logger) throws IOException
	{
		_logger = logger;
		//_passportLink = new PassportLink("OMNIKEY CardMan 5x21-CL 0");
		_passportLink = new PassportLink();
		
		_networkLink = new NetworkLink();
		//_networkLink.start();
		
		isRunning = true;
	}

	/**
	 * Start a new AuthEPInterfacer.
	 * @param args Not used at this moment.
	 */
	public static void main(String[] args)
	{
		System.out.println("########################");
		System.out.println("# Auth EP - Interfacer #");
		System.out.println("########################");
		System.out.println();
		
		try {
			Interfacer interfacer = new Interfacer(new DebugConsole());
			while(interfacer.isRunning)
			{
				Thread.sleep(1000);
			}
		} catch (IOException e) {
			System.out.println("Error - "+e.getMessage());
			e.printStackTrace();
			System.exit(-1);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		System.exit(0);
	}
	
	/**
	 * Get the PassportLink associated with this Interfacer.
	 * @return The PassportLink.
	 * @see PassportLink
	 */
	public static PassportLink getPassportLink()
	{
		return _passportLink;
	}
	
	/**
	 * Get the NetworkLink associated with this Interfacer.
	 * @return The NetworkLink
	 * @see NetworkLink
	 */
	public static NetworkLink getNetworkLink()
	{
		return _networkLink;
	}
	
	/**
	 * Get the ILogger associated with this Interfacer.
	 * @return The ILogger
	 * @see ILogger
	 */
	public static ILogger getLogger()
	{
		return _logger;
	}
}
