// (C) Copyright IBM Corp. 2024
package com.ibm.ta.jam.recipe;

import java.util.List;
import java.util.Map;


import com.ibm.ta.jam.plugin.GradleRewritePlugin;
import com.ibm.ta.jam.plugin.RewritePlugin;

public class GradleRecipeAutomation implements RecipeAutomation {
    private Map<String, Object> recipeListYaml;
    private GradleRewritePlugin rewritePlugin;

    @Override
    public List<String> getAllRecipes() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getAllRecipes'");
    }

    /**
     * Run one or more recipes from available recipes
     */
    @Override
    public boolean runRecipes(List<String> recipes) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'runRecipes'");
    }

    @Override
    public Map<String, Object> getRewriteYaml() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getRewriteYaml'");
    }

    @Override
    public RewritePlugin getRewritePlugin() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getRewritePlugin'");
    }

    @Override
    public boolean runAllRecipes() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'runAllRecipes'");
    }
}

