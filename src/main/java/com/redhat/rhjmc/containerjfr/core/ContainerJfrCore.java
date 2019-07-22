package com.redhat.rhjmc.containerjfr.core;

import com.redhat.rhjmc.containerjfr.core.jmc.RegistryProvider;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.RegistryFactory;

public class ContainerJfrCore {
    private ContainerJfrCore() { }

    public static void initialize() throws CoreException {
        RegistryFactory.setDefaultRegistryProvider(new RegistryProvider());
    }
}
