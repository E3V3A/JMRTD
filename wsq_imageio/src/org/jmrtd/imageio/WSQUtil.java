package org.jmrtd.imageio;

import java.io.File;
import java.net.URL;

class WSQUtil
{
	private static final WSQUtil INSTANCE = new WSQUtil();

	private static String LIBRARY_NAME = "j2wsq";

	private static String[] LOCATIONS = {
		System.getProperty("user.dir"),
		getBaseDir(WSQUtil.class)
	};


	private boolean isLibraryLoaded;

	private WSQUtil() {
		isLibraryLoaded = false;
	}

	public static void loadLibrary() {
		if (INSTANCE.isLibraryLoaded) { return; }
		try {
			System.loadLibrary(LIBRARY_NAME);
		} catch (UnsatisfiedLinkError ule1) {
			String separator = System.getProperty("file.separator");
			if (separator == null) { separator = "/"; }
			for (String location: LOCATIONS) {
				String localLibFileName = System.mapLibraryName(LIBRARY_NAME);
				try {
					System.out.println("DEBUG: trying " + location);
					System.load(location + separator + localLibFileName);
					INSTANCE.isLibraryLoaded = true;
					break;
				} catch (UnsatisfiedLinkError ule2) {
					continue;
				}
			}
			if (!INSTANCE.isLibraryLoaded) { throw ule1; }
		}
	}

	/**
	 * This will hopefully find the parent of the jar or the folder that
	 * <code>c</code> resides in.
	 * 
	 * @param c a class
	 * @return a path to where <code>c</code> was loaded from
	 */
	private static String getBaseDir(Class<?> c) {
		try {
			String className = c.getCanonicalName();
			if (className == null) { className = c.getName(); }
			String pathToClass = "/" + className.replace(".", "/") + ".class";
			URL url = c.getResource(pathToClass);

			String dirString = url.getFile();
			int classNameIndex = dirString.indexOf(pathToClass);
			String basePathString = dirString.substring(0, classNameIndex);
			if (basePathString.endsWith(".jar!")) {
				basePathString = (new File(basePathString)).getParent();
			} else if (basePathString.endsWith("/bin")
					|| basePathString.endsWith("/bin/")
					|| basePathString.endsWith("/build")
					|| basePathString.endsWith("/build/")) {
				basePathString = (new File(basePathString)).getParent();
			}
			if (basePathString.startsWith("file:\\")) {
				basePathString = basePathString.substring("file:\\".length());
			}
			return basePathString.replace("%20", " ");
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
}
