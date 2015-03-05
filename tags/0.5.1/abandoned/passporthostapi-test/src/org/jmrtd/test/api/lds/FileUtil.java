package org.jmrtd.test.api.lds;

import java.io.File;
import java.io.FileFilter;

public class FileUtil {

	/**
	 * Files containing individual LDS files (such as COM, DG1, ..., SOd) or
	 * zipped collections of these.
	 */
	public static File[] getSamples(String dirName, final String extension) {
		File dirFile = new File(dirName);
		File[] files = dirFile.listFiles(new FileFilter() {
			@Override
			public boolean accept(File pathname) {
				if (extension == null) { return false; }
				String ext = extension;
				if (ext.startsWith(".")) { ext = "." + ext; }
				return pathname.getName().endsWith(ext)
						|| pathname.getName().endsWith(ext.toLowerCase())
						|| pathname.getName().endsWith(ext.toUpperCase());
			}

		});
		return files;
	}
}
