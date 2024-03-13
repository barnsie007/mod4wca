// (C) Copyright IBM Corp. 2024
package com.ibm.ta.jam.app;

import java.io.File;
import java.util.List;

/**
 * Model of a Java application that we want to modernize. It is expected that WAR and EAR applications implement this interface.
 */
public interface Application {

    public static final String RELATIVE_LIB_DIR = "src" + File.separator + "main" + File.separator + "liberty" + File.separator + "lib";
	public static final String RELATIVE_CONFIG_DIR = "src" + File.separator + "main" + File.separator + "liberty" + File.separator + "config";
	public static final String RELATIVE_SERVER_XML_LOCATION = RELATIVE_CONFIG_DIR + File.separator + "server.xml";
    public static final String RELATIVE_TARGET_LIB_DIR = "liberty/wlp/usr/shared/config/lib/global";
	public static final String PLACEHOLDER_WILDCARD = "*.jar.placeholder";

	public boolean addServerConfigFromBundle(String serverXmlPath);

    public boolean addPlaceholderDependencies();

    public boolean addLocalDependencies(List<String> dependencyPaths);
}

