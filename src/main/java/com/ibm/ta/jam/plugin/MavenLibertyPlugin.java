// (C) Copyright IBM Corp. 2024
package com.ibm.ta.jam.plugin;

/**
 * Model of the Maven Liberty plugin
 */
public class MavenLibertyPlugin implements BuildToolPlugin {

	public final static String LIBERTY_MAVEN_PLUGIN_ARTIFACT_ID = "liberty-maven-plugin";
	public final static String LIBERTY_MAVEN_PLUGIN_GROUP_ID = "io.openliberty.tools";
	public final static String LIBERTY_MAVEN_PLUGIN_VERSION = "3.9";
	public final static String LIBERTY_PLUGIN_EMPTY_CONFIGURATION_XML = "<configuration></configuration>";
	public final static String LIBERTY_PLUGIN_COPY_DEPENDENCIES_ELEMENT_NAME = "copyDependencies";
	public final static String LIBERTY_PLUGIN_EMPTY_COPY_DEPENDENCIES_XML = "<copyDependencies></copyDependencies>";
	public final static String LIBERTY_PLUGIN_DEPENDENCY_GROUP_ELEMENT_NAME = "dependencyGroup";
	public final static String ARTIFACT_ID_ELEMENT_NAME = "artifactId";
	public final static String LIBERTY_PLUGIN_COPY_DEPENDENCY_GROUP_XML =
		"<dependencyGroup>" +
			"<location>${project.build.directory}/liberty/wlp/usr/shared/config/lib/global</location>" +
			"<stripVersion>true</stripVersion>" +
		"</dependencyGroup>";
	public final static String LIBERTY_PLUGIN_COPY_DEPENDENCY_XML =
		"<dependency>" +
			"<artifactId>%s</artifactId>" +
			"<groupId>%s</groupId>" +
			"<version>%s</version>" +
		"</dependency>";
		
    public String getArtifactId() {
        return LIBERTY_MAVEN_PLUGIN_ARTIFACT_ID;
    }

    public String getGroupId() {
        return LIBERTY_MAVEN_PLUGIN_GROUP_ID;
    }

    public String getVersion() {
        return LIBERTY_MAVEN_PLUGIN_VERSION;
    }

}
