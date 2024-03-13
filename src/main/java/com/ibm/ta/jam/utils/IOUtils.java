// (C) Copyright IBM Corp. 2024
package com.ibm.ta.jam.utils;

import java.io.File;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.tinylog.Logger;

public class IOUtils {

    public static boolean isValidDirectory(String directory) {
        if (directory == null) {
            Logger.error("Application directory cannot be null");
            return false;
        }
        if (!new File(directory).exists()) {
            Logger.error("Application directory does not exist");
            return false;
        }

        if (!new File(directory).isDirectory()) {
            Logger.error("Application directory is not a valid directory");
            return false;
        }

        return true;
    }

    public static boolean isValidFlie(String file) {
        if (file == null) {
            Logger.error("File cannot be null");
            return false;
        }
        if (!new File(file).exists()) {
            Logger.error("File does not exist");
            return false;
        }

        if (!new File(file).isFile()) {
            Logger.error("File is not a valid file");
            return false;
        }
        return true;
    }
    
    // This method will check that a file is an actual zip file and not based on zip
	// structure, so jar/ear/war etc will be rejected
	public static boolean isUploadFileZipFile(final InputStream fileInputStream) {
		try {
			ZipInputStream zis = new ZipInputStream(fileInputStream);
			ZipEntry ze;

			boolean ofTypeZip = false;
			while ((ze = zis.getNextEntry()) != null) {
				ofTypeZip = true; // If the file is not a zip then we dont get here and will return false
				if (ze.getName().equals("META-INF/MANIFEST.MF")) {
					// This is a jar,ear,war file: we will reject
					return false;
				}
			}
			return ofTypeZip;
		} catch (Exception e) {
			return false;
		}
	}
}
