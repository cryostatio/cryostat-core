package com.redhat.rhjmc.containerjfr.core.net;

import com.redhat.rhjmc.containerjfr.core.sys.Clock;
import com.redhat.rhjmc.containerjfr.core.tui.ClientWriter;

public class JFRConnectionToolkit {

    private final ClientWriter cw;
    private final Clock clock;

    public JFRConnectionToolkit(ClientWriter cw, Clock clock) {
        this.cw = cw;
        this.clock = clock;
    }

    public JFRConnection connect(String host) throws Exception {
        return connect(host, JFRConnection.DEFAULT_PORT);
    }

    public JFRConnection connect(String host, int port) throws Exception {
        return new JFRConnection(cw, clock, host, port);
    }
}
