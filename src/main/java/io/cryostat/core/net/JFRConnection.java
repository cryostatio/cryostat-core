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

import javax.management.InstanceNotFoundException;
import javax.management.IntrospectionException;
import javax.management.ReflectionException;
import javax.management.remote.JMXServiceURL;

import org.openjdk.jmc.rjmx.ConnectionException;
import org.openjdk.jmc.rjmx.IConnectionHandle;
import org.openjdk.jmc.rjmx.ServiceNotAvailableException;

import io.cryostat.core.JvmIdentifier;
import io.cryostat.core.sys.Clock;
import io.cryostat.core.templates.TemplateService;

public interface JFRConnection extends AutoCloseable {

    public IConnectionHandle getHandle() throws ConnectionException, IOException;

    public CryostatFlightRecorderService getService()
            throws ConnectionException, IOException, ServiceNotAvailableException;

    public TemplateService getTemplateService();

    public long getApproximateServerTime(Clock clock);

    public JMXServiceURL getJMXURL() throws IOException;

    public String getHost();

    public int getPort();

    @Deprecated
    public default String getJvmId() throws IDException, IOException {
        return getJvmIdentifier().getHash();
    }

    public JvmIdentifier getJvmIdentifier() throws IDException, IOException;

    public MBeanMetrics getMBeanMetrics()
            throws ConnectionException, IOException, InstanceNotFoundException,
                    IntrospectionException, ReflectionException;

    public boolean isConnected();

    public void connect() throws ConnectionException;

    public void disconnect();
}
