package org.jmrtd.imageio;

import java.io.File;
import java.net.URL;

class WSQUtil {

	private static final WSQUtil INSTANCE = new WSQUtil();

	private static String LIBRARY_NAME = "j2wsq";
	private static String USER_DIR = System.getProperty("user.dir");
	private static String BASE_DIR = getBaseDir(WSQUtil.class);

	private static String[] LOCATIONS = {
		USER_DIR,
		BASE_DIR,
		getOSDir(BASE_DIR)
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
			/* System.loadLibrary failed, going to try System.load with likely locations instead. */
			
			for (String location: LOCATIONS) {
				String localLibFileName = System.mapLibraryName(LIBRARY_NAME);
				try {
					String fullLibFilePath = location + getSeparator() + localLibFileName;
					System.out.println("DEBUG: fullLibFilePath = " + fullLibFilePath);
					System.load(fullLibFilePath);
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
			basePathString = basePathString.replace("%20", " ");
			return basePathString;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	private static String getOSDir(String baseDir) {
		String osArch = getOSArch();
		return baseDir + getSeparator() + osArch;
	}

	private static String getOSArch() {
		String osName = System.getProperty("os.name");
		if (osName == null) { osName = ""; }
		osName = osName.toLowerCase();
		String osArch = System.getProperty("os.arch");
		if (osArch == null) { osArch = ""; }
		osArch = osArch.toLowerCase();
		if ("x86".equals(osArch) || "i386".equals(osArch)) { return "i386"; }
		if ("amd64".equals(osArch) || "x86_64".equals(osArch) || "x64".equals(osArch)) { return "x86_64"; }
		return "unknown";
	}
	
	private static String getSeparator() {
		String separator = System.getProperty("file.separator");
		if (separator == null) { separator = "/"; }
		return separator;
	}

}
