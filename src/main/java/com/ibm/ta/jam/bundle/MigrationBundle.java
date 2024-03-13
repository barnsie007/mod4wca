// (C) Copyright IBM Corp. 2024
package com.ibm.ta.jam.bundle;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.tinylog.Logger;

import com.ibm.ta.jam.utils.IOUtils;

/**
 * Model of the Transformation Advisor migration bundle
 */
public class MigrationBundle {
	/**
	 * File representing the zipped migration bundle
	 */
    private File migrationBundleZip;
    
	/**
	 * Create the MgrationBundle object.
	 * @param migrationBundleZip file representing the zipped migration bundle from Transformation Advisor
	 */
    public MigrationBundle(File migrationBundleZip) {
        this.migrationBundleZip = migrationBundleZip;
    }

	/**
	 * Unzip the migration bundle and validate it
	 * @return the ExpandedBundle representing the relevant files and information in the migration bundle
	 * @throws InvalidBundleException when the bundle was found to be invalid
	 */
    public ExpandedBundle validateAndUnzip() throws InvalidBundleException {
		Logger.debug("Start validate and unzip bundle");
	
		if(!migrationBundleZip.exists() || migrationBundleZip.isDirectory()) { 
			throw new InvalidBundleException("Bundle does not exist or is a directory");
		}
		
		try {
			InputStream inputStream = new FileInputStream(migrationBundleZip);
			boolean isUploadFileZipFile = IOUtils.isUploadFileZipFile(inputStream);
			if (!isUploadFileZipFile) {
                throw new InvalidBundleException("Bundle is not a valid Transformation Advisor migration bundle zip file.");
			}
			
			String expandTmpDir = Files.createTempDirectory("ta-").toFile().getAbsolutePath();
			ExpandedBundle expandBundle = unzipBundle(migrationBundleZip.getAbsolutePath(), expandTmpDir);
			if (expandBundle.getServerXmlPath() == null) {
                expandBundle.cleanUpExpandedBundle();
                throw new InvalidBundleException("Bundle does not contain a server.xml file.");
			}

			Logger.info("Bundle is valid.");
			Logger.debug("Finish validate and unzip bundle");
			return expandBundle;
	    } catch (FileNotFoundException fnfe) {
            if (Logger.isDebugEnabled()) {
                fnfe.printStackTrace();
            }
            throw new InvalidBundleException("FileNotFoundException when validating and unzipping the migration bundle.");
        } catch (IOException ioe) {
            if (Logger.isDebugEnabled()) {
                ioe.printStackTrace();
            }
            throw new InvalidBundleException("IOException when validating and unzipping the migration bundle.");
        }
	}

    /**
	 * Unzip the zip file to a given directory location
	 * @param zipFilePath
	 * @param destDir
	 * @return
	 * @throws IOException
	 */
	private static ExpandedBundle unzipBundle(String zipFilePath, String destDir) throws IOException {
		Logger.debug("Start unzipping bundle at " + zipFilePath);
		ExpandedBundle expandedBundle = new ExpandedBundle();
		String serverXmlPath = null;
		String rewriteYamlPath = null;
		String rewritePluginConfigPath = null;
		List<String> libDependencies = new ArrayList();

		File dir = new File(destDir);
		// create output directory if it doesn't exist
		if (!dir.exists()) {
			Logger.debug("Creating output directory: " + destDir);
			dir.mkdirs();
		}
		FileInputStream fis;
		// buffer for read and write data to file
		byte[] buffer = new byte[1024];

		fis = new FileInputStream(zipFilePath);
		ZipInputStream zis = new ZipInputStream(fis);
		ZipEntry ze = zis.getNextEntry();
		while (ze != null) {
			String fileName = ze.getName();
			String filePath = destDir + File.separator + fileName;
			if (fileName.endsWith("server.xml")) {
				serverXmlPath = filePath;
			}
			if (fileName.endsWith(".jar.placeholder")) {
				libDependencies.add(filePath);
			}
			if (fileName.endsWith("recipes.pom")) {
				rewritePluginConfigPath = filePath;
			}
			if (fileName.endsWith("rewrite.yml")) {
				rewriteYamlPath = filePath;
			}

			File newFile = new File(filePath);
			if (ze.isDirectory()) {
				newFile.mkdirs();
			} else {
				// create directories for sub directories in zip
				new File(newFile.getParent()).mkdirs();
				FileOutputStream fos = new FileOutputStream(newFile);
				int len;
				while ((len = zis.read(buffer)) > 0) {
					fos.write(buffer, 0, len);
				}
				fos.close();
			}
			// close this ZipEntry
			zis.closeEntry();
			ze = zis.getNextEntry();
		}
		// close last ZipEntry
		zis.closeEntry();
		zis.close();
		fis.close();
		
		expandedBundle.setExpandedPath(destDir);
		expandedBundle.setServerXmlPath(serverXmlPath);
		expandedBundle.setLibDependenciesPaths(libDependencies);
		expandedBundle.setRewritePluginConfigPath(rewritePluginConfigPath);
		expandedBundle.setRewriteYamlPath(rewriteYamlPath);
		
		Logger.debug("Finish unzipping bundle at " + zipFilePath);
		return expandedBundle;
	}
}
