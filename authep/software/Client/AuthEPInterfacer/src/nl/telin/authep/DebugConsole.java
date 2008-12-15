package nl.telin.authep;

public class DebugConsole implements ILogger {

	@Override
	public void log(String msg) {
		System.out.println(msg);
	}

}
