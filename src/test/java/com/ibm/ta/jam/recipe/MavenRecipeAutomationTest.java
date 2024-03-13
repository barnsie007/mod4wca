// (C) Copyright IBM Corp. 2024
package com.ibm.ta.jam.recipe;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.ibm.ta.jam.build.BuildTool;
import com.ibm.ta.jam.build.BuildToolFactory;
import com.ibm.ta.jam.build.BuildToolFactory.BuildToolType;
import com.ibm.ta.jam.bundle.ExpandedBundle;
import com.ibm.ta.jam.bundle.InvalidBundleException;
import com.ibm.ta.jam.bundle.MigrationBundle;
import com.ibm.ta.jam.utils.JamUtils;
import com.ibm.ta.jam.utils.TestUtils;

public class MavenRecipeAutomationTest {

    private final String MAVEN_MODERATE_APP_LOCATION = "src/test/resources/moderateApp/source";
    private final String MAVEN_MODERATE_APP_NAME = "modresorts";
    private final String MODERATE_BUNDLE_LOCATION = "src/test/resources/moderateApp/bundle/modresorts.ear_migrationBundle.zip";
    
    private String applicationDir;
    private ExpandedBundle expandedBundle;
    private BuildTool buildTool;

    @BeforeEach
	void setUp() {
        applicationDir = TestUtils.copyProjectToTempLocation(MAVEN_MODERATE_APP_LOCATION, MAVEN_MODERATE_APP_NAME);
        MigrationBundle bundle = new MigrationBundle(new File(MODERATE_BUNDLE_LOCATION));
        try {
            expandedBundle = bundle.validateAndUnzip();
        } catch (InvalidBundleException ibe) {
            ibe.printStackTrace();
        }

        BuildToolType buildSysType = JamUtils.getBuildToolType(applicationDir);
        buildTool = BuildToolFactory.getBuildTool(applicationDir, buildSysType);
    }

	@AfterEach
	void tearDown() {
        try {
			FileUtils.deleteDirectory(new File(applicationDir).getParentFile());
        } catch (IOException e) {
            e.printStackTrace();
        }
        expandedBundle.cleanUpExpandedBundle();
	}

    @Test
    public void getAllRecipesTest() {

        MavenRecipeAutomation rAuto = new MavenRecipeAutomation(expandedBundle.getRewritePluginConfigPath(), expandedBundle.getRewriteYamlPath(), buildTool);

        List<String> recipes = rAuto.getAllRecipes();

        assertEquals(3, recipes.size());
        assertTrue(recipes.contains("org.openrewrite.java.liberty.ServerName"));
        assertTrue(recipes.contains("org.openrewrite.java.liberty.RemoveWas2LibertyNonPortableJndiLookup"));
        assertTrue(recipes.contains("org.openrewrite.java.liberty.WebSphereUnavailableSSOCookieMethod"));
    }

}
