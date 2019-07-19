package com.redhat.rhjmc.containerjfr.core.net;

import com.redhat.rhjmc.containerjfr.core.sys.Clock;
import com.redhat.rhjmc.containerjfr.core.tui.ClientWriter;

public class JMCConnectionToolkit {

    private final ClientWriter cw;
    private final Clock clock;

    JMCConnectionToolkit(ClientWriter cw, Clock clock) {
        this.cw = cw;
        this.clock = clock;
    }

    public JMCConnection connect(String host) throws Exception {
        return connect(host, JMCConnection.DEFAULT_PORT);
    }

    public JMCConnection connect(String host, int port) throws Exception {
        return new JMCConnection(cw, clock, host, port);
    }
}
