// (C) Copyright IBM Corp. 2024
package com.ibm.ta.jam.build;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.ibm.ta.jam.build.BuildToolFactory.BuildToolType;
import com.ibm.ta.jam.bundle.ExpandedBundle;
import com.ibm.ta.jam.bundle.InvalidBundleException;
import com.ibm.ta.jam.bundle.MigrationBundle;
import com.ibm.ta.jam.utils.JamUtils;
import com.ibm.ta.jam.utils.TestUtils;

public class MavenBuildToolTest {
    private final String GRADLE_SIMPLE_APP_LOCATION = "src/test/resources/simpleApp/source-gradle";
    private final String GRADLE_SIMPLE_APP_NAME = "weatherCheckDemo";

    private final String MAVEN_MODERATE_APP_LOCATION = "src/test/resources/moderateApp/source";
    private final String MAVEN_MODERATE_APP_NAME = "modresorts";
    private final String MODERATE_BUNDLE_LOCATION = "src/test/resources/moderateApp/bundle/modresorts.ear_migrationBundle.zip";
    
    private String mavenAppDir;
    private String gradleAppDir;
    private ExpandedBundle expandedBundle;

    @BeforeEach
	void setUp() {
        mavenAppDir = TestUtils.copyProjectToTempLocation(MAVEN_MODERATE_APP_LOCATION, MAVEN_MODERATE_APP_NAME);
        gradleAppDir = TestUtils.copyProjectToTempLocation(GRADLE_SIMPLE_APP_LOCATION, GRADLE_SIMPLE_APP_NAME);
        MigrationBundle bundle = new MigrationBundle(new File(MODERATE_BUNDLE_LOCATION));
        try {
            expandedBundle = bundle.validateAndUnzip();
        } catch (InvalidBundleException ibe) {
            ibe.printStackTrace();
        }
    }

	@AfterEach
	void tearDown() {
        try {
			FileUtils.deleteDirectory(new File(mavenAppDir).getParentFile());
            FileUtils.deleteDirectory(new File(gradleAppDir).getParentFile());
        } catch (IOException e) {
            e.printStackTrace();
        }
        expandedBundle.cleanUpExpandedBundle();
	}

    @Test
    /**
     * Test BuildTool type detection
     */
    public void BuildToolTypeTest() {

        BuildToolType buildSysType = JamUtils.getBuildToolType(mavenAppDir);
        assertEquals(buildSysType, BuildToolType.MAVEN);

        buildSysType = JamUtils.getBuildToolType(gradleAppDir);
        assertEquals(buildSysType, BuildToolType.GRADLE);
    }


    // TODO: implement more tests
}
