// (C) Copyright IBM Corp. 2024
package com.ibm.ta.jam;

import static com.ibm.ta.jam.build.BuildToolFactory.BuildToolType.UNKNOWN;

import java.io.File;
import java.util.List;

import org.tinylog.Logger;
import org.tinylog.configuration.Configuration;

import com.ibm.ta.jam.app.Application;
import com.ibm.ta.jam.app.ApplicationFactory;
import com.ibm.ta.jam.app.ApplicationFactory.ApplicationType;
import com.ibm.ta.jam.build.BuildTool;
import com.ibm.ta.jam.build.BuildToolFactory;
import com.ibm.ta.jam.build.BuildToolFactory.BuildToolType;
import com.ibm.ta.jam.bundle.ExpandedBundle;
import com.ibm.ta.jam.bundle.InvalidBundleException;
import com.ibm.ta.jam.bundle.MigrationBundle;
import com.ibm.ta.jam.recipe.RecipeAutomation;
import com.ibm.ta.jam.recipe.RecipeAutomationFactory;
import com.ibm.ta.jam.recipe.RecipeAutomationInitializationException;
import com.ibm.ta.jam.utils.JamUtils;
import com.ibm.ta.jam.utils.IOUtils;
import com.ibm.ta.jam.utils.MavenCoords;

//
// TODO API to remove WAS dependencies from build config?
//

/**
 * Facade for interacting with the auto migration
 */
public class Jam {

    private String applicationDir;
    private String migrationBundleZip;

    private BuildTool buildTool;
    private ExpandedBundle expandedBundle;
    private Application application;
    private RecipeAutomation recipeAutomation;


    // TODO: make the migratino bundle optional - will need to make sure no methods crash and burn because of that
    public Jam (String applicationDir, String migrationBundleZip, boolean debugMode) 
            throws InvalidApplicationDirectoryException, InvalidMigrationBundleException, InitializationException{
        
        if (!IOUtils.isValidDirectory (applicationDir)) {
            throw new InvalidApplicationDirectoryException("Application directory is not valid");
        }

        if (!IOUtils.isValidFlie (migrationBundleZip)) {
            throw new InvalidMigrationBundleException("Migration bundle is not valid");
        }

        this.applicationDir = applicationDir;
        this.migrationBundleZip = migrationBundleZip;


        if (!initialize(debugMode)) {
            throw new InitializationException("Initialization of migration automation failed");
        }
    }

    //
    // Plugins
    //
    public boolean hasPlugin(String pluginArtifactId) {
        return false;
    }

    // Note: this needs to be added before the dependencies are added (at least in the case of Maven) because the dependencies need to be added to copyDependencies.
    public boolean addLibertyDevPlugin() {
        return buildTool.addLibertyDevPlugin();
    }

    
    //
    // Builds
    //
    public boolean runBuild(List<String> buildArgs) {
        return false;
    }

    public boolean runBasicBuild() {
        return runBasicBuild(/* skipTests */ true);
    }

    public boolean runBasicBuild(boolean skipTests) {
        return buildTool.runBasicBuild(skipTests);
    }

    public boolean runLibertyDevMode() {
        return buildTool.runLibertyDevMode();
    }


    //
    // Application
    //
    public boolean addLibertyServerConfigToApplication() {
        return application.addServerConfigFromBundle(expandedBundle.getServerXmlPath());
    }

    public boolean addLocalDependenciesToApplication(List<String> dependencyPaths) {
        // Update the build tool config
        if (buildTool.addLocalDependencies(dependencyPaths)) {
            // Update the application itself with the local libs
            return application.addLocalDependencies(dependencyPaths);
        } else {
            Logger.error("Failed to add local dependencies to build tool configuration");
            return false;
        }
    }

    public boolean addPlaceholderDependenciesToApplication(List<String> deps) {
        return false;//application.addPlaceholderDependencies(deps);
    }

    //
    // Dependencies, jar files that the application depends on
    //
    public List<String> getAllLibDependencies() {
        return expandedBundle.getLibDependencyNames();
    }

    public boolean addRemoteDependenciesToApplication(List<MavenCoords> dependencyCoords) {
        // Update the build tool config
        return buildTool.addRemoteDependencies(dependencyCoords);
    }


    //
    // Recipes
    //
    public List<String> getAllAvailableRecipes() {
        return recipeAutomation.getAllRecipes();
    }

    public boolean runRecipes(List<String> recipes) {
        return recipeAutomation.runRecipes(recipes);
    }

    public boolean runAllRecipes() {
        return recipeAutomation.runAllRecipes();
    }


    //
    // Logging
    //

    /**
     * Tinylog configuration. Must be done before first log out.
     * 
     * @param debugMode
     */
    public void configureLogging(boolean debugMode) {
        Configuration.set("writer", "console");
        // Configuration.set("writer.format", "{date} [{thread}] {class}.{method}() {level}: {message}");
        Configuration.set("writer.format", "{level}: {message}");
        Configuration.set("writer.level", "info");
        if (debugMode) {
            Configuration.set("writer.level", "debug");
            Logger.info("DEBUG mode is ON");
        }
    }

    /**
     * Initialize the auto migration objects. 
     * This will involve understanding the build tool, unzipping the bundle, and creating an application object.
     * 
     * @param debugMode
     * @return a boolean indicating if initialization succeeded.
     */
    private boolean initialize (boolean debugMode) {

        configureLogging(debugMode);

        //
        // Determine the type of project (maven/gradle), bail if its unknown
        //
        BuildToolType buildSysType = JamUtils.getBuildToolType(applicationDir);
        if (buildSysType == UNKNOWN) {
            Logger.error("Unable to determine the build tool type. Exiting.");
            return false;
        }

        //
        // Create the BuildTool object
        //
        buildTool = BuildToolFactory.getBuildTool(applicationDir, buildSysType);
        if (!buildTool.backupBuildConfig()) {
            // TODO: consider if this should be error here and return
            Logger.warn("Unable to backup the build configuration");
        }

        //
        // Validate and unzip the bundle
        //
        MigrationBundle bundle = new MigrationBundle(new File(migrationBundleZip));
        try {
            expandedBundle = bundle.validateAndUnzip();
        } catch (InvalidBundleException ibe) {
            Logger.error("Migration bundle is invalid. Exiting.");
            return false;
        }

        //
        // Determine the type of application (maven/gradle), bail if its unknown
        //
        ApplicationType appType = buildTool.getPackagingType();
        if (appType == ApplicationType.UNKNOWN) {
            Logger.error("Unable to determine the application type. Exiting.");
            return false;
        }

        //
        // Create Application object
        //
        application = ApplicationFactory.getApplication(applicationDir, appType);


        //
        // Create recipe automation object
        //
        try {
            recipeAutomation = RecipeAutomationFactory.getRecipeAutomation(expandedBundle, buildTool);
        } catch (RecipeAutomationInitializationException re) {
            Logger.error("RecipeAutomation could not be initialized.");
            return false;
        }    

        return true;
    }
    
    /**
     * Call when finished with Jam to remove temporary files
     */
    public void close () {
        if (expandedBundle != null) {
            expandedBundle.cleanUpExpandedBundle();
        }
    }
}
