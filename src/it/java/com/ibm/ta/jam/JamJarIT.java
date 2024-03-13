// (C) Copyright IBM Corp. 2024
package com.ibm.ta.jam;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.ibm.ta.jam.bundle.ExpandedBundle;

public class JamJarIT {

    private final String MAVEN_MODERATE_APP_LOCATION = "src/test/resources/moderateApp/source";
    private final String MAVEN_MODERATE_APP_NAME = "modresorts";
    private final String MODERATE_BUNDLE_LOCATION = "src/test/resources/moderateApp/bundle/modresorts.ear_migrationBundle.zip";
    
    private String applicationDir;
    private ExpandedBundle expandedBundle;

    @BeforeEach
	void setUp() {
    }

	@AfterEach
	void tearDown() {
	}

    @Test
    @Tag("integration")
    /**
     * Verifies exit code of 2 when no app dir or migration bundle supplied to cli
     */
    public void JamJarCliTest_1() {
        ProcessBuilder builder = new ProcessBuilder();

        builder.command(
            "java",
            "-jar",
            "target/ta-jam-0.0.1-SNAPSHOT.jar"
        );

        builder.directory(new File("./"));

        Process process = null;
        try {
            process = builder.start();
            int exitCode = process.waitFor();
            assertTrue(exitCode == 2);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (process != null) {
                process.destroy();
            }
        }
    }
}
