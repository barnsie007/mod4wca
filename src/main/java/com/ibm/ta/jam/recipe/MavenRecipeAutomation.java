// (C) Copyright IBM Corp. 2024
package com.ibm.ta.jam.recipe;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

import org.tinylog.Logger;
import org.yaml.snakeyaml.Yaml;

import com.ibm.ta.jam.build.BuildTool;
import com.ibm.ta.jam.plugin.MavenRewritePlugin;
import com.ibm.ta.jam.plugin.PluginInitializationException;
import com.ibm.ta.jam.plugin.RewritePlugin;

/**
 * Model for RecipeAutomation in JAM
 */
public class MavenRecipeAutomation implements RecipeAutomation {

    public static final String REWRITE_RECIPE_LIST_NAME = "recipeList";

    /**
     * Full path to the rewrite plugin configuration from migration bundle (recipes.pom)
     */
    private String rewritePluginConfigPath;

    /**
     * Full path to the rewrite.yml from migration bundle
     */
    private String rewriteYamlPath;

    /**
     * Model of the RewritePlugin for Maven
     */
    private MavenRewritePlugin rewritePlugin;

    /**
     * Map representing the data in the rewrite.yml
     */
    private Map<String, Object> rewriteYaml;

    /**
     * Build tool object used to interface the Plugin with the build tool
     */
    private BuildTool buildTool;

    /**
     * Create and initialize the MavenRecipeAutomation object
     * @param rewritePluginConfigPath full path to the rewrite plugin configuration (recipes.pom)
     * @param rewriteYamlPath full path to the rewrite.yml from migration bundle
     * @param buildTool BuildTool object
     */
    public MavenRecipeAutomation(String rewritePluginConfigPath, String rewriteYamlPath, BuildTool buildTool) {
        this.rewritePluginConfigPath = rewritePluginConfigPath;
        this.rewriteYamlPath = rewriteYamlPath;
        this.buildTool = buildTool;
        initialize();
    }

    /**
     * Get all recipes from the rewrite.yml
     */
    @SuppressWarnings("unchecked")
    @Override
    public List<String> getAllRecipes() {
        return (ArrayList<String>)rewriteYaml.get("recipeList");
    }


    //TODO: Change recipes form strings to object - as recipes might have parameters in future
    /**
     * Run the given set of recipes
     */
    @Override
    public boolean runRecipes(List<String> recipes) {
        List<String> availableRecipes = getAllRecipes();
        List<String> recipesToRun = new ArrayList<>();
        Map<String, Object> newRewriteYaml;
        // Check all recipes exist in available recipes
        for (String recipe : recipes) {
            if (!availableRecipes.contains(recipe)) {
                Logger.warn("Recipe not found in available recipes: " + recipe);
            } else {
                recipesToRun.add(recipe);
            }
        }
        if (recipesToRun.size() == 0) {
            Logger.warn("No valid recipes to run");
            return false;
        } else {
            newRewriteYaml = new HashMap<String, Object>(rewriteYaml);
            newRewriteYaml.put(REWRITE_RECIPE_LIST_NAME, recipesToRun);
        }

        // TODO: Understand if we need to merge to existing plugin in build config if it exists, or if we can always run rewrite externally from rewrite.yml
        // The preferred option is to run from rewrite.yml as its less disruptive to the build environment
        return buildTool.runRecipesFromRewriteYaml(newRewriteYaml, rewritePlugin);
    }

    /**
     * Run all recipes that we have in the rewriteYaml
     */
    @Override
    public boolean runAllRecipes() {
        return runRecipes(getAllRecipes());
    }

    /**
     * Get teh RewritePlugin object
     */
    @Override
    public RewritePlugin getRewritePlugin() {
        return rewritePlugin;
    }

    /**
     * Get the Map representing the rewriteYaml
     */
    @Override
    public Map<String, Object> getRewriteYaml() {
        return rewriteYaml;
    }

    /**
     * Initialise the rewritePlugin and the rewriteYaml
     * @return a boolean indicating if the initialization was succesful or not
     */
    private boolean initialize() {
        try {
            rewritePlugin = new MavenRewritePlugin(rewritePluginConfigPath);

            if (!createRewriteYaml()) {
                Logger.error("RewriteYaml could not be created");
                return false;
            }

            return true;
        } catch (PluginInitializationException pe) {
            Logger.error("RewritePlugin could not be initialized.");
            return false;
        }
    }

    /**
     * Read the following YAML and produce a MAP representation:
     * 
     * type: specs.openrewrite.org/v1beta/recipe
     * name: com.ibm.testRecipe
     * recipeList:
     *   - org.openrewrite.java.liberty.ServerName
     *   - org.openrewrite.java.liberty.RemoveWas2LibertyNonPortableJndiLookup
     *   - org.openrewrite.java.liberty.WebSphereUnavailableSSOCookieMethod
     * 
     * @return a Map representing the YAML
     */
    private boolean createRewriteYaml() {
        InputStream inputStream = null;;
        try {
            inputStream = new FileInputStream(new File(rewriteYamlPath));
        } catch (FileNotFoundException e) {
            Logger.error("Rewrite Yaml file could not be found: " + rewriteYamlPath);
            if (Logger.isDebugEnabled()) {
                e.printStackTrace();
            }
            return false;
        }
        Yaml yaml = new Yaml();
        rewriteYaml = yaml.load(inputStream);
        return true;
    }
}

