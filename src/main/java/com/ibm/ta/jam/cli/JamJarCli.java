// (C) Copyright IBM Corp. 2024
package com.ibm.ta.jam.cli;

import java.util.Arrays;

import static com.ibm.ta.jam.utils.AnsiConsts.ANSI_BLUE;
import static com.ibm.ta.jam.utils.AnsiConsts.ANSI_GREEN;
import static com.ibm.ta.jam.utils.AnsiConsts.ANSI_RESET;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import org.tinylog.Logger;

import com.ibm.ta.jam.Jam;
import com.ibm.ta.jam.InitializationException;
import com.ibm.ta.jam.InvalidApplicationDirectoryException;
import com.ibm.ta.jam.InvalidMigrationBundleException;
import com.ibm.ta.jam.utils.MavenCoords;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

/**
 * CLI to demonstrate the Jam API
 */
@Command(name = "jam-jar", version = "alpha", description = "Automates migration of a Maven or Gradle application to Liberty", header = {
    "@|blue ___ ___ __  __      _  _   __  __      _          |@",
    "@|blue |_ _| _ )  \\/  |  _ | |/_\\ |  \\/  |  _ | |__ _ _ _ |@",
    "@|blue  | || _ \\ |\\/| | | || / _ \\| |\\/| | | || / _` | '_||@",
    "@|blue |___|___/_|  |_|  \\__/_/ \\_\\_|  |_|  \\__/\\__,_|_|  |@",
    "@|blue |@"})
public class JamJarCli implements Runnable {

    @Option(names = {"-a", "--application-directory"}, paramLabel = "APPLICATION DIRECTORY", required = true, echo = true, description = "Location of application to migrate")
    private String appDir;

    @Option(names = {"-m", "--migration-bundle"}, paramLabel = "MIGRATION BUNDLE", required = true, echo = true, description = "Location of migration bundle from Transformation Advisor")
    private String migrationBundle;

    @Option(names = {"-d", "--debug"}, defaultValue = "false", description = "Include debug information in the output")
    private static boolean debugMode;

    @Option(names = { "-h", "--help", "-?", "-help"}, usageHelp = true, description = "Display this help and exit")
    private boolean helpRequested;

    @Option(names = { "-v", "--version"}, versionHelp = true, description = "Display the version")
    private boolean versionRequested;

    @Override
    public void run() {

        //
        // Create Jam instance - checking for errors in the inputs
        //
        Jam jam;
        try {
            jam = new Jam(appDir, migrationBundle, debugMode);
        } catch (InvalidApplicationDirectoryException ade) {
            Logger.error("Application directory specified is not valid");
            return;
        } catch (InvalidMigrationBundleException mbe) {
            Logger.error("Migration bundle specified is not valid");
            return;
        } catch (InitializationException ie) {
            Logger.error("Error occurred when initializing the auto migration. Enable debug mode and check logs");
            return;
        }

        int option = getUserOption();
        while (option != 0) {

            if (option == 1 || option == 7) {
                //
                // Run a basic build to ensure all is OK before we make any changes
                //
                Logger.info("Running a basic build...");
                boolean skipTests = false;
                String skipTestsResp = promptUser("Skip tests when running build? [Y|N]", "Y");
                if (skipTestsResp.toUpperCase().equals("Y")) {
                    skipTests = true;
                }
                boolean buildSuccess = jam.runBasicBuild(skipTests);
                if (buildSuccess) {
                    Logger.info("Basic build succeeded");
                } else {
                    Logger.error("Basic build failed. Fix build before proceeding.");
                }
            }

            
            if (option == 2 || option == 7) {
                //
                // Add server.xml config
                //
                Logger.info("Adding server.xml from migration bundle...");
                boolean addServerXmlOK = jam.addLibertyServerConfigToApplication();
                if (addServerXmlOK) {
                    Logger.info("server.xml successfully added to application");
                }
            }

            if (option == 3 || option == 7) {
                //
                // Add dependencies to application
                //
                Logger.info("Checking for required dependencies");
                List<MavenCoords> remoteDependencies = new ArrayList<>();
                List<String> localDependencies = new ArrayList<>();
                List<String> placeholderDependencies = new ArrayList<>();
                List<String> libDependencies = jam.getAllLibDependencies();
                for (String dep : libDependencies) {
                    System.out.print("Found dependency: ");
                    System.out.print(ANSI_GREEN);
                    System.out.println(dep);
                    System.out.print(ANSI_RESET);
                    String locationOrCoords = promptUser(
                        "Specify maven coordinates OR a path to the local file OR skip\n" +
                        "Skipping this step may result in application not starting.\n" +
                        "[M(Maven coordinates) | L(Local path) | S(Skip)]",
                        "L");
                    if (locationOrCoords.equals("S")) {
                        Logger.warn("Skipping adding dependency " + dep + ". Application may fail to start or function");
                        placeholderDependencies.add(dep);
                    } else if (locationOrCoords.equals("M")) {
                        String artifactId = promptUser("artifactId: ", "");
                        String groupId = promptUser("groupId: ", "");
                        String version = promptUser("version: ", "");

                        MavenCoords coords = new MavenCoords(artifactId, groupId, version);
                        remoteDependencies.add(coords);
                    } else if (locationOrCoords.equals("L")) {
                        String location = promptUser("Enter location of " + dep + ": ", "");
                        localDependencies.add(location);
                    }
                }
                if (remoteDependencies.size() > 0) {
                    if (!jam.addRemoteDependenciesToApplication(remoteDependencies)) {
                        Logger.error("Failed to add remote dependencies to application");
                    } else {
                        Logger.info("Sucessfully added remote dependencies to application");
                    }
                }
                if (localDependencies.size() > 0) {
                    if (!jam.addLocalDependenciesToApplication(localDependencies)) {
                        Logger.error("Failed to add local dependencies to application");
                    } else {
                        Logger.info("Sucessfully added local dependencies to application");
                    }
                }
                if (placeholderDependencies.size() > 0) {
                    if (!jam.addPlaceholderDependenciesToApplication(localDependencies)) {
                        Logger.error("Failed to add placeholders for dependencies to application");
                    } else {
                        Logger.info("Sucessfully added placeholder dependencies to application");
                    }
                }
            }
            
            if (option == 4 || option == 7) {
                //
                // Run recipes
                //
                Logger.info("Running recipes to fix code issues");

                boolean recipeSuccess = false;
                List<String> recipes = jam.getAllAvailableRecipes();
                System.out.println("The following recipes are available to fix migration issues: ");
                System.out.print(ANSI_GREEN);
                for (String recipe : recipes) {
                    System.out.println(recipe);
                }
                System.out.print(ANSI_RESET);
                String resp = promptUser("Run all recipes, or select a recipe: [A(All) | S(Select)]", "A");
                if (resp.toUpperCase().equals("A")) {
                    recipeSuccess = jam.runAllRecipes();
                } else {
                    String recipe = promptUser("Enter recipe to run: ", "");
                    List<String> userRecipes = Arrays.asList(recipe);
                    recipeSuccess = jam.runRecipes(userRecipes);
                }

                if (recipeSuccess) {
                    Logger.info("Recipe running has finished successfully.");
                } else {
                    Logger.error("There was an error running recipes. Review logs.");
                }
            }
            
            if (option == 5 || option == 7) {
                //
                // Add liberty dev plugin
                //
                Logger.info("Adding liberty dev plugin");
                boolean libPluginAdded = jam.addLibertyDevPlugin();
                if (libPluginAdded) {
                    Logger.info("Liberty Dev plugin added");
                }
            }

            if (option == 6 || option == 17) {
                //
                // Run liberty dev mode
                // Note: if run here will have to quit in order to proceed and run the recipes. OR run in bg, but don't get the term.
                //
                Logger.info("Running Liberty Dev mode");
                boolean devModeSucceeded = jam.runLibertyDevMode();
                if (devModeSucceeded) {
                    Logger.info("Liberty Dev mode finished");
                }
            }

            option = getUserOption();

        }

        //
        // Clean up
        //
        Logger.info("Cleaning up");
        jam.close();

        Logger.info("Exiting");
    }
    
    private String promptUser(String prompt, String defaultValue) {
		String fullPrompt = prompt;
        boolean defaultGiven = defaultValue != null && !defaultValue.isEmpty() && !defaultValue.isEmpty();
        if (defaultGiven) {
            fullPrompt += " (" + defaultValue + "): ";
        }

        System.out.print(ANSI_BLUE);
		System.out.print(fullPrompt);
        System.out.print(ANSI_RESET);
		Scanner scanner = new Scanner(System.in);
		String response = scanner.nextLine();

        if (defaultGiven && (response.isBlank() || response.isEmpty())) {
            response = defaultValue;
        }

		return response;
	}

    private int getUserOption() {
        System.out.println(ANSI_BLUE);
        System.out.println("");
        System.out.println("********************************************************************************");
        System.out.println("Choose an option:");
        System.out.println("1 - Run a build");
        System.out.println("2 - Add server.xml from migration bundle");
        System.out.println("3 - Add dependencies");
        System.out.println("4 - Run recipes");
        System.out.println("5 - Add Liberty Dev plugin");
        System.out.println("6 - Run Liberty Dev mode");
        System.out.println("7 - Run All Actions");
        System.out.println("Q - Quit");
        System.out.println(ANSI_RESET);

        Scanner scanner = new Scanner(System.in);
        String response = scanner.nextLine();
        if (response.isBlank() || response.isEmpty()) {
            return getUserOption();
        } else {
            if (response.toUpperCase().equals("Q")) {
                return 0;
            } else {
                try {
                    int respInt = Integer.parseInt(response);
                    if (respInt < 1 || respInt > 8) {
                        return getUserOption();
                    }
                    return respInt;
                } catch (NumberFormatException nfe) {
                    return getUserOption();
                }
            }
        }
    }

    public static void main(String[] args) {
        int exitCode = new CommandLine(new JamJarCli()).execute(args); 
        System.exit(exitCode);
    }
}
