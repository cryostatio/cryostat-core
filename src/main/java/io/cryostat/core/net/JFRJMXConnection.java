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
import java.lang.management.MemoryUsage;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.management.AttributeNotFoundException;
import javax.management.InstanceNotFoundException;
import javax.management.IntrospectionException;
import javax.management.MBeanException;
import javax.management.ObjectName;
import javax.management.ReflectionException;
import javax.management.openmbean.CompositeData;
import javax.management.openmbean.TabularData;
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

import io.cryostat.core.net.MemoryMetrics.CustomMemoryUsage;
import io.cryostat.core.sys.Clock;
import io.cryostat.core.sys.Environment;
import io.cryostat.core.sys.FileSystem;
import io.cryostat.core.templates.MergedTemplateService;
import io.cryostat.core.templates.TemplateService;
import io.cryostat.core.tui.ClientWriter;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;

public class JFRJMXConnection implements JFRConnection {

    public static final int DEFAULT_PORT = 9091;

    protected final ClientWriter cw;
    protected final FileSystem fs;
    protected final Environment env;
    protected final FlightRecorderServiceFactory serviceFactory;
    protected final List<Runnable> closeListeners;
    protected RJMXConnection rjmxConnection;
    protected IConnectionHandle handle;
    protected IConnectionDescriptor connectionDescriptor;

    JFRJMXConnection(
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

    JFRJMXConnection(ClientWriter cw, FileSystem fs, Environment env, IConnectionDescriptor cd)
            throws ConnectionException {
        this(cw, fs, env, cd, List.of());
    }

    public synchronized IConnectionHandle getHandle() throws ConnectionException, IOException {
        if (!isConnected()) {
            connect();
        }
        IConnectionHandle handle = this.handle;
        if (handle == null || !isConnected()) {
            throw new ConnectionException(
                    String.format(
                            "Could not connect to remote target %s",
                            this.connectionDescriptor.createJMXServiceURL().toString()));
        }
        return handle;
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

    public synchronized String getJvmId() throws IDException, IOException {
        if (!isConnected()) {
            connect();
        }
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

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream(512);
                DataOutputStream dos = new DataOutputStream(baos)) {
            for (String attr : attrNames) {
                Object attrObject =
                        this.rjmxConnection.getAttributeValue(
                                new MRI(Type.ATTRIBUTE, ConnectionToolkit.RUNTIME_BEAN_NAME, attr));
                if (attrObject.getClass().isArray()) {
                    String stringified = stringifyArray(attrObject);
                    dos.writeUTF(stringified);
                } else {
                    dos.writeUTF(attrObject.toString());
                }
            }
            byte[] hash = DigestUtils.sha256(baos.toByteArray());
            return new String(Base64.getUrlEncoder().encode(hash), StandardCharsets.UTF_8).trim();
        } catch (AttributeNotFoundException
                | InstanceNotFoundException
                | MBeanException
                | ReflectionException e) {
            throw new IDException(e);
        }
    }

    private Map<String, Object> parseCompositeData(CompositeData compositeData) {
        Map<String, Object> map = new HashMap<>();
        for (String key : compositeData.getCompositeType().keySet()) {
            Object value = compositeData.get(key);
            if (value instanceof CompositeData) {
                map.put(key, parseCompositeData((CompositeData) value));
            } else {
                map.put(key, value);
            }
        }
        return map;
    }

    private Map<Object, Object> parseTabularData(TabularData tabularData) {
        Set<List<?>> keySet = (Set<List<?>>) tabularData.keySet();
        Map<Object, Object> tdMap = new HashMap<>();
        for (List<?> keys : keySet) {
            CompositeData compositeData = tabularData.get(keys.toArray());
            var cd = parseCompositeData(compositeData);
            if (keys.size() == 1 && cd.size() == 2) {
                Object actualKey = keys.get(0);
                Map<?, ?> valueMap = (Map<?, ?>) cd;
                if (valueMap.containsKey("key")
                        && valueMap.containsKey("value")
                        && valueMap.get("key").equals(actualKey)) {
                    tdMap.put(valueMap.get("key"), valueMap.get("value"));
                } else {
                    tdMap.put(keys.get(0), cd);
                }
            } else {
                tdMap.put(keys, cd);
            }
        }
        return tdMap;
    }

    private Object parseObject(Object obj) {
        if (obj instanceof CompositeData) {
            CompositeData cd = (CompositeData) obj;
            if (cd.getCompositeType().getTypeName().equals("java.lang.management.MemoryUsage"))
                return CustomMemoryUsage.fromMemoryUsage(MemoryUsage.from(cd));
            return parseCompositeData(cd);
        } else if (obj instanceof TabularData) {
            return parseTabularData((TabularData) obj);
        } else {
            return obj;
        }
    }

    private Object defaultValue(String type) {
        switch (type) {
            case "boolean":
                return null;
            case "int":
                return Integer.MIN_VALUE;
            case "long":
                return Long.MIN_VALUE;
            case "double":
                return Double.MIN_VALUE;
            case "float":
                return Float.MIN_VALUE;
            case "java.lang.String":
                return StringUtils.EMPTY;
            case "[Ljava.lang.String;":
                return new String[0];
            case "[J":
                return new long[0];
            case "javax.management.openmbean.CompositeData":
                return Collections.emptyMap();
            case "javax.management.openmbean.TabularData":
                return Collections.emptyMap();
            default:
                return null;
        }
    }

    private Map<String, Object> getAttributeMap(ObjectName beanName)
            throws InstanceNotFoundException, IntrospectionException, ReflectionException,
                    IOException {
        Map<String, Object> attrMap = new HashMap<>();

        var attrs = rjmxConnection.getMBeanInfo(beanName).getAttributes();

        for (var attr : attrs) {
            if (attr.isReadable() && !attr.getName().equals("ObjectName")) {
                try {
                    Object attrObject =
                            this.rjmxConnection.getAttributeValue(
                                    new MRI(Type.ATTRIBUTE, beanName, attr.getName()));
                    attrMap.put(attr.getName(), parseObject(attrObject));
                } catch (AttributeNotFoundException
                        | InstanceNotFoundException
                        | MBeanException
                        | ReflectionException
                        | IOException e) {
                    cw.println(
                            String.format(
                                    "Could not read attribute: [%s], message: [%s]",
                                    attr.getName(), e.getMessage()));
                    attrMap.put(attr.getName(), defaultValue(attr.getType()));
                }
            }
        }
        return attrMap;
    }

    public synchronized MBeanMetrics getMBeanMetrics()
            throws IOException, InstanceNotFoundException, IntrospectionException,
                    ReflectionException {
        if (!isConnected()) {
            connect();
        }

        Map<String, Object> runtimeMap = getAttributeMap(ConnectionToolkit.RUNTIME_BEAN_NAME);
        Map<String, Object> memoryMap = getAttributeMap(ConnectionToolkit.MEMORY_BEAN_NAME);
        Map<String, Object> threadMap = getAttributeMap(ConnectionToolkit.THREAD_BEAN_NAME);
        Map<String, Object> osMap = getAttributeMap(ConnectionToolkit.OPERATING_SYSTEM_BEAN_NAME);

        return new MBeanMetrics(
                new RuntimeMetrics(runtimeMap),
                new MemoryMetrics(memoryMap),
                new ThreadMetrics(threadMap),
                new OperatingSystemMetrics(osMap));
    }

    private String stringifyArray(Object arrayObject) {
        String stringified;
        String componentType = arrayObject.getClass().getComponentType().toString();
        switch (componentType) {
            case "boolean":
                stringified = Arrays.toString((boolean[]) arrayObject);
                break;

            case "byte":
                stringified = Arrays.toString((byte[]) arrayObject);
                break;

            case "char":
                stringified = Arrays.toString((char[]) arrayObject);
                break;

            case "short":
                stringified = Arrays.toString((short[]) arrayObject);
                break;

            case "int":
                stringified = Arrays.toString((int[]) arrayObject);
                break;

            case "long":
                stringified = Arrays.toString((long[]) arrayObject);
                break;

            case "float":
                stringified = Arrays.toString((float[]) arrayObject);
                break;

            case "double":
                stringified = Arrays.toString((double[]) arrayObject);
                break;

            default:
                stringified = Arrays.toString((Object[]) arrayObject);
        }
        return stringified;
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
                    new RJMXConnection(
                            cd, new ServerDescriptor(), JFRJMXConnection::failConnection);
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
