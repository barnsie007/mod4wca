// (C) Copyright IBM Corp. 2024
package com.ibm.ta.jam.utils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import org.apache.commons.io.FileUtils;
import org.tinylog.Logger;

public class TestUtils {

    /**
     * Copy the test app source to a temp location and work from that to keep the source dir clean
     * @param projectLocation
     * @param projectName
     * @return
     */
    public static String copyProjectToTempLocation(String projectLocation, String projectName) {
        String appWorkingLocation = "";
        try {
            String tmpLocation = Files.createTempDirectory("auto-ta.MavenHandlerTest").toFile().getAbsolutePath();
            Logger.info("Begin copying test data to tmp location");
            FileUtils.copyDirectory(new File(projectLocation + File.separator + projectName), new File(tmpLocation + File.separator + projectName));
            Logger.info("End copying test data to tmp location");
            appWorkingLocation = tmpLocation + File.separator + projectName;
            Logger.info("Temp location for app source project: " + appWorkingLocation);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return appWorkingLocation;
    }
}
