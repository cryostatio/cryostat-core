package com.redhat.rhjmc.containerjfr.core.net.discovery;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import org.openjdk.jmc.jdp.client.JDPClient;

import com.redhat.rhjmc.containerjfr.core.log.Logger;

public class JvmDiscoveryClient {

    private final Logger logger;
    private final JDPClient jdp;

    public JvmDiscoveryClient(Logger logger) {
        this.logger = logger;
        this.jdp = new JDPClient();
    }

    public void start() throws IOException {
        this.logger.info("JDP Discovery started");
        this.jdp.start();
    }

    public void stop() {
        this.logger.info("JDP Discovery stopped");
        this.jdp.stop();
    }

    public List<DiscoveredJvmDescriptor> getDiscoveredJvmDescriptors() {
        return this.jdp.getDiscoverables().stream()
                .map(d -> new DiscoveredJvmDescriptor(d.getPayload()))
                .collect(Collectors.toList());
    }
}
