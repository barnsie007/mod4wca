// (C) Copyright IBM Corp. 2024
package com.ibm.ta.jam.build;

import org.tinylog.Logger;

/**
 * Factory class to create and return a BuildTool object of the given type
 */
public class BuildToolFactory {
    
    public enum BuildToolType {
        MAVEN,
        GRADLE,
        UNKNOWN
    }

    /**
     * Create and return the BuildTool object. 
     * @param applicationDir root directory of the application we are working on
     * @param type the type of the application (WAR|EAR)
     * @return BuildTool object of given type
     * @throws UnsupportedOperationException if the given type is not supported
     */
    public static BuildTool getBuildTool(String applicationDir, BuildToolType type) throws UnsupportedOperationException {
        if (type == BuildToolType.MAVEN) {
            Logger.debug("Getting Maven build tool");
            return new MavenBuildTool(applicationDir);
        } else if (type == BuildToolType.GRADLE) {
            Logger.debug("Getting Gradle build tool");
            return new GradleBuildTool(applicationDir);
        } else {
            throw new UnsupportedOperationException("Unknown build tool type. Supported types are Maven and Gradle");
        }
    }
}