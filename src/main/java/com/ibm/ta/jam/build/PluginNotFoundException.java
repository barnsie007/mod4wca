// (C) Copyright IBM Corp. 2024
package com.ibm.ta.jam.build;

public class PluginNotFoundException extends Exception {
    public PluginNotFoundException(String statement){
        super(statement);
    }
}
