// (C) Copyright IBM Corp. 2024
package com.ibm.ta.jam.plugin;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.tinylog.Logger;

import com.ibm.ta.jam.utils.MavenCoords;

/**
 * Model of the Maven Rewrite plugin
 * {@snippet
        <plugin>
            <groupId>org.openrewrite.maven</groupId>
            <artifactId>rewrite-maven-plugin</artifactId>
            <version>5.4.2</version>
            <configuration>
                <activeRecipes>
                    <!--Getting the server name on Liberty-->
                    <recipe>org.openrewrite.java.liberty.ServerName</recipe>
                    <!--Use the default InitialContext JNDI properties-->
                    <recipe>org.openrewrite.java.liberty.RemoveWas2LibertyNonPortableJndiLookup</recipe>
                    <!--Do not use the WSSecurityHelper revokeSSOCookies method-->
                    <recipe>org.openrewrite.java.liberty.WebSphereUnavailableSSOCookieMethod</recipe>
                </activeRecipes>
            </configuration>
            <dependencies>
                <dependency>
                    <groupId>org.openrewrite.recipe</groupId>
                    <artifactId>rewrite-liberty</artifactId>
                    <version>1.0.0</version>
                </dependency>
            </dependencies>
        </plugin>
    }
 */
public class MavenRewritePlugin implements RewritePlugin {
    private final static String ACTIVE_RECIPES_ELEMENT_NAME = "activeRecipes";
    private final static String MAVEN_PROJECT_WITH_PLUGIN_XML =
	"<project>" +
		"<modelVersion>4.0.0</modelVersion>" +
		"<groupId>groupId1</groupId>" +
		"<artifactId>app1</artifactId>" +
		"<version>1</version>" +
		"<build>" +
			"<plugins>" +
				"%s" +
			"</plugins>" +
		"</build>" +
	"</project>";

    /**
     * The Maven plugin object {@link org.apache.maven.model.Plugin}
     */
    private Plugin mavenPlugin;

    /**
     * list of dependencies described in the plugin
     */
    private List<MavenCoords> dependencies;

    /**
     * List of active recipes described in the plugin
     */
    private List<String> activeRecipes;

    /**
     * Create the plugin object from the plugin config (recipes.pom)
     * @param rewritePluginConfigPath full path to the plugin in the migration bundle (recipes.pom)
     * @throws PluginInitializationException when fail to initialize the plugin values
     */
    public MavenRewritePlugin(String rewritePluginConfigPath) throws PluginInitializationException {
        Plugin plugin = null;
        try {
            plugin = createPlugin(rewritePluginConfigPath);
        } catch (IOException | XmlPullParserException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        
        this.mavenPlugin = plugin;

        dependencies = new ArrayList<>();
        activeRecipes = new ArrayList<>();

        if (!initialize()) {
            throw new PluginInitializationException("Exception initializing Plugin in MavenRewritePlugin");
        }
    }

    /**
     * Get the Maven plugin object {@link org.apache.maven.model.Plugin}
     * @return
     */
    public Plugin getMavenPlugin() {
        return mavenPlugin;
    }

    /**
     * Get the artifactId for the plugin
     */
    @Override
    public String getArtifactId() {
        return mavenPlugin.getArtifactId();
    }

    /**
     * Get the groupId for the plugin
     */
    @Override
    public String getGroupId() {
        return mavenPlugin.getGroupId();
    }

    /**
     * Get the versino for the plugin
     */
    @Override
    public String getVersion() {
        return mavenPlugin.getVersion();
    }

    /**
     * Get the list of dependencies in the plugin - expect the rewrite-liberty to be listed
     */
    @Override
    public List<MavenCoords> getDependencies() {
        return dependencies;
    }

    /**
     * Get the list of active recipes listed in the plugin
     */
    @Override
    public List<String> getActiveRecipes() {
        return activeRecipes;
    }

    /**
     * Create te Maven plugin object {@link org.apache.maven.model.Plugin}
     * @param rewritePluginConfigPath full path to the plugin in the migration bundle (recipes.pom)
     * @return the Maven Plugin object for the Rewrite Plugin
     * @throws IOException when encouters problem reading the plugin config file
     * @throws XmlPullParserException when encounters problems parsing the plugin config file
     */
    private Plugin createPlugin(String rewritePluginConfigPath) throws IOException, XmlPullParserException {
        String pluginAsString = Files.readString(Path.of(rewritePluginConfigPath));
        String fullPomWithPlugin = String.format(MAVEN_PROJECT_WITH_PLUGIN_XML, pluginAsString);
        MavenXpp3Reader mavenreader = new MavenXpp3Reader();
        Reader reader = new StringReader(fullPomWithPlugin);
        Model model = mavenreader.read(reader);
        return model.getBuild().getPlugins().get(0);
    }

    /**
     * Initialize the activeRecipes and dependencies from values in the plugin
     * 
     * @return a boolean indicating if the initialization was successful or not
     */
    private boolean initialize() {
        Xpp3Dom configElement = (Xpp3Dom)mavenPlugin.getConfiguration();
        if(configElement != null) {
            Xpp3Dom activeRecipesElement = configElement.getChild(ACTIVE_RECIPES_ELEMENT_NAME);
            if (activeRecipesElement != null) {
                List<Xpp3Dom> recipeElements = Arrays.asList(activeRecipesElement.getChildren());
                for (Xpp3Dom recipeElement : recipeElements) {
                    String recipeName = recipeElement.getValue();
                    activeRecipes.add(recipeName);
                }
            }
        }
        List<Dependency> dependenciesElements = mavenPlugin.getDependencies();
        for (Dependency depElement : dependenciesElements) {
            MavenCoords dep = new MavenCoords(depElement.getArtifactId(), depElement.getGroupId(), depElement.getVersion());
            dependencies.add(dep);
        }
        
        if (activeRecipes.size() == 0) {
            Logger.warn("No activeRecipes found in plugin configuration");
        }

        return true;
    }

    /**
     * Get artifactId of liberty-rewrite (Listed as dependency in RewritePlugin)
     */
    @Override
    public String getRewriteLibertyArtifactId() {
        return RewritePlugin.REWRITE_LIBERTY_ARTIFACT_ID;
    }

    /**
     * Get groupId of liberty-rewrite (Listed as dependency in RewritePlugin)
     */
    @Override
    public String getRewriteLibertyGroupId() {
        for (MavenCoords coords : dependencies) {
            if (coords.getArtifactId().equals(RewritePlugin.REWRITE_LIBERTY_ARTIFACT_ID)) {
                return coords.getGroupId();
            }
        }
        throw new RuntimeException("Liberty rewrite dependency not found in Rewrite plugin");
    }

    /**
     * Getversion of liberty-rewrite (Listed as dependency in RewritePlugin)
     */
    @Override
    public String getRewriteLibertyVersion() {
        for (MavenCoords coords : dependencies) {
            if (coords.getArtifactId().equals(RewritePlugin.REWRITE_LIBERTY_ARTIFACT_ID)) {
                return coords.getVersion();
            }
        }
        throw new RuntimeException("Liberty rewrite dependency not found in Rewrite plugin");
    }
}
