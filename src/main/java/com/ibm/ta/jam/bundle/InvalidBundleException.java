// (C) Copyright IBM Corp. 2024
package com.ibm.ta.jam.bundle;

public class InvalidBundleException extends Exception {
    public InvalidBundleException(String statement){
        super(statement);
    }
}
