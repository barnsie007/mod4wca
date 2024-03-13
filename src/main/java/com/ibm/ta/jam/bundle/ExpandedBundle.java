// (C) Copyright IBM Corp. 2024
package com.ibm.ta.jam.bundle;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.tinylog.Logger;

import lombok.*;

/**
 * Class that models the contents of the migration bundle after it has been uncompressed
 */
@NoArgsConstructor
@Setter(AccessLevel.PACKAGE)
@Getter
public class ExpandedBundle {
	/**
	 * Extension used by placeholder jar files
	 */
    private final String PLACEHOLDER_EXTN = ".placeholder";

	/**
	 * Full path to location of uncompressed migration bundle
	 */
	private String expandedPath;

	/**
	 * Full path to server.xml file
	 */
	private String serverXmlPath;

	/**
	 * Full path to rewrite plugin configuration (recipes.pom)
	 */
	private String rewritePluginConfigPath;

	/**
	 * Full path to the rewrite.yml
	 */
	private String rewriteYamlPath;

	/**
	 * Full path to the dependencies listed in the migration bundle
	 */
	private List<String> libDependenciesPaths = new ArrayList<>();

    /**
	 * Cleanup an expanded bundle
	 * @param expandedBundle
	 */
	public void cleanUpExpandedBundle() {
		try {
			FileUtils.deleteDirectory(new File(expandedPath));
		} catch (IOException e) {
			Logger.warn("The expanded migration bundle was not fully cleaned up: " + expandedPath);
			if (Logger.isDebugEnabled()) {
                e.printStackTrace();
            }
		}
	}

	/**
	 * Get the names of all the dependency jar files listed in the migratino bundle
	 * @return a list of names from the migration bundle
	 */
    public List<String> getLibDependencyNames() {
        return libDependenciesPaths
            .stream()
            .map(d -> new File(d).getName().replace(PLACEHOLDER_EXTN, ""))
            .collect(Collectors.toList());
    }
}
