// (C) Copyright IBM Corp. 2024
package com.ibm.ta.jam.plugin;

/**
 * Model of build tool plugins. All plugins are expected to implement this interface.
 */
public interface BuildToolPlugin {
    String getArtifactId();
    String getGroupId();
    String getVersion();
}
