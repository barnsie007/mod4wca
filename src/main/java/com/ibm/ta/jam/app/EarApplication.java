// (C) Copyright IBM Corp. 2024
package com.ibm.ta.jam.app;

import java.util.List;

public class EarApplication implements Application {
    
    @Override
	public boolean addServerConfigFromBundle(String serverXmlPath) {
        return false;
    }
    @Override
    public boolean addPlaceholderDependencies() {
        return false;
    }

    @Override
    public boolean addLocalDependencies(List<String> dependencyPaths) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'addLocalDependencies'");
    }
}
