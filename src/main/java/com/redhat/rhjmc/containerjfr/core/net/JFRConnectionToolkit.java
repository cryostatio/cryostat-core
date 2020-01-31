package com.redhat.rhjmc.containerjfr.core.net;

import javax.management.remote.JMXServiceURL;

import org.openjdk.jmc.rjmx.ConnectionDescriptorBuilder;

import com.redhat.rhjmc.containerjfr.core.tui.ClientWriter;

public class JFRConnectionToolkit {

    private final ClientWriter cw;

    public JFRConnectionToolkit(ClientWriter cw) {
        this.cw = cw;
    }

    public JFRConnection connect(JMXServiceURL url) throws Exception {
        return new JFRConnection(cw, new ConnectionDescriptorBuilder().url(url).build());
    }

    public JFRConnection connect(String host) throws Exception {
        return connect(host, JFRConnection.DEFAULT_PORT);
    }

    public JFRConnection connect(String host, int port) throws Exception {
        return new JFRConnection(
                cw, new ConnectionDescriptorBuilder().hostName(host).port(port).build());
    }
}
