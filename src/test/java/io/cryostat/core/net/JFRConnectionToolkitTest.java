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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import javax.management.remote.JMXServiceURL;

import org.openjdk.jmc.rjmx.internal.WrappedConnectionException;

import io.cryostat.core.sys.Environment;
import io.cryostat.core.sys.FileSystem;
import io.cryostat.core.tui.ClientWriter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class JFRConnectionToolkitTest {

    JFRConnectionToolkit toolkit;
    @Mock ClientWriter cw;
    @Mock FileSystem fs;
    @Mock Environment env;

    @BeforeEach
    void setup() {
        toolkit = new JFRConnectionToolkit(cw, fs, env);
    }

    @ParameterizedTest
    @ValueSource(
            strings = {
                "service:jmx:rmi:///jndi/rmi://cryostat:9091/jmxrmi",
                "service:jmx:rmi:///jndi/rmi://localhost/jmxrmi"
            })
    void shouldThrowInTestEnvironment(String s) {
        assertThrows(
                WrappedConnectionException.class,
                () -> toolkit.connect(new JMXServiceURL(s)).connect());
    }

    @Test
    void shouldGetHostName() throws Exception {
        JMXServiceURL jmxServiceUrl = new JMXServiceURL("rmi", "localhost", 8080);
        assertEquals(toolkit.getHostName(jmxServiceUrl), "localhost");
    }

    @Test
    void shouldGetPort() throws Exception {
        JMXServiceURL jmxServiceUrl = new JMXServiceURL("rmi", "localhost", 8080);
        assertEquals(toolkit.getPort(jmxServiceUrl), 8080);
    }

    @Test
    void shouldCreateServiceURL() throws Exception {
        JMXServiceURL jmxServiceUrl =
                new JMXServiceURL("rmi", "", 0, "/jndi/rmi://localhost:8080/jmxrmi");
        assertTrue(toolkit.createServiceURL("localhost", 8080).equals(jmxServiceUrl));
    }

    @Test
    void shouldGetDefaultPort() {
        assertEquals(toolkit.getDefaultPort(), 7091);
    }
}
