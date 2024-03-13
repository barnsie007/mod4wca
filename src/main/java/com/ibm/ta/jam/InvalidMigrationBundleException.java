// (C) Copyright IBM Corp. 2024
package com.ibm.ta.jam;

public class InvalidMigrationBundleException extends Exception {
    public InvalidMigrationBundleException(String statement){
        super(statement);
    }
}
