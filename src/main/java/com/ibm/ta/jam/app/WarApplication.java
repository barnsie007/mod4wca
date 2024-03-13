// (C) Copyright IBM Corp. 2024
package com.ibm.ta.jam.app;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.tinylog.Logger;

/**
 * Model of an Application that builds to a WAR archive
 */
public class WarApplication implements Application {

    private String applicationDir;

    /**
     * Create WarApplication object
     * @param applicationDir the root directory for the application
     */
    public WarApplication(String applicationDir) {
        this.applicationDir = applicationDir;
    }
    
    /**
     * Add the given server.xml to the appropriate location in the application (Application.RELATIVE_SERVER_XML_LOCATION)
     * This method will attempt to create the location for the server.xml if it doesn't exist.
     * If the server.xml already exists, the file will be overwritten.
     * 
     * @param serverXmlPath full path to the server.xml to add to the application
     * @return a boolean indicating if server.xml was successfull added.
     */
    @Override
	public boolean addServerConfigFromBundle(String serverXmlPath) {
        Logger.debug("Start adding server.xml to application: " + applicationDir);
		String configDirPath = applicationDir + File.separator + RELATIVE_CONFIG_DIR;
		String newServerXmlLocation = applicationDir + File.separator + RELATIVE_SERVER_XML_LOCATION;

        // Setup Liberty config directory
		Logger.debug("Creating liberty config directory: " + configDirPath);
        File configDir = new File(configDirPath);
        if (!configDir.exists()) {
		    if (!configDir.mkdirs()) {
                Logger.error("Error occurred creating config directory: " + configDirPath);
                return false;
            }
        } else {
            Logger.info("Config dir location already exists.");
        }

        // Copy server.xml
		try {
            if (new File(newServerXmlLocation).exists()) {
                Logger.warn("server.xml already exists. Overwriting. " + newServerXmlLocation);
            }

            FileUtils.copyFile(new File(serverXmlPath), new File(newServerXmlLocation));
        } catch (IOException ioe) {
            Logger.error("Error occurred copying server.xml from migration bundle to: " + newServerXmlLocation);
            return false;
        }

        Logger.debug("Finish adding server.xml to application: " + applicationDir);
        return true;
    }

    /**
     * Add placeholder dependencies to the application. 
     * The placeholders may be replace by the user in future with the actual jars
     */
    @Override
    public boolean addPlaceholderDependencies() {
        // TODO: implement me
        return false;
    }

    /**
     * Copies the given dependencies (jar files) to the appropriate location in the application. (Application.RELATIVE_LIB_DIR)
     * If files already exist, they will be overwritten.
     * Note, if using local dependencies like this, a resource must also be added to the build tool to ensure they are 
     * included in the build {@link com.ibm.ta.jam.build.BuildTool#addLocalDependencies(List)}
     * @param dependencyPaths full paths to the jars to copy
     * @return a boolean indicating if the adding of dependencies was successful
     */
    @Override
    public boolean addLocalDependencies(List<String> dependencyPaths) {
		Logger.debug("Start adding local dependencies to application");
        String libDirPath = applicationDir + File.separator + RELATIVE_LIB_DIR;
        File libDir = new File(libDirPath);
        if (!libDir.exists()) {
            if (!libDir.mkdirs()) {
                Logger.error("Error occurred creating lib directory: " + libDirPath);
                return false;
            }
        }

        // Copy libs
        for (String dep : dependencyPaths) {
            String depName = new File(dep).getName();
            String newLocation = libDirPath + File.separator + depName;

		    try {
                File newFile = new File(newLocation);
                if (newFile.exists()) {
                    Logger.warn("File already exists, overwriting: " + newLocation);
                }

                FileUtils.copyFile(new File(dep), newFile);
            } catch (IOException ioe) {
                Logger.error("Error occurred copying library to new location: " + depName + " " + newLocation);
                return false;
            }
        }
        return true;
    }
}
