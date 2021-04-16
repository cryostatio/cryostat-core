/*-
 * #%L
 * Cryostat Core
 * %%
 * Copyright (C) 2020 - 2021 The Cryostat Authors
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
package io.cryostat.core.net;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import javax.management.remote.JMXServiceURL;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.openjdk.jmc.rjmx.internal.WrappedConnectionException;

import io.cryostat.core.sys.Environment;
import io.cryostat.core.sys.FileSystem;
import io.cryostat.core.tui.ClientWriter;

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