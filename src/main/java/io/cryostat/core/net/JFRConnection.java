/*
 * Copyright The Cryostat Authors
 *
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
 */
package io.cryostat.core.net;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;

import javax.management.ObjectName;
import javax.management.remote.JMXServiceURL;

import org.openjdk.jmc.rjmx.ConnectionException;
import org.openjdk.jmc.rjmx.ConnectionToolkit;
import org.openjdk.jmc.rjmx.IConnectionDescriptor;
import org.openjdk.jmc.rjmx.IConnectionHandle;
import org.openjdk.jmc.rjmx.IConnectionListener;
import org.openjdk.jmc.rjmx.ServiceNotAvailableException;
import org.openjdk.jmc.rjmx.internal.DefaultConnectionHandle;
import org.openjdk.jmc.rjmx.internal.RJMXConnection;
import org.openjdk.jmc.rjmx.internal.ServerDescriptor;
import org.openjdk.jmc.rjmx.services.jfr.IFlightRecorderService;
import org.openjdk.jmc.rjmx.services.jfr.internal.FlightRecorderServiceFactory;
import org.openjdk.jmc.rjmx.services.jfr.internal.FlightRecorderServiceV2;
import org.openjdk.jmc.rjmx.subscription.MRI;
import org.openjdk.jmc.rjmx.subscription.MRI.Type;

import io.cryostat.core.sys.Clock;
import io.cryostat.core.sys.Environment;
import io.cryostat.core.sys.FileSystem;
import io.cryostat.core.templates.MergedTemplateService;
import io.cryostat.core.templates.TemplateService;
import io.cryostat.core.tui.ClientWriter;

import org.apache.commons.codec.digest.DigestUtils;

public class JFRConnection implements AutoCloseable {

    public static final int DEFAULT_PORT = 9091;

    protected final ClientWriter cw;
    protected final FileSystem fs;
    protected final Environment env;
    protected final FlightRecorderServiceFactory serviceFactory;
    protected final List<Runnable> closeListeners;
    protected RJMXConnection rjmxConnection;
    protected IConnectionHandle handle;
    protected IConnectionDescriptor connectionDescriptor;

    JFRConnection(
            ClientWriter cw,
            FileSystem fs,
            Environment env,
            IConnectionDescriptor cd,
            List<Runnable> listeners)
            throws ConnectionException {
        this.cw = cw;
        this.fs = fs;
        this.env = env;
        this.connectionDescriptor = cd;
        this.closeListeners = new ArrayList<>(listeners);
        this.serviceFactory = new FlightRecorderServiceFactory();
    }

    JFRConnection(ClientWriter cw, FileSystem fs, Environment env, IConnectionDescriptor cd)
            throws ConnectionException {
        this(cw, fs, env, cd, List.of());
    }

    public synchronized IConnectionHandle getHandle() {
        return this.handle;
    }

    public synchronized IFlightRecorderService getService()
            throws ConnectionException, IOException, ServiceNotAvailableException {
        if (!isConnected()) {
            connect();
        }
        IFlightRecorderService service = serviceFactory.getServiceInstance(handle);
        if (service == null || !isConnected()) {
            throw new ConnectionException(
                    String.format(
                            "Could not connect to remote target %s",
                            this.connectionDescriptor.createJMXServiceURL().toString()));
        }
        return service;
    }

    public TemplateService getTemplateService() {
        return new MergedTemplateService(this, fs, env);
    }

    public synchronized long getApproximateServerTime(Clock clock) {
        return this.rjmxConnection.getApproximateServerTime(clock.getWallTime());
    }

    public synchronized JMXServiceURL getJMXURL() throws IOException {
        return this.connectionDescriptor.createJMXServiceURL();
    }

    public synchronized String getHost() {
        try {
            return ConnectionToolkit.getHostName(
                    this.rjmxConnection.getConnectionDescriptor().createJMXServiceURL());
        } catch (IOException e) {
            cw.println(e);
            return "unknown";
        }
    }

    public synchronized int getPort() {
        try {
            return ConnectionToolkit.getPort(
                    this.rjmxConnection.getConnectionDescriptor().createJMXServiceURL());
        } catch (IOException e) {
            cw.println(e);
            return 0;
        }
    }

    public synchronized String getJvmId() throws Exception {
        if (!isConnected()) {
            connect();
        }
        ObjectName runtimeBean = new ObjectName("java.lang:type=Runtime");
        List<String> attrNames =
                new ArrayList<>(
                        Arrays.asList(
                                "ClassPath",
                                "Name",
                                "InputArguments",
                                "LibraryPath",
                                "VmVendor",
                                "VmVersion",
                                "StartTime"));

        ByteArrayOutputStream baos = new ByteArrayOutputStream(512);
        DataOutputStream dos = new DataOutputStream(baos);

        for (String attr : attrNames) {
            dos.writeUTF(
                    this.rjmxConnection
                            .getAttributeValue(new MRI(Type.ATTRIBUTE, runtimeBean, attr))
                            .toString());
        }
        byte[] hash = DigestUtils.sha256(baos.toByteArray());
        return new String(Base64.getUrlEncoder().encode(hash), StandardCharsets.UTF_8).trim();
    }

    public synchronized boolean isV1() {
        return !isV2();
    }

    public synchronized boolean isV2() {
        return FlightRecorderServiceV2.isAvailable(this.handle);
    }

    public synchronized boolean isConnected() {
        return this.rjmxConnection != null && this.rjmxConnection.isConnected();
    }

    public synchronized void connect() throws ConnectionException {
        if (isConnected()) {
            return;
        }
        this.rjmxConnection = attemptConnect(connectionDescriptor);
        this.handle =
                new DefaultConnectionHandle(
                        rjmxConnection,
                        "RJMX Connection",
                        closeListeners.stream()
                                .map(
                                        l ->
                                                new IConnectionListener() {
                                                    @Override
                                                    public void onConnectionChange(
                                                            IConnectionHandle arg0) {
                                                        l.run();
                                                    }
                                                })
                                .collect(Collectors.toList())
                                .toArray(new IConnectionListener[0]));
    }

    public synchronized void disconnect() {
        try {
            if (this.handle != null) {
                this.handle.close();
            }
        } catch (IOException e) {
            cw.println(e);
        } finally {
            if (this.rjmxConnection != null) {
                this.rjmxConnection.close();
            }
        }
    }

    @Override
    public synchronized void close() {
        this.disconnect();
    }

    protected synchronized RJMXConnection attemptConnect(IConnectionDescriptor cd)
            throws ConnectionException {
        try {
            RJMXConnection conn =
                    new RJMXConnection(cd, new ServerDescriptor(), JFRConnection::failConnection);
            if (!conn.connect()) {
                failConnection();
            }
            return conn;
        } catch (ConnectionException e) {
            cw.println("connection attempt failed.");
            closeListeners.forEach(Runnable::run);
            throw e;
        }
    }

    protected static void failConnection() {
        throw new ConnectionFailureException("Connection Failed");
    }

    static class ConnectionFailureException extends RuntimeException {
        public ConnectionFailureException(String message) {
            super(message);
        }
    }
}
