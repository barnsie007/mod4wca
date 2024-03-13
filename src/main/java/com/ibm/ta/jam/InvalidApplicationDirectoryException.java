// (C) Copyright IBM Corp. 2024
package com.ibm.ta.jam;

public class InvalidApplicationDirectoryException extends Exception {
    public InvalidApplicationDirectoryException(String statement){
        super(statement);
    }
}
