/*
 * Copyright The Cryostat Authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.cryostat.core.net;

import java.io.IOException;
import java.lang.management.MemoryMXBean;
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.RuntimeMXBean;
import java.lang.management.ThreadMXBean;
import java.net.MalformedURLException;
import java.util.List;

import javax.management.MBeanServerConnection;
import javax.management.remote.JMXServiceURL;

import org.openjdk.jmc.rjmx.ConnectionDescriptorBuilder;
import org.openjdk.jmc.rjmx.ConnectionException;
import org.openjdk.jmc.rjmx.ConnectionToolkit;

import io.cryostat.core.sys.Environment;
import io.cryostat.core.sys.FileSystem;
import io.cryostat.core.tui.ClientWriter;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

public class JFRConnectionToolkit {

    private final ClientWriter cw;
    private final FileSystem fs;
    private final Environment env;

    @SuppressFBWarnings(
            value = "EI_EXPOSE_REP2",
            justification = "no mutable fields can be accessed through this class")
    public JFRConnectionToolkit(ClientWriter cw, FileSystem fs, Environment env) {
        this.cw = cw;
        this.fs = fs;
        this.env = env;
    }

    public JFRConnection connect(JMXServiceURL url)
            throws ConnectionException, IllegalStateException {
        return connect(url, null);
    }

    public JFRConnection connect(JMXServiceURL url, Credentials credentials)
            throws ConnectionException, IllegalStateException {
        return connect(url, credentials, List.of());
    }

    public JFRConnection connect(
            JMXServiceURL url, Credentials credentials, List<Runnable> listeners)
            throws ConnectionException, IllegalStateException {
        ConnectionDescriptorBuilder connectionDescriptorBuilder = new ConnectionDescriptorBuilder();
        connectionDescriptorBuilder = connectionDescriptorBuilder.url(url);
        if (credentials != null) {
            connectionDescriptorBuilder =
                    connectionDescriptorBuilder
                            .username(credentials.getUsername())
                            .password(credentials.getPassword());
        }
        return new JFRJMXConnection(cw, fs, env, connectionDescriptorBuilder.build(), listeners);
    }

    public String getHostName(JMXServiceURL url) {
        return ConnectionToolkit.getHostName(url);
    }

    public int getPort(JMXServiceURL url) {
        return ConnectionToolkit.getPort(url);
    }

    public JMXServiceURL createServiceURL(String host, int port) throws MalformedURLException {
        return ConnectionToolkit.createServiceURL(host, port);
    }

    public int getDefaultPort() {
        return ConnectionToolkit.getDefaultPort();
    }

    public MemoryMXBean getMemoryBean(MBeanServerConnection server) throws IOException {
        return ConnectionToolkit.getMemoryBean(server);
    }

    public RuntimeMXBean getRuntimeBean(MBeanServerConnection server) throws IOException {
        return ConnectionToolkit.getRuntimeBean(server);
    }

    public ThreadMXBean getThreadBean(MBeanServerConnection server) throws IOException {
        return ConnectionToolkit.getThreadBean(server);
    }

    public OperatingSystemMXBean getOperatingSystemBean(MBeanServerConnection server)
            throws IOException {
        return ConnectionToolkit.getOperatingSystemBean(server);
    }
}
