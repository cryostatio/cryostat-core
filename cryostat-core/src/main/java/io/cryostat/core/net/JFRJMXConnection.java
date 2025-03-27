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
import java.lang.management.MemoryUsage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javax.management.AttributeNotFoundException;
import javax.management.InstanceNotFoundException;
import javax.management.IntrospectionException;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanException;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.ReflectionException;
import javax.management.openmbean.CompositeData;
import javax.management.openmbean.TabularData;
import javax.management.remote.JMXServiceURL;

import org.openjdk.jmc.rjmx.common.ConnectionException;
import org.openjdk.jmc.rjmx.common.ConnectionToolkit;
import org.openjdk.jmc.rjmx.common.IConnectionDescriptor;
import org.openjdk.jmc.rjmx.common.IConnectionHandle;
import org.openjdk.jmc.rjmx.common.IConnectionListener;
import org.openjdk.jmc.rjmx.common.ServiceNotAvailableException;
import org.openjdk.jmc.rjmx.common.internal.DefaultConnectionHandle;
import org.openjdk.jmc.rjmx.common.internal.RJMXConnection;
import org.openjdk.jmc.rjmx.common.internal.ServerDescriptor;
import org.openjdk.jmc.rjmx.common.services.internal.AttributeStorageServiceFactory;
import org.openjdk.jmc.rjmx.common.services.internal.CommercialFeaturesServiceFactory;
import org.openjdk.jmc.rjmx.common.services.internal.DiagnosticCommandServiceFactory;
import org.openjdk.jmc.rjmx.common.services.internal.ServiceEntry;
import org.openjdk.jmc.rjmx.common.services.internal.SubscriptionServiceFactory;
import org.openjdk.jmc.rjmx.common.services.jfr.internal.FlightRecorderServiceFactory;
import org.openjdk.jmc.rjmx.common.services.jfr.internal.FlightRecorderServiceV2;
import org.openjdk.jmc.rjmx.common.subscription.MRI;
import org.openjdk.jmc.rjmx.common.subscription.MRI.Type;

import io.cryostat.core.templates.RemoteTemplateService;
import io.cryostat.core.templates.TemplateService;
import io.cryostat.libcryostat.JvmIdentifier;
import io.cryostat.libcryostat.net.IDException;
import io.cryostat.libcryostat.net.MBeanMetrics;
import io.cryostat.libcryostat.net.MemoryMetrics;
import io.cryostat.libcryostat.net.OperatingSystemMetrics;
import io.cryostat.libcryostat.net.RuntimeMetrics;
import io.cryostat.libcryostat.net.ThreadMetrics;
import io.cryostat.libcryostat.sys.Clock;
import io.cryostat.libcryostat.sys.Environment;
import io.cryostat.libcryostat.sys.FileSystem;
import io.cryostat.libcryostat.tui.ClientWriter;

public class JFRJMXConnection implements JFRConnection {

    public static final int DEFAULT_PORT = 9091;

    protected final ClientWriter cw;
    protected final FileSystem fs;
    protected final Environment env;
    protected final FlightRecorderServiceFactory serviceFactory;
    protected final List<Runnable> closeListeners;
    protected final List<ServiceEntry<?>> serviceEntries;
    protected RJMXConnection rjmxConnection;
    protected IConnectionHandle handle;
    protected IConnectionDescriptor connectionDescriptor;

    JFRJMXConnection(
            ClientWriter cw,
            FileSystem fs,
            Environment env,
            IConnectionDescriptor cd,
            List<Runnable> listeners) {
        this.cw = cw;
        this.fs = fs;
        this.env = env;
        this.connectionDescriptor = cd;
        this.closeListeners = new ArrayList<>(listeners);
        this.serviceFactory = new FlightRecorderServiceFactory();
        this.serviceEntries = new ArrayList<>();
        initializeServiceEntries();
    }

    private void initializeServiceEntries() {
        serviceEntries.add(
                new ServiceEntry<>(
                        new AttributeStorageServiceFactory(),
                        "Attribute Storage",
                        "Service for storing attribute values"));
        serviceEntries.add(
                new ServiceEntry<>(
                        new CommercialFeaturesServiceFactory(),
                        "Commercial Features",
                        "Service for checking and enabling the state of the commercial features"
                                + " in hotspot."));
        serviceEntries.add(
                new ServiceEntry<>(
                        new DiagnosticCommandServiceFactory(),
                        "Diagnostic Commands",
                        "Diagnostic Commands"));
        serviceEntries.add(
                new ServiceEntry<>(
                        new SubscriptionServiceFactory(),
                        "Subscription Engine",
                        "Service for controlling the client side attribute subscription"
                                + " engine"));
    }

    JFRJMXConnection(ClientWriter cw, FileSystem fs, Environment env, IConnectionDescriptor cd) {
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

    public synchronized CryostatFlightRecorderService getService()
            throws ConnectionException, IOException, ServiceNotAvailableException {
        return new JmxFlightRecorderService(this);
    }

    public TemplateService getTemplateService() {
        return new RemoteTemplateService(this);
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

    @Override
    public synchronized JvmIdentifier getJvmIdentifier() throws IDException, IOException {
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
        try {
            return JvmIdentifier.from(
                    new RuntimeMetrics(
                            getAttributeMap(
                                    ConnectionToolkit.RUNTIME_BEAN_NAME,
                                    m -> attrNames.contains(m.getName()))));
        } catch (ReflectionException | IntrospectionException | InstanceNotFoundException e) {
            throw new IDException(e);
        }
    }

    @Override
    public <T> T invokeMBeanOperation(
            String beanName,
            String operation,
            Object[] params,
            String[] signature,
            Class<T> returnType)
            throws MalformedObjectNameException,
                    InstanceNotFoundException,
                    MBeanException,
                    ReflectionException,
                    IOException,
                    ConnectionException {
        if (!isConnected()) {
            connect();
        }
        return (T)
                this.rjmxConnection
                        .getMBeanServer()
                        .invoke(ObjectName.getInstance(beanName), operation, params, signature);
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
            if (cd.getCompositeType().getTypeName().equals(MemoryUsage.class.getName()))
                return MemoryUsage.from(cd);
            return parseCompositeData(cd);
        } else if (obj instanceof TabularData) {
            return parseTabularData((TabularData) obj);
        } else {
            return obj;
        }
    }

    private Map<String, Object> getAttributeMap(ObjectName beanName)
            throws InstanceNotFoundException,
                    IntrospectionException,
                    ReflectionException,
                    IOException {
        return getAttributeMap(beanName, m -> true);
    }

    private Map<String, Object> getAttributeMap(
            ObjectName beanName, Predicate<MBeanAttributeInfo> attrPredicate)
            throws InstanceNotFoundException,
                    IntrospectionException,
                    ReflectionException,
                    IOException {
        Map<String, Object> attrMap = new HashMap<>();

        var attrs = rjmxConnection.getMBeanInfo(beanName).getAttributes();

        for (var attr : attrs) {
            if (attr.isReadable()
                    && !attr.getName().equals("ObjectName")
                    && attrPredicate.test(attr)) {
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
                }
            }
        }
        return attrMap;
    }

    public synchronized MBeanMetrics getMBeanMetrics()
            throws IOException,
                    InstanceNotFoundException,
                    IntrospectionException,
                    ReflectionException {
        if (!isConnected()) {
            connect();
        }

        Map<String, Object> runtimeMap = getAttributeMap(ConnectionToolkit.RUNTIME_BEAN_NAME);
        Map<String, Object> memoryMap = getAttributeMap(ConnectionToolkit.MEMORY_BEAN_NAME);
        Map<String, Object> threadMap = getAttributeMap(ConnectionToolkit.THREAD_BEAN_NAME);
        Map<String, Object> osMap = getAttributeMap(ConnectionToolkit.OPERATING_SYSTEM_BEAN_NAME);

        RuntimeMetrics runtimeMetrics = new RuntimeMetrics(runtimeMap);
        return new MBeanMetrics(
                runtimeMetrics,
                new MemoryMetrics(memoryMap),
                new ThreadMetrics(threadMap),
                new OperatingSystemMetrics(osMap),
                JvmIdentifier.from(runtimeMetrics).getHash());
    }

    public synchronized boolean isV1() throws ConnectionException, IOException {
        return !isV2();
    }

    public synchronized boolean isV2() throws ConnectionException, IOException {
        return FlightRecorderServiceV2.isAvailable(getHandle());
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
                                .toArray(new IConnectionListener[0]),
                        serviceEntries);
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

    public static class ConnectionFailureException extends RuntimeException {
        public ConnectionFailureException(String message) {
            super(message);
        }
    }
}
