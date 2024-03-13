// (C) Copyright IBM Corp. 2024
package com.ibm.ta.jam.build;


import java.util.List;
import java.util.Map;

import com.ibm.ta.jam.app.ApplicationFactory.ApplicationType;
import com.ibm.ta.jam.build.BuildToolFactory.BuildToolType;
import com.ibm.ta.jam.plugin.RewritePlugin;
import com.ibm.ta.jam.utils.MavenCoords;

public interface BuildTool {
    boolean backupBuildConfig();

    BuildToolType getBuildToolType();

    String getBuildToolConfiguration();
    
    ApplicationType getPackagingType();

    boolean runBuild(List<String> buildArgs);

	boolean runBasicBuild(boolean skipTests);

    boolean runBasicBuild();
	
	boolean runLibertyDevMode();

    boolean hasLibertyDevPlugin();

    boolean addLibertyDevPlugin();

    boolean runRecipesFromRewriteYaml(Map<String, Object> rewriteYaml, RewritePlugin rewritePlugin);

    boolean addLocalDependencies(List<String> dependencyPaths);

    boolean addRemoteDependencies(List<MavenCoords> dependencyCoords);
}
