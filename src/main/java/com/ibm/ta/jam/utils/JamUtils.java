// (C) Copyright IBM Corp. 2024
package com.ibm.ta.jam.utils;

import static com.ibm.ta.jam.build.BuildToolFactory.BuildToolType.GRADLE;
import static com.ibm.ta.jam.build.BuildToolFactory.BuildToolType.MAVEN;
import static com.ibm.ta.jam.build.BuildToolFactory.BuildToolType.UNKNOWN;

import java.io.File;

import org.tinylog.Logger;

import com.ibm.ta.jam.build.BuildToolFactory.BuildToolType;;

public class JamUtils {
    
    public static BuildToolType getBuildToolType(String applicationDir) {
        if (new File(applicationDir + File.separator + "pom.xml").exists()) {
            Logger.info("Detected pom.xml, setting build tool type to \"maven\"");
            return MAVEN;
        } else if (new File(applicationDir + File.separator + "build.gradle").exists()) {
            Logger.info("Detected build.gradle, setting build tool type to \"gradle\"");
            return GRADLE;
        } else {
            Logger.info("Known build config files not found, so returning UNKNOWN build tool type");
            return UNKNOWN;
        }
    }
}
