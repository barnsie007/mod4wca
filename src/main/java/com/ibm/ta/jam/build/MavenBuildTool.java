// (C) Copyright IBM Corp. 2024
package com.ibm.ta.jam.build;

import static com.ibm.ta.jam.utils.AnsiConsts.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.maven.model.Build;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.Resource;

import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;
import org.apache.maven.shared.invoker.DefaultInvocationRequest;
import org.apache.maven.shared.invoker.DefaultInvoker;
import org.apache.maven.shared.invoker.InvocationRequest;
import org.apache.maven.shared.invoker.InvocationResult;
import org.apache.maven.shared.invoker.Invoker;
import org.apache.maven.shared.invoker.MavenInvocationException;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.codehaus.plexus.util.xml.Xpp3DomBuilder;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.tinylog.Logger;
import org.yaml.snakeyaml.Yaml;

import com.ibm.ta.jam.app.Application;
import com.ibm.ta.jam.app.ApplicationFactory.ApplicationType;
import com.ibm.ta.jam.build.BuildToolFactory.BuildToolType;
import com.ibm.ta.jam.plugin.MavenLibertyPlugin;
import com.ibm.ta.jam.plugin.RewritePlugin;
import com.ibm.ta.jam.recipe.RecipeAutomation;
import com.ibm.ta.jam.utils.MavenCoords;

/**
 * Model of the Maven build tool for the Java application. This class handles all interactions 
 * with the pom.xml
 */
public class MavenBuildTool implements BuildTool {

	private final String MVN_OPEN_REWRITE_ARG = "%s:%s:%s:run";
	private final String MVN_RECIPE_ARTIFACT_COORDS_ARG = "-Drewrite.recipeArtifactCoordinates=%s:%s:%s";
	private final String MVN_RECIPE_NAME_ARG = "-Drewrite.activeRecipes=%s";
    private final String POM_FILE = "pom.xml";
    private final String WAR_PACKAGING = "war";
    private final String EAR_PACKAGING = "ear";

	/**
	 * Root directory of the applicatino this BuildTool is a part of
	 */
    private String applicationDir;

	/**
	 * Full path to pom.xml file
	 */
	private String pomXmlFile;

	/**
	 * Maven invoker to implement maven actions
	 */
    private final Invoker invoker;

	/**
	 * Create a MavenBuildTool object. Discovers the Maven Home location and create the Maven Invoker.
	 * @param applicationDir
	 */
    public MavenBuildTool(String applicationDir) {
		Logger.debug("Start create MavenBuildTool instance");
		this.applicationDir = applicationDir;
		pomXmlFile = applicationDir + File.separator + POM_FILE;

		String mavenHome = getMavenHome();
		if (mavenHome == null) {
			Logger.error("Could not find maven home location. Tried system property, mvn --version, maven.home, environment variables MAVEN_HOME and M2_HOME");
			throw new RuntimeException("Could not find maven home");
		}

		Invoker invokerInstance = new DefaultInvoker();
		invokerInstance.setMavenHome(new File(mavenHome));
		this.invoker = invokerInstance;
		Logger.debug("Finish create MavenValidator instance");
    }

	/**
	 * Run a maven build with given arguments
	 * @param buildArgs the build arguments to run, e.g. "clean build"
	 * @return a boolean indicating success or failure of the build
	 */
    @Override
	public boolean runBuild(final List<String> buildArgs) {
		Logger.debug("Start runBuild for " + applicationDir + " with build args: " + buildArgs);
		InvocationRequest request = new DefaultInvocationRequest();
        request.setPomFile(new File(applicationDir));
        request.setGoals(buildArgs);
		// request.setInputStream(System.in); // Unable to get this working. using ProcessBuilder instead where input to maven is required, e.g. liberty:dev
	
        InvocationResult result;
		try {
			System.out.println(ANSI_BLUE);
			result = invoker.execute(request);
			System.out.println(ANSI_RESET);
			Logger.debug("Finish runBuild for " + applicationDir);
			return result.getExitCode() == 0;
		} catch (MavenInvocationException e) {
			Logger.error("Exception encountered when running build");
			if (Logger.isDebugEnabled()) {
                e.printStackTrace();
            }
			return false;
		}
	}

	/**
	 * Runs a basic maven build ("clean package"). Skips tests by default.
	 * @return a boolean indicating success or failure of the build
	 */
	public boolean runBasicBuild() {
      	return runBasicBuild(true);
    }

	/**
	 * Runs a basic maven build ("clean package").
	 * @param skipTests boolean indicating if the test should be skipped or not
	 * @return a boolean indicating success or failure of the build
	 */
	public boolean runBasicBuild(boolean skipTests) {
        List<String> args = Arrays.asList("clean", "package");
		if (skipTests) {
			args = Arrays.asList("clean", "package", "-DskipTests");
		}
      	return runBuild(args);
    }
	
	/**
	 * Runs the liberty dev mode. Will first check that the Liberty plugin has been added to the application
	 * @return a boolean indicating if the Liberty Dev mode ran sucessfully.
	 */
	public boolean runLibertyDevMode() {
		if (!hasLibertyDevPlugin()) {
			Logger.error("Liberty Dev plugin is not installed, so Liberty Dev mode cannot be run.");
			return false;
		}
		List<String> args = Arrays.asList("liberty:dev");
        return runBuildAsProcess(applicationDir, args);
    }

	/**
	 * Return the current build tool type
	 * @return a BuildToolType indicating a maven build tool
	 */
	@Override
	public BuildToolType getBuildToolType() {
		return BuildToolType.MAVEN;
	}

	/**
	 * Gets the full path to the pom.xml
	 * @return a String representing the full path to the pom.xml file
	 */
	@Override
	public String getBuildToolConfiguration() {
		return pomXmlFile;
	}
	
	/**
	 * Get the packaging type described in the build tool. WAR and EAR are valid for our purposes.
	 * @return ApplicaitonType representing the packaging type for the application that this build tool builds. Returns UNKNOWN if not a recognised type.
	 * 
	 */
    @Override
    public ApplicationType getPackagingType() {
        try {
            Model model = getPomModel();
            String packaging = model.getPackaging();
            if (WAR_PACKAGING.equals(packaging)) {
                return ApplicationType.WAR;
            } else if (EAR_PACKAGING.equals(packaging)) {
                return ApplicationType.EAR;
            } else {
                return ApplicationType.UNKNOWN;
            }
        } catch (IOException e) {
            Logger.error("IOException when getting packaging type");
            if (Logger.isDebugEnabled()) {
                e.printStackTrace();
            }
            return ApplicationType.UNKNOWN;
        } catch (XmlPullParserException xe) {
            Logger.error("XmlPullParserException when getting packaging type");
            if (Logger.isDebugEnabled()) {
                xe.printStackTrace();
            }
            return ApplicationType.UNKNOWN;
        }
    }

	/**
	 * Backup the build configuration (pom.xml)
	 * @return a boolean indicating if the backup was successful or not
	 */
    @Override
	public boolean backupBuildConfig() {
		try {	
			String backupFile = backupFile(applicationDir + File.separator + POM_FILE);
			Logger.debug("Backed up file to: " + backupFile);
			return true;
		} catch (IOException ioe) {
			Logger.error("IO exception when backing up the build config (pom.xml)");
			if (Logger.isDebugEnabled()) {
				ioe.printStackTrace();
			}
			return false;
		}
	}

	/**
	 * Backup a file using a numbered backup scheme (...bak.1 ...bak.2 etc)
	 * @param file path of file to be backed up
	 * @return the path to the backup
	 * @throws IOException when backup fails
	 */
	private String backupFile(String file) throws IOException {
		Logger.debug("Start make backup of: " + file);

		int backupNumber = 1;
		String backupFileName = file + ".bak." + backupNumber;
		File backupFile = new File(backupFileName);
		while (backupFile.exists()) {
			backupNumber++;
			backupFileName = file + ".bak." + backupNumber;
			backupFile = new File(backupFileName);
		}

		FileUtils.copyFile(new File(file), backupFile);
		Logger.debug("Finish make backup of: " + file);
		return backupFileName;
	}


	/**
	 * Returns the POM Model for the pom file {@link org.apache.maven.model.Model}
	 * @return org.apache.maven.model.Model for the pom.xml
	 * @throws IOException if fails to read the pom.xml
	 * @throws XmlPullParserException if fails to parse the pom.xml
	 */
    private Model getPomModel() throws IOException, XmlPullParserException{
		FileReader reader = new FileReader(pomXmlFile);
		MavenXpp3Reader mavenreader = new MavenXpp3Reader();
		Model model = mavenreader.read(reader);
		return model;
	}

    /**
	 * Try a number of fallbacks to get maven home. Try executing mvn in external process as first option as 
	 * its likely to be most reliable. Fallback to system properties and environment variables.
	 * @return
	 */
	private String getMavenHome() {
		// Try a number of fallback options if the maven home property is not given
		String mvnHomeFromExe = getMavenHomeFromMvnExecution();
		if (mvnHomeFromExe != null && !mvnHomeFromExe.isBlank()) {
			Logger.info("Using 'mvn --version' for maven home location: " + mvnHomeFromExe);
			return mvnHomeFromExe;
		}

		String mavenHomeProp = System.getProperty("maven.home");
		if (mavenHomeProp != null && !mavenHomeProp.isBlank()) {
			Logger.info("Using system property maven.home for maven home location: " + mavenHomeProp);
			return mavenHomeProp;
		} 

		String envVarMvn = System.getenv("MAVEN_HOME");
		if (envVarMvn != null && !envVarMvn.isBlank()) {
			Logger.info("Using environment variable MAVEN_HOME for maven home location: " + envVarMvn);
			return envVarMvn;
		} 

		String envVarMvn2 = System.getenv("M2_HOME");
		if (envVarMvn2 != null && !envVarMvn2.isBlank()) {
			Logger.info("Using environment variable M2_HOME for maven home location: " + envVarMvn2);
			return envVarMvn2;
		}
		return null;
	}

	/**
	 * Run a maven build as an external process using ProcessBuilder. Using this approach for builds that require user input.
	 * For example, the Liberty Dev mode requires user to enter 'q' to quit. Getting user input redirected to the
	 * Maven Invoker process was problematic. It is trivial to to redirect user input with the ProcessBuilder (using inheritIO method).
	 * @param applicationDir the root application directory
	 * @param buildArgs the arguments to pass to mvn
	 * @return a boolean indicating success or failure of the build
	 */
	private boolean runBuildAsProcess(String applicationDir, List<String> buildArgs) {
		Logger.debug("Start maven build as a process");
		boolean isWindows = System.getProperty("os.name").toLowerCase().startsWith("windows");

		String argsAsString = String.join(" ", buildArgs);

        ProcessBuilder builder = new ProcessBuilder().inheritIO();
        if (isWindows) {
            builder.command("cmd.exe", "/c", "cd " + applicationDir + "& mvn " + argsAsString);
        } else {
            builder.command("sh", "-c", "cd " + applicationDir + "; mvn " + argsAsString);
        }

        try {
            Process process = builder.start();
			process.waitFor();
        } catch (Exception e) {
			Logger.warn("Exception was encountered when running maven build with args " + argsAsString);
			if (Logger.isDebugEnabled()) {
				e.printStackTrace();
			}
			return false;
		}
		Logger.debug("Finish run maven build as a process");
		return true;
	}

	/**
	 * Execute mvn --version in the system an get the maven home value from the output
	 * 
	 * @return A string representing the maven home location, or null if a problem is encountered
	 */
	private String getMavenHomeFromMvnExecution() {
		Logger.debug("Start get maven home value from the mvn execution");
		String mavenHome = null;
		boolean isWindows = System.getProperty("os.name").toLowerCase().startsWith("windows");

        ProcessBuilder builder = new ProcessBuilder();
        if (isWindows) {
            builder.command("cmd.exe", "/c", "mvn -version");
        } else {
            builder.command("sh", "-c", "mvn --version");
        }

        try {
            Process process = builder.start();
            BufferedReader br = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = br.readLine()) != null) {
                if (line.contains("Maven home:")) {
                    String[] parts = line.trim().split("\\s*:\\s*");
                    mavenHome = parts[1];
                }
            }
        } catch (Exception e) {
			Logger.warn("Exception was encountered when trying to get maven home from mvn --version");
			if (Logger.isDebugEnabled()) {
                e.printStackTrace();
            }
		}

		Logger.debug("Start get maven home value from the mvn execution");
		return mavenHome;
	}

	/**
	 * Check if the Liberty Dev plugin is added to the build tool
	 * @return a boolean indicating if the Liberty Dev plugin is already added
	 */
    public boolean hasLibertyDevPlugin() {
		try {
            Model model = getPomModel();
		    List<Plugin> plugins = model.getBuild().getPlugins();
		    for (Plugin plugin : plugins) {
			    if (MavenLibertyPlugin.LIBERTY_MAVEN_PLUGIN_ARTIFACT_ID.equals(plugin.getArtifactId())) {
				    return true;
			    }
		    }
        } catch (IOException ioe) {
            Logger.error("IOException when creating pom model");
            if (Logger.isDebugEnabled()) {
                ioe.printStackTrace();
            }
        } catch (XmlPullParserException xe) {
            Logger.error("XmlPullParserException when creating pom model");
            if (Logger.isDebugEnabled()) {
                xe.printStackTrace();
            }
        }
		return false;
	}

	/**
	 * Add the Liberty Dev plugin to build tool. Check first to see if its already added and does not re-add.
	 * @return a boolean indicating if plugin was sucessfully added.
	 */
    public boolean addLibertyDevPlugin() {
		Logger.debug("Start add liberty dev plugin to pom.xml");

        if (!hasLibertyDevPlugin()) {
            Plugin libertyDevPlugin = new Plugin();
            libertyDevPlugin.setGroupId(MavenLibertyPlugin.LIBERTY_MAVEN_PLUGIN_GROUP_ID);
            libertyDevPlugin.setArtifactId(MavenLibertyPlugin.LIBERTY_MAVEN_PLUGIN_ARTIFACT_ID);
            libertyDevPlugin.setVersion(MavenLibertyPlugin.LIBERTY_MAVEN_PLUGIN_VERSION);
            
            if (!addPluginToPomXml(libertyDevPlugin)) {
                return false;
            }

        } else {
            Logger.info("pom.xml already has liberty dev plugin");
        }

		Logger.debug("Finish add liberty dev plugin to pom.xml");
		return true;
    }


    /**
     * Add a build resource to the pom.xml. This resource lcation is where lib dependencies are placed.
	 * Dependency jar files must be copied to the correct location {@link com.ibm.ta.jam.app.Application.addLocalDependencies()}
     * @return a boolean indicaing if resource was sucessfully added or not.
     */
    public boolean addLibResourceToPomXml() {
		Logger.debug("Start adding resource to pom.xml");
        try {
            Resource res = new Resource();
            String dir = "${project.basedir}" + File.separator + Application.RELATIVE_LIB_DIR;
            String target = "${project.build.directory}" + File.separator + Application.RELATIVE_TARGET_LIB_DIR;
            res.setDirectory(dir);
            res.setTargetPath(target);

            Model model = getPomModel();
			Build build = model.getBuild();

			// TODO: Move this check to its own function and into the BuildTool and Jam APIs
			List<Resource> existingResources = model.getBuild().getResources();
			for (Resource existingRes : existingResources) {
				if (existingRes.getDirectory() != null && existingRes.getDirectory().equals(dir)) {
					if (existingRes.getTargetPath() != null && existingRes.getTargetPath().equals(target)) {
						Logger.info("Resource with directory and targetPath already exists in pom.xml.");
						return true;
					} else {
						Logger.error("Resource with directory already exists in pom.xml, however, its targetPath is different to new targetPath of: " + target);
						Logger.error("Unable to add resource as it conflicts with existing resource entry");
						return false;
					}
				}
			}
            
            build.addResource(res);

            MavenXpp3Writer writer = new MavenXpp3Writer();
            writer.write(new FileOutputStream(new File(pomXmlFile)), model);

			Logger.debug("Finish adding resource to pom.xml");
            return true;

		} catch (IOException ioe)  {
			Logger.debug("IO exception when adding library resource to pom.xml");
			if (Logger.isDebugEnabled()) {
                ioe.printStackTrace();
            }
			return false;
		} catch (XmlPullParserException xe) {
			Logger.debug("XML parsing exception when  adding library resource to pom.xml");
			if (Logger.isDebugEnabled()) {
                xe.printStackTrace();
            }
			return false;
		}
    }
    
    /**
	 * Add support for local dependencies to the pom.xml
     * @param dependencyPaths Not used right now in Maven as we just add a resource with a location of the jars to the pom.xml
     */
    public boolean addLocalDependencies(List<String> dependencyPaths) {
        return addLibResourceToPomXml();
    }

	/**
	 * Add remote dependencies to pom.xml. This methos also calls {@link addLibertyPluginCopyDependency} to ensure that the 
	 * dependencies are available in Liberty Dev Mode
	 * @param dependencyCoords list of dependency maven coordinates to add as dependencies
	 * @return boolean indicating if the adding of dependencies was successful or not.
	 */
    public boolean addRemoteDependencies(List<MavenCoords> dependencyCoords){
        Logger.debug("Start adding remote dependencies to pom.xml");
        try {
            Model model = getPomModel();
            for (MavenCoords coords : dependencyCoords) {
                Dependency dep = new Dependency();
			    dep.setArtifactId(coords.getArtifactId());
			    dep.setGroupId(coords.getGroupId());
			    dep.setVersion(coords.getVersion());		
			    model.getDependencies().add(dep);
            }

			MavenXpp3Writer writer = new MavenXpp3Writer();
			writer.write(new FileOutputStream(new File(pomXmlFile)), model);

        } catch (IOException ioe)  {
			Logger.debug("IO exception when adding remte dependencies");
			if (Logger.isDebugEnabled()) {
                ioe.printStackTrace();
            }
			return false;
		} catch (XmlPullParserException xe) {
			Logger.debug("XML parsing exception when adding remote dependencies");
			if (Logger.isDebugEnabled()) {
                xe.printStackTrace();
            }
			return false;
		}

		// Also add dependencies to copDependencies for liberty plugin
		return addLibertyPluginCopyDependency(dependencyCoords);
    }

	// TODO this copyDependencies method shouldnt be here. the buildtool should NOT know the inner details of the plugin. this logic belongs in the plugin.
	/**
	 * copyDependencies are added to the liberty plugin configuration in the following way. 
	 * 
	 * {@snippet :
	  <configuration>
	      <copyDependencies>
	  	       <dependencyGroup>
	 			   <location></location>
	 			   <stripVersion></stripVersion>
	 			   <dependency>  
                    ...
                  </dependency>
	 			   ...
	 		   </dependencyGroup>
	      </copyDependencies>
	  </configuration>
	 } 
	 * This method must check to see if the dependemcy is already added before adding it.
	 * @param dependencyCoords List of maven coordinates to add as copyDependencies
	 * @return a boolean indicating of the adding of copy Dependencies was successful or not.
	 */
	public boolean addLibertyPluginCopyDependency(List<MavenCoords> dependencyCoords) {
		Logger.debug("Start adding remote copyDependencies to pom.xml");
		boolean libertyPluginFound = false;
		try {
			Model model = getPomModel();
			List<Plugin> plugins = model.getBuild().getPlugins();
			for (Plugin plugin : plugins) {
				if (MavenLibertyPlugin.LIBERTY_MAVEN_PLUGIN_ARTIFACT_ID.equals(plugin.getArtifactId())) {
					libertyPluginFound = true;
					plugin.getConfiguration();
					Xpp3Dom config = (Xpp3Dom)plugin.getConfiguration();
					if(config == null) {
						String content = MavenLibertyPlugin.LIBERTY_PLUGIN_EMPTY_CONFIGURATION_XML;
						config = Xpp3DomBuilder.build(new StringReader(content));
						plugin.setConfiguration(config);
					}
					Xpp3Dom copyDependencies = config.getChild(MavenLibertyPlugin.LIBERTY_PLUGIN_COPY_DEPENDENCIES_ELEMENT_NAME);
					if(copyDependencies == null) {
						String content = MavenLibertyPlugin.LIBERTY_PLUGIN_EMPTY_COPY_DEPENDENCIES_XML;
						copyDependencies = Xpp3DomBuilder.build(new StringReader(content));
						config.addChild(copyDependencies);
					}
					List<Xpp3Dom> existingDeps = Arrays.asList(copyDependencies.getChildren());
					
					Xpp3Dom depGroup = Xpp3DomBuilder.build(new StringReader(MavenLibertyPlugin.LIBERTY_PLUGIN_COPY_DEPENDENCY_GROUP_XML));

					for (MavenCoords coords : dependencyCoords) {
						if (dependecyExistsinList(existingDeps, coords.getArtifactId())) {
							Logger.warn("artifactId is alredy in copyDependencies. Not adding now." + coords.getArtifactId());
							return true;
						}
						String dependencyToAdd = String.format(MavenLibertyPlugin.LIBERTY_PLUGIN_COPY_DEPENDENCY_XML, 
								coords.getArtifactId(), 
								coords.getGroupId(), 
								coords.getVersion());

						Xpp3Dom dep = Xpp3DomBuilder.build(new StringReader(dependencyToAdd));
						depGroup.addChild(dep);
					}
					copyDependencies.addChild(depGroup);

					MavenXpp3Writer writer = new MavenXpp3Writer();
					writer.write(new FileOutputStream(new File(pomXmlFile)), model);
				}
			}
		} catch (Exception e){
			Logger.error("Exception when adding dependencies to copyDependencies");
			if (Logger.isDebugEnabled()) {
				e.printStackTrace();
			}
			return false;
		}

		if (!libertyPluginFound) {
			Logger.warn("copyDependencies not added because Liberty plugin not found. Liberty Dev mode may not work.");
		}

		return true;
	}

	// TODO this is related to copyDependencies method and shouldnt be here. the buildtool should NOT know the inner details of the plugin. this logic belongs in the plugin.
	/**
	 * Check if an artifact is already in a list of dependencies. Also serach inside the dependencyGroup element
	 * @see MavenUtils#addLibertyPluginCopyDependency
	 * @param dependencies
	 * @param artifactId
	 * @return
	 */
	private static boolean dependecyExistsinList(List<Xpp3Dom>dependencies, String artifactId) {
		for(Xpp3Dom dependency : dependencies) {
			if(dependency.getName().equals(MavenLibertyPlugin.LIBERTY_PLUGIN_DEPENDENCY_GROUP_ELEMENT_NAME)) {
				List<Xpp3Dom>depGroupDeps = Arrays.asList(dependency.getChildren());
				for(Xpp3Dom dep : depGroupDeps) {
					Xpp3Dom artifactIdElement = dependency.getChild(MavenLibertyPlugin.ARTIFACT_ID_ELEMENT_NAME);
					if (artifactIdElement != null && artifactIdElement.getValue().equals(artifactId)) {
						Logger.debug("Found " + artifactId + " in list");
						return true;
					}
				}
			} else {
				Xpp3Dom artifactIdElement = dependency.getChild(MavenLibertyPlugin.ARTIFACT_ID_ELEMENT_NAME);
				if (artifactIdElement != null && artifactIdElement.getValue().equals(artifactId)) {
					Logger.debug("Found " + artifactId + " in list");
					return true;
				}
			}
		}
		return false;
	}

	//TODO: consider to add this method to the BuildTool interface - will require Object instead of Plugin parameter and casting (or somother mechanism)
    /**
	 * Add a given plugin to the pom.xml
	 * @param pomXmlPath location of the pom.xml
	 * @param plugin 
	 */
	public boolean addPluginToPomXml(Plugin plugin) {
		Logger.debug("Start add plugin to pom.xml");
		Logger.info("pom.xml does not have liberty dev plugin. Adding now");
		try {
            Model model = getPomModel();
		    model.getBuild().addPlugin(plugin);
		    MavenXpp3Writer writer = new MavenXpp3Writer();
		    writer.write(new FileOutputStream(new File(pomXmlFile)), model);
        } catch (IOException ioe)  {
			Logger.debug("IO exception when adding plugin to pom.xml");
			if (Logger.isDebugEnabled()) {
                ioe.printStackTrace();
            }
			return false;
		} catch (XmlPullParserException xe) {
			Logger.debug("XML parsing exception when adding plugin to pom.xml: ");
			if (Logger.isDebugEnabled()) {
                xe.printStackTrace();
            }
			return false;
		}
		Logger.debug("Finish add plugin to pom.xml");
        return true;
	}

	/**
	 * Using the build tool to run recipies from the rewriteYaml. 
	 * This is the preferred way to run the recipes over adding or modifying the ReWrite plugin in the pom.xml.
	 * Will check for an existing rewrite.yml in the applicatoin root. If it exists, it will be backed up, and 
	 * restored after running the recipes.
	 * @param rewriteYaml a Map representing the rewrite.yml from the migration bundle
	 * @param rewritePlugin a RewritePlugin representing the MavenRewritePlugin. Used to retrive plugin details needed to run the mvn command.
	 * @return a boolean indicating if the recipe running was successful
	 */
	@Override
	public  boolean runRecipesFromRewriteYaml(Map<String, Object> rewriteYaml, RewritePlugin rewritePlugin) {
		
		String rewritePluginArtifactId = rewritePlugin.getArtifactId();
		String rewritePluginGroupId = rewritePlugin.getGroupId();
		String rewritePluginVersion = rewritePlugin.getVersion();
		String libertyRewriteArtifactId = rewritePlugin.getRewriteLibertyArtifactId();
		String libertyRewritePluginGroupId = rewritePlugin.getRewriteLibertyGroupId();
		String libertyRewritePluginVersion = rewritePlugin.getRewriteLibertyVersion();
		String rewriteYamlRecipeName = (String)rewriteYaml.get(RecipeAutomation.REWRITE_YAML_RECIPE_NAME);
		

		// mvn -U org.openrewrite.maven:rewrite-maven-plugin:run 
		//		-Drewrite.recipeArtifactCoordinates=org.openrewrite.recipe:rewrite-liberty:1.1.4 
		//		-Drewrite.activeRecipes=com.ibm.testRecipe
		List<String> args = Arrays.asList(
			"-U",
			String.format(MVN_OPEN_REWRITE_ARG, rewritePluginGroupId, rewritePluginArtifactId, rewritePluginVersion),
			String.format(MVN_RECIPE_ARTIFACT_COORDS_ARG, libertyRewritePluginGroupId, libertyRewriteArtifactId, libertyRewritePluginVersion),
			String.format(MVN_RECIPE_NAME_ARG, rewriteYamlRecipeName)
		);

		// Backup an existing rewrite.yaml if it exists.
		File rootRewriteYaml = new File(applicationDir + File.separator + RecipeAutomation.REWRITE_YAML_FILE_NAME);
		boolean origRewriteYamlExists = rootRewriteYaml.exists();
		String backupFile = null;
		if (origRewriteYamlExists) {
			try {
				backupFile = backupFile(applicationDir + File.separator + RecipeAutomation.REWRITE_YAML_FILE_NAME);
			} catch (IOException e) {
				Logger.error("Failed to create backup of " + RecipeAutomation.REWRITE_YAML_FILE_NAME);
				if (Logger.isDebugEnabled()) {
					e.printStackTrace();
				}
				return false;
			}
		}

		// Write rewrite.yaml to file in app root
		try {
			Yaml yaml = new Yaml();
			FileWriter writer = new FileWriter(applicationDir + File.separator + RecipeAutomation.REWRITE_YAML_FILE_NAME);
			yaml.dump(rewriteYaml, writer);
		} catch (IOException e) {
			Logger.error("Failed to create file " + RecipeAutomation.REWRITE_YAML_FILE_NAME);
			if (Logger.isDebugEnabled()) {
				e.printStackTrace();
			}
			return false;
		}

		// Run the recipes
		boolean runSuccess = runBuild(args);

		// Restore the backup
		if (origRewriteYamlExists) {
			try {
				rootRewriteYaml.delete();
				FileUtils.moveFile(new File(backupFile), rootRewriteYaml);
			} catch (IOException e) {
				Logger.warn("Failed to restore backup of " + RecipeAutomation.REWRITE_YAML_FILE_NAME);
				if (Logger.isDebugEnabled()) {
					e.printStackTrace();
				}
			}
		} else {
			// Remove the rewrite.yml
			try {
				rootRewriteYaml.delete();
			} catch (SecurityException e) {
				Logger.warn("Failed to delete file: " + RecipeAutomation.REWRITE_YAML_FILE_NAME);
				if (Logger.isDebugEnabled()) {
					e.printStackTrace();
				}
			}
		}
		return runSuccess;
	}
}
