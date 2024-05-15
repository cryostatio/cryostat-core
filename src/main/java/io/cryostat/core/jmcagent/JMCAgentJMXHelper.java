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
package io.cryostat.core.jmcagent;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.management.InstanceNotFoundException;
import javax.management.MBeanException;
import javax.management.MBeanServerConnection;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.ReflectionException;

import org.openjdk.jmc.rjmx.common.IConnectionHandle;

public class JMCAgentJMXHelper {

    private static final Logger logger = Logger.getLogger(JMCAgentJMXHelper.class.getName());
    private static final String AGENT_OBJECT_NAME =
            "org.openjdk.jmc.jfr.agent:type=AgentController";
    private static final String DEFINE_EVENT_PROBES = "defineEventProbes";
    private static final String RETRIEVE_EVENT_PROBES = "retrieveEventProbes";
    private static final String RETRIEVE_CURRENT_TRANSFORMS = "retrieveCurrentTransforms";

    private final IConnectionHandle connectionHandle;
    private final MBeanServerConnection mbsc;

    public JMCAgentJMXHelper(IConnectionHandle connectionHandle) {
        this.connectionHandle = connectionHandle;
        mbsc = connectionHandle.getServiceOrDummy(MBeanServerConnection.class);
    }

    public IConnectionHandle getConnectionHandle() {
        return connectionHandle;
    }

    protected MBeanServerConnection getMBeanServerConnection() {
        return mbsc;
    }

    public boolean isMXBeanRegistered() throws MalformedObjectNameException, IOException {
        try {
            return mbsc.isRegistered(new ObjectName(AGENT_OBJECT_NAME));
        } catch (MalformedObjectNameException | IOException e) {
            logger.log(Level.SEVERE, "Could not check if agent MXBean is registered", e);
            throw e;
        }
    }

    public String retrieveEventProbes() throws MBeanRetrieveException {
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
            throw new MBeanRetrieveException(e);
        }
    }

    public Object retrieveCurrentTransforms() throws MBeanRetrieveException {
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
            throw new MBeanRetrieveException(e);
        }
    }

    public void defineEventProbes(String xmlDescription) throws ProbeDefinitionException {
        try {
            Object[] params = {xmlDescription};
            String[] signature = {String.class.getName()};
            mbsc.invoke(new ObjectName(AGENT_OBJECT_NAME), DEFINE_EVENT_PROBES, params, signature);
        } catch (InstanceNotFoundException
                | MalformedObjectNameException
                | MBeanException
                | ReflectionException
                | IOException e) {
            throw new ProbeDefinitionException("Could not define event probes", e);
        }
    }

    @Override
    protected final void finalize() {}

    public static class ProbeDefinitionException extends Exception {
        ProbeDefinitionException(String message, Throwable e) {
            super(message, e);
        }
    }

    public static class MBeanRetrieveException extends Exception {
        MBeanRetrieveException(Throwable e) {
            super(e);
        }
    }
}
