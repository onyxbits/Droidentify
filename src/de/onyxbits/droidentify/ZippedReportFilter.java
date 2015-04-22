package de.onyxbits.droidentify;

import java.io.File;
import java.io.FilenameFilter;

public class ZippedReportFilter implements FilenameFilter {

	public static String NAME = "fullreport.zip";
	
	@Override
	public boolean accept(File dir, String filename) {
		return !filename.equals(NAME);
	}

}
