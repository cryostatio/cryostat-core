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
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.management.InstanceNotFoundException;
import javax.management.MBeanException;
import javax.management.MBeanServerConnection;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.ReflectionException;

import org.openjdk.jmc.rjmx.ConnectionException;
import org.openjdk.jmc.rjmx.IConnectionHandle;

public class AgentJMXHelper {

    private static final Logger logger = Logger.getLogger(AgentJMXHelper.class.getName());
    private static final String AGENT_OBJECT_NAME =
            "org.openjdk.jmc.jfr.agent:type=AgentController";
    private static final String DEFINE_EVENT_PROBES = "defineEventProbes";
    private static final String RETRIEVE_EVENT_PROBES = "retrieveEventProbes";
    private static final String RETRIEVE_CURRENT_TRANSFORMS = "retrieveCurrentTransforms";

    private final IConnectionHandle connectionHandle;
    private final MBeanServerConnection mbsc;

    public AgentJMXHelper(IConnectionHandle connectionHandle) throws ConnectionException {
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

    static class ProbeDefinitionException extends Exception {
        ProbeDefinitionException(String message, Throwable e) {
            super(message, e);
        }
    }

    static class MBeanRetrieveException extends Exception {
        MBeanRetrieveException(Throwable e) {
            super(e);
        }
    }
}
