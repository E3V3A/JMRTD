package sos.util;

import java.io.File;
import java.net.URL;

public class Files {
	
	private Files() {
	}
	
	public static URL getBaseDir() {
		ClassLoader cl = (new Object() {
			public String toString() { return super.toString(); }
		}).getClass().getClassLoader();

		try {
			URL basePathURL = cl.getResource(".");
			
			if (basePathURL.getProtocol().toLowerCase().startsWith("file")) {
				File basePathFile = new File(basePathURL.getFile());
				File imagesDirFile = new File(basePathFile, "images");
				if (!imagesDirFile.isDirectory()) {
					basePathFile = new File(basePathFile.getParent());
					imagesDirFile = new File(basePathFile, "images");
					basePathURL = new URL("file:" + basePathFile);
				}
			}			
			return basePathURL;
		} catch (Exception e) {
			return null;
		}
	}
}
