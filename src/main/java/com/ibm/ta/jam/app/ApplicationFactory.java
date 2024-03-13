// (C) Copyright IBM Corp. 2024
package com.ibm.ta.jam.app;

import org.tinylog.Logger;

/**
 * Create and return an Application object of the given type.
 */
public class ApplicationFactory {

    public enum ApplicationType {
        WAR,
        EAR,
        UNKNOWN
    }

    public static Application getApplication(String applicationDir, ApplicationType type) throws UnsupportedOperationException {
        if (type == ApplicationType.WAR) {
            Logger.debug("Getting WAR application");
            return new WarApplication(applicationDir);
        } else if (type == ApplicationType.EAR) {
            Logger.debug("Getting EAR application");
            return new EarApplication();
        } else {
            throw new UnsupportedOperationException("Unknown application type. Supported types are WAR and EAR");
        }
    }
}

