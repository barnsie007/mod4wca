// (C) Copyright IBM Corp. 2024
package com.ibm.ta.jam.recipe;

import org.tinylog.Logger;

import com.ibm.ta.jam.build.BuildTool;
import com.ibm.ta.jam.build.BuildToolFactory.BuildToolType;
import com.ibm.ta.jam.bundle.ExpandedBundle;

public class RecipeAutomationFactory {

    public static RecipeAutomation getRecipeAutomation(ExpandedBundle expandedBundle, BuildTool buildTool) 
            throws UnsupportedOperationException, RecipeAutomationInitializationException {
        if (buildTool.getBuildToolType() == BuildToolType.MAVEN) {
            Logger.debug("Getting MavenRewritePlugin");
            return new MavenRecipeAutomation(expandedBundle.getRewritePluginConfigPath(), expandedBundle.getRewriteYamlPath(), buildTool);
        } else if (buildTool.getBuildToolType() == BuildToolType.GRADLE) {
            Logger.debug("Getting GradleRewritePlugin");
            return new GradleRecipeAutomation();
        } else {
            throw new UnsupportedOperationException("Unknown build tool type. Supported types are MAVEN and GRADLE");
        }
    }
}
