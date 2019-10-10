package com.redhat.rhjmc.containerjfr.core.net;

import javax.management.remote.JMXServiceURL;

import com.redhat.rhjmc.containerjfr.core.tui.ClientWriter;

public class JFRConnectionToolkit {

    private final ClientWriter cw;

    public JFRConnectionToolkit(ClientWriter cw) {
        this.cw = cw;
    }

    public JFRConnection connect(JMXServiceURL url) throws Exception {
        return new JFRConnection(cw, url);
    }

    public JFRConnection connect(String host) throws Exception {
        return connect(host, JFRConnection.DEFAULT_PORT);
    }

    public JFRConnection connect(String host, int port) throws Exception {
        return new JFRConnection(cw, host, port);
    }
}
