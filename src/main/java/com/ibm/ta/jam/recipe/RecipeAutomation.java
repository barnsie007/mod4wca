// (C) Copyright IBM Corp. 2024
package com.ibm.ta.jam.recipe;

import java.util.List;
import java.util.Map;

import com.ibm.ta.jam.plugin.RewritePlugin;


public interface RecipeAutomation {
    public static final String REWRITE_YAML_RECIPE_NAME = "name";
    public static final String REWRITE_YAML_FILE_NAME = "rewrite.yml";

    List<String> getAllRecipes();

    boolean runRecipes(List<String> recipes);

    boolean runAllRecipes();

    Map<String, Object> getRewriteYaml();

    RewritePlugin getRewritePlugin();
}
