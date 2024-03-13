// (C) Copyright IBM Corp. 2024
package com.ibm.ta.jam.build;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.gradle.tooling.BuildLauncher;
import org.gradle.tooling.GradleConnectionException;
import org.gradle.tooling.GradleConnector;
import org.gradle.tooling.ProjectConnection;
import org.gradle.tooling.ResultHandler;

import com.ibm.ta.jam.app.ApplicationFactory.ApplicationType;
import com.ibm.ta.jam.build.BuildToolFactory.BuildToolType;
import com.ibm.ta.jam.plugin.RewritePlugin;
import com.ibm.ta.jam.utils.MavenCoords;

public class GradleBuildTool implements BuildTool {
    private String applicationDir;

    public GradleBuildTool(String applicationDir) {
        this.applicationDir = applicationDir;
    }
    @Override
	public boolean backupBuildConfig() { return false; }

	@Override
	public BuildToolType getBuildToolType() {
		return BuildToolType.GRADLE;
	}

    @Override
	public ApplicationType getPackagingType() { return null; }

    @Override
	public boolean runBasicBuild() {
		List<String> args = Arrays.asList("clean", "build");
      	return runBuild(args);
	}

    @Override
    public boolean addLibertyDevPlugin() { return false; }

	@Override
	public boolean runLibertyDevMode() {
		// TODO implement this.
		return false;
	}

	@Override
	public boolean runBuild(List<String> tasks) {
		ProjectConnection connection = GradleConnector.newConnector()
    			.forProjectDirectory(new File(applicationDir))
    			.connect();
		
		final BuildRunInfo buildRunInfo = new BuildRunInfo();
		ResultHandler<Object> resultHandler = new ResultHandler<Object>() {
			
			@Override
			public void onFailure(GradleConnectionException failure) {
				buildRunInfo.setFinished(true);
				buildRunInfo.setSuccessful(false);
			}
			
			@Override
			public void onComplete(Object result) {
				buildRunInfo.setFinished(true);
				buildRunInfo.setSuccessful(true);
			}
		};
		
		BuildLauncher buildLauncher = connection.newBuild().forTasks(tasks.toArray(new String[]{}));
		buildLauncher.run(resultHandler);
		
		waitForBuild(buildRunInfo);
		
		return buildRunInfo.isSuccessful();
	}
	
	synchronized void waitForBuild(final BuildRunInfo buildRunInfo) {
		while (!buildRunInfo.isFinished()) {
			try {
				wait(100L);
			} catch (InterruptedException e) {
				
			}
		}
	}
	
	class BuildRunInfo {
		private boolean isFinished;
		private boolean isSuccessful;

		public boolean isFinished() {
			return isFinished;
		}

		public void setFinished(boolean isFinished) {
			this.isFinished = isFinished;
		}

		public boolean isSuccessful() {
			return isSuccessful;
		}

		public void setSuccessful(boolean isSuccessful) {
			this.isSuccessful = isSuccessful;
		}
	}

    @Override
    public boolean addLocalDependencies(List<String> dependencyPaths) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'addLocalDependencies'");
    }
    @Override
    public boolean addRemoteDependencies(List<MavenCoords> dependencyCoords) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'addRemoteDependencies'");
    }
	@Override
	public boolean hasLibertyDevPlugin() {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Unimplemented method 'hasLibertyDevPlugin'");
	}
	@Override
	public String getBuildToolConfiguration() {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Unimplemented method 'getBuildToolConfiguration'");
	}
	
	@Override
	public  boolean runRecipesFromRewriteYaml(Map<String, Object> rewriteYaml, RewritePlugin rewritePlugin) {
		// TODO Auto-generated method stu
		throw new UnsupportedOperationException("Unimplemented method 'runRecipesInExternal'");
	}
	@Override
	public boolean runBasicBuild(boolean skipTests) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Unimplemented method 'runBasicBuild'");
	}

}
