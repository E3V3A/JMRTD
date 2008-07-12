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
			URL imagesURL = cl.getResource("images");
			if (imagesURL == null) {
				imagesURL = new URL(cl.getResource(".") + "../images");
			}
			String protocol = imagesURL.getProtocol().toLowerCase();
			String host = imagesURL.getHost().toLowerCase();
			String imagesDirFileString = imagesURL.getFile();

			File imagesDirFile = new File(imagesDirFileString);
			String basePathString = imagesDirFile.getParent();

			URL basePathURL = new URL(protocol, host, basePathString);
			return basePathURL;
		} catch (Exception e) {
			return null;
		}
	}
}
