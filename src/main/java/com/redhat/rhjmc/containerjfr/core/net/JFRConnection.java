/*-
 * #%L
 * Container JFR Core
 * %%
 * Copyright (C) 2020 Red Hat, Inc.
 * %%
 * The Universal Permissive License (UPL), Version 1.0
 *
 * Subject to the condition set forth below, permission is hereby granted to any
 * person obtaining a copy of this software, associated documentation and/or data
 * (collectively the "Software"), free of charge and under any and all copyright
 * rights in the Software, and any and all patent rights owned or freely
 * licensable by each licensor hereunder covering either (i) the unmodified
 * Software as contributed to or provided by such licensor, or (ii) the Larger
 * Works (as defined below), to deal in both
 *
 * (a) the Software, and
 * (b) any piece of software and/or hardware listed in the lrgrwrks.txt file if
 * one is included with the Software (each a "Larger Work" to which the Software
 * is contributed by such licensors),
 *
 * without restriction, including without limitation the rights to copy, create
 * derivative works of, display, perform, and distribute the Software and make,
 * use, sell, offer for sale, import, export, have made, and have sold the
 * Software and the Larger Work(s), and to sublicense the foregoing rights on
 * either these or other terms.
 *
 * This license is subject to the following condition:
 * The above copyright notice and either this complete permission notice or at
 * a minimum a reference to the UPL must be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 * #L%
 */
package com.redhat.rhjmc.containerjfr.core.net;

import java.io.IOException;
import java.util.List;

import javax.management.remote.JMXServiceURL;

import org.openjdk.jmc.rjmx.ConnectionToolkit;
import org.openjdk.jmc.rjmx.IConnectionDescriptor;
import org.openjdk.jmc.rjmx.IConnectionHandle;
import org.openjdk.jmc.rjmx.IConnectionListener;
import org.openjdk.jmc.rjmx.internal.DefaultConnectionHandle;
import org.openjdk.jmc.rjmx.internal.RJMXConnection;
import org.openjdk.jmc.rjmx.internal.ServerDescriptor;
import org.openjdk.jmc.rjmx.services.jfr.IFlightRecorderService;
import org.openjdk.jmc.rjmx.services.jfr.internal.FlightRecorderServiceFactory;
import org.openjdk.jmc.rjmx.services.jfr.internal.FlightRecorderServiceV2;

import com.redhat.rhjmc.containerjfr.core.sys.Clock;
import com.redhat.rhjmc.containerjfr.core.templates.RemoteTemplateService;
import com.redhat.rhjmc.containerjfr.core.templates.TemplateService;
import com.redhat.rhjmc.containerjfr.core.tui.ClientWriter;

public class JFRConnection implements AutoCloseable {

    public static final int DEFAULT_PORT = 9091;

    private final ClientWriter cw;
    private final RJMXConnection rjmxConnection;
    private final IConnectionHandle handle;
    private final IFlightRecorderService service;

    JFRConnection(ClientWriter cw, IConnectionDescriptor cd, List<IConnectionListener> listeners)
            throws Exception {
        this.cw = cw;
        this.rjmxConnection = attemptConnect(cd);
        this.handle =
                new DefaultConnectionHandle(
                        rjmxConnection,
                        "RJMX Connection",
                        listeners.toArray(new IConnectionListener[0]));
        this.service = new FlightRecorderServiceFactory().getServiceInstance(handle);
    }

    JFRConnection(ClientWriter cw, IConnectionDescriptor cd) throws Exception {
        this(cw, cd, List.of());
    }

    public IConnectionHandle getHandle() {
        return this.handle;
    }

    public IFlightRecorderService getService() {
        return this.service;
    }

    public TemplateService getTemplateService() {
        return new RemoteTemplateService(this);
    }

    public long getApproximateServerTime(Clock clock) {
        return this.rjmxConnection.getApproximateServerTime(clock.getWallTime());
    }

    public JMXServiceURL getJMXURL() throws IOException {
        return this.rjmxConnection.getConnectionDescriptor().createJMXServiceURL();
    }

    public String getHost() {
        try {
            return ConnectionToolkit.getHostName(
                    this.rjmxConnection.getConnectionDescriptor().createJMXServiceURL());
        } catch (IOException e) {
            cw.println(e);
            return "unknown";
        }
    }

    public int getPort() {
        try {
            return ConnectionToolkit.getPort(
                    this.rjmxConnection.getConnectionDescriptor().createJMXServiceURL());
        } catch (IOException e) {
            cw.println(e);
            return 0;
        }
    }

    public boolean isV1() {
        return !isV2();
    }

    public boolean isV2() {
        return FlightRecorderServiceV2.isAvailable(this.handle);
    }

    public void disconnect() {
        this.rjmxConnection.close();
    }

    @Override
    public void close() {
        this.disconnect();
    }

    private RJMXConnection attemptConnect(IConnectionDescriptor cd) throws Exception {
        try {
            RJMXConnection conn =
                    new RJMXConnection(cd, new ServerDescriptor(), JFRConnection::failConnection);
            if (!conn.connect()) {
                failConnection();
            }
            return conn;
        } catch (Exception e) {
            cw.println("connection attempt failed.");
            throw e;
        }
    }

    private static void failConnection() {
        throw new RuntimeException("Connection Failed");
    }
}
