// (C) Copyright IBM Corp. 2024
package com.ibm.ta.jam.plugin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.File;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.ibm.ta.jam.bundle.ExpandedBundle;
import com.ibm.ta.jam.bundle.InvalidBundleException;
import com.ibm.ta.jam.bundle.MigrationBundle;

public class MavenRewritePluginTest {

    private final String MODERATE_BUNDLE_LOCATION = "src/test/resources/moderateApp/bundle/modresorts.ear_migrationBundle.zip";
    
    private ExpandedBundle expandedBundle;

    @BeforeEach
	void setUp() {
        MigrationBundle bundle = new MigrationBundle(new File(MODERATE_BUNDLE_LOCATION));
        try {
            expandedBundle = bundle.validateAndUnzip();
        } catch (InvalidBundleException ibe) {
            fail("Unable to validate and unzip bundle");
        }
    }

	@AfterEach
	void tearDown() {
	}

    @Test
    public void mavenRewritePlugin() {
        MavenRewritePlugin rewritePlugin = null;
        try {
            rewritePlugin = new MavenRewritePlugin(expandedBundle.getRewritePluginConfigPath());
        } catch (PluginInitializationException e) {
            fail("Failed to create MavenRewritePlugin");
        }

        assertEquals(rewritePlugin.getActiveRecipes().size(), 3);
        assertEquals(rewritePlugin.getArtifactId(), "rewrite-maven-plugin");
        assertEquals(1, rewritePlugin.getDependencies().size());
        assertEquals(rewritePlugin.getRewriteLibertyArtifactId(), "rewrite-liberty");
    }
}
