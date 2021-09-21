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
package io.cryostat.core.agent;

import java.io.IOException;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.management.MBeanServerConnection;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import org.openjdk.jmc.rjmx.ConnectionException;
import org.openjdk.jmc.rjmx.IConnectionHandle;
import org.openjdk.jmc.rjmx.IConnectionListener;
import org.openjdk.jmc.rjmx.IServerHandle;

public class AgentJMXHelper {

    private static final Logger logger = Logger.getLogger(AgentJMXHelper.class.getName());
    private static final String AGENT_OBJECT_NAME =
            "org.openjdk.jmc.jfr.agent:type=AgentController"; //$NON-NLS-1$
    private static final String DEFINE_EVENT_PROBES = "defineEventProbes"; // $NON-NLS-1$
    private static final String RETRIEVE_EVENT_PROBES = "retrieveEventProbes"; // $NON-NLS-1$
    private static final String RETRIEVE_CURRENT_TRANSFORMS =
            "retrieveCurrentTransforms"; //$NON-NLS-1$
    private static final String CONNECTION_USAGE = "Agent MBean"; // $NON-NLS-1$

    private final IServerHandle serverHandle;
    private final IConnectionHandle connectionHandle;
    private final MBeanServerConnection mbsc;

    public AgentJMXHelper(IServerHandle serverHandle) throws ConnectionException {
        this.serverHandle = Objects.requireNonNull(serverHandle);
        connectionHandle = serverHandle.connect(CONNECTION_USAGE, this::onConnectionChange);
        mbsc = connectionHandle.getServiceOrDummy(MBeanServerConnection.class);
    }

    public IServerHandle getServerHandle() {
        return serverHandle;
    }

    public IConnectionHandle getConnectionHandle() {
        return connectionHandle;
    }

    public MBeanServerConnection getMBeanServerConnection() {
        return mbsc;
    }

    public void addConnectionChangedListener(IConnectionListener connectionListener) {
        // connectionListeners.add(Objects.requireNonNull(connectionListener));
    }

    public void removeConnectionChangedListener(IConnectionListener connectionListener) {
        // connectionListeners.remove(connectionListener);
    }

    public boolean isLocalJvm() {
        return connectionHandle.getServerDescriptor().getJvmInfo() != null;
    }

    public boolean isMXBeanRegistered() {
        try {
            return mbsc.isRegistered(new ObjectName(AGENT_OBJECT_NAME));
        } catch (MalformedObjectNameException | IOException e) {
            logger.log(Level.SEVERE, "Could not check if agent MXBean is registered", e);
        }
        return false;
    }

    public String retrieveEventProbes() {
        try {
            Object result =
                    mbsc.invoke(
                            new ObjectName(AGENT_OBJECT_NAME),
                            RETRIEVE_EVENT_PROBES,
                            new Object[0],
                            new String[0]);
            return result.toString();
        } catch (Exception e) {
            logger.log(Level.WARNING, "Could not retrieve event probes", e);
        }
        return null;
    }

    public Object retrieveCurrentTransforms() {
        try {
            Object result =
                    mbsc.invoke(
                            new ObjectName(AGENT_OBJECT_NAME),
                            RETRIEVE_CURRENT_TRANSFORMS,
                            new Object[0],
                            new String[0]);
            return result;
        } catch (Exception e) {
            logger.log(Level.WARNING, "Could not retrieve current transforms", e);
        }
        return null;
    }

    public void defineEventProbes(String xmlDescription) {
        try {
            Object[] params = {xmlDescription};
            String[] signature = {String.class.getName()};
            mbsc.invoke(new ObjectName(AGENT_OBJECT_NAME), DEFINE_EVENT_PROBES, params, signature);
        } catch (Exception e) {
            logger.log(Level.WARNING, "Could not define event probes: " + xmlDescription, e);
        }
    }

    public void onConnectionChange(IConnectionHandle connection) {
        // for (IConnectionListener listener : connectionListeners) {
        //	listener.onConnectionChange(connection);
        // }
    }
}
