// (C) Copyright IBM Corp. 2024
package com.ibm.ta.jam.utils;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MavenCoords {
    public MavenCoords(String artifactId, String groupId, String version) {
        this.artifactId = artifactId;
        this.groupId = groupId;
        this.version = version;
    }
    private String artifactId;
    private String groupId;
    private String version;
}
