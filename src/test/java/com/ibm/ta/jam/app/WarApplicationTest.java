// (C) Copyright IBM Corp. 2024
package com.ibm.ta.jam.app;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.ibm.ta.jam.app.ApplicationFactory.ApplicationType;
import com.ibm.ta.jam.bundle.ExpandedBundle;
import com.ibm.ta.jam.bundle.InvalidBundleException;
import com.ibm.ta.jam.bundle.MigrationBundle;
import com.ibm.ta.jam.utils.TestUtils;

public class WarApplicationTest {

    private final String MAVEN_MODERATE_APP_LOCATION = "src/test/resources/moderateApp/source";
    private final String MAVEN_MODERATE_APP_NAME = "modresorts";
    private final String MODERATE_BUNDLE_LOCATION = "src/test/resources/moderateApp/bundle/modresorts.ear_migrationBundle.zip";
    
    private String applicationDir;
    private ExpandedBundle expandedBundle;

    @BeforeEach
	void setUp() {
        applicationDir = TestUtils.copyProjectToTempLocation(MAVEN_MODERATE_APP_LOCATION, MAVEN_MODERATE_APP_NAME);
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
			FileUtils.deleteDirectory(new File(applicationDir).getParentFile());
        } catch (IOException e) {
            e.printStackTrace();
        }
        expandedBundle.cleanUpExpandedBundle();
	}

    @Test
    public void addServerConfigFromBundleTest_1() {
        Application application = ApplicationFactory.getApplication(applicationDir, ApplicationType.WAR);

        application.addServerConfigFromBundle(expandedBundle.getServerXmlPath());

        boolean serverXmlAdded = new File(applicationDir + File.separator + Application.RELATIVE_SERVER_XML_LOCATION).exists();

        assertTrue(serverXmlAdded);
    }
}
