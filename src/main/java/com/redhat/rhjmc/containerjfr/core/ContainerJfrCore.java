package com.redhat.rhjmc.containerjfr.core;

import java.util.logging.LogManager;

import com.redhat.rhjmc.containerjfr.core.jmc.RegistryProvider;
import com.redhat.rhjmc.containerjfr.core.jmc.SecurityManager;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.RegistryFactory;

public class ContainerJfrCore {
    private ContainerJfrCore() {}

    public static void initialize() throws CoreException {
        System.setProperty(
                "org.openjdk.jmc.common.security.manager",
                SecurityManager.class.getCanonicalName());
        LogManager.getLogManager().reset();
        RegistryFactory.setDefaultRegistryProvider(new RegistryProvider());
    }
}
