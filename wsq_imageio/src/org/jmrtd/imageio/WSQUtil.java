package org.jmrtd.imageio;

class WSQUtil
{
	private static final WSQUtil INSTANCE = new WSQUtil();
	
	private boolean isLibraryLoaded;
	
	private WSQUtil() {
		isLibraryLoaded = false;
	}
	
	public static void loadLibrary() {
		if (INSTANCE.isLibraryLoaded) { return; }
		String libraryName = "j2wsq";
		try {
			System.loadLibrary(libraryName);
		} catch (UnsatisfiedLinkError ule1) {
			String separator = System.getProperty("file.separator");
			if (separator == null) { separator = "/"; }
			String pwd = System.getProperty("user.dir");
			System.load(pwd + separator + System.mapLibraryName(libraryName));
		}
		INSTANCE.isLibraryLoaded = true;
	}
}
