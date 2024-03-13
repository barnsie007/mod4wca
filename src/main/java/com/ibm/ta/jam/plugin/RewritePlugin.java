// (C) Copyright IBM Corp. 2024
package com.ibm.ta.jam.plugin;
import java.util.List;

import com.ibm.ta.jam.utils.MavenCoords;

/**
 * Model for RewritePlugin for BuildTools. All RewritePlugins are expected to implement this interface
 */
public interface RewritePlugin extends BuildToolPlugin {
    public static String REWRITE_PLUGIN_ARTIFACT_ID = "org.openrewrite.maven";
    public static String REWRITE_LIBERTY_ARTIFACT_ID = "rewrite-liberty";

    public List<MavenCoords> getDependencies();
    public String getRewriteLibertyArtifactId();
    public String getRewriteLibertyGroupId();
    public String getRewriteLibertyVersion();
    
    public List<String> getActiveRecipes();
}
