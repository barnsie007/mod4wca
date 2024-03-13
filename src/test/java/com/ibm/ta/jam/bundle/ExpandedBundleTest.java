// (C) Copyright IBM Corp. 2024
package com.ibm.ta.jam.bundle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.File;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;


public class ExpandedBundleTest {

    private final String MODERATE_BUNDLE_LOCATION = "src/test/resources/moderateApp/bundle/modresorts.ear_migrationBundle.zip";
    
    private MigrationBundle bundle;

    @BeforeEach
	void setUp() {
        bundle = new MigrationBundle(new File(MODERATE_BUNDLE_LOCATION));
    }

	@AfterEach
	void tearDown() {
	}

    @Test
    public void expandedBundleTest() {
        ExpandedBundle expandedBundle = null;
        try {
            expandedBundle = bundle.validateAndUnzip();
        } catch (InvalidBundleException ibe) {
            fail("Unable to validate and unzip bundle");
        }

        assertTrue(expandedBundle.getServerXmlPath().endsWith("server.xml"));
        assertTrue(expandedBundle.getRewritePluginConfigPath().endsWith("recipes.pom"));
        assertTrue(expandedBundle.getRewriteYamlPath().endsWith("rewrite.yml"));
        assertEquals(3, expandedBundle.getLibDependenciesPaths().size());
        assertEquals(3, expandedBundle.getLibDependencyNames().size());

        expandedBundle.cleanUpExpandedBundle();
    }

}
