package com.redhat.rhjmc.containerjfr.core.net.discovery;

import java.net.MalformedURLException;
import java.util.Map;

import javax.management.remote.JMXServiceURL;

public class DiscoveredJvmDescriptor {

    private final Map<String, String> o;

    DiscoveredJvmDescriptor(Map<String, String> o) {
        this.o = o;
    }

    public String getMainClass() {
        return o.get("MAIN_CLASS");
    }

    public JMXServiceURL getJmxServiceUrl() throws MalformedURLException {
        return new JMXServiceURL(o.get("JMX_SERVICE_URL"));
    }

    @Override
    public String toString() {
        return o.toString();
    }

}
