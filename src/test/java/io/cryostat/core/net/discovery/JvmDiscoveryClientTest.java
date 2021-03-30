/*-
 * #%L
 * Cryostat Core
 * %%
 * Copyright (C) 2021 Red Hat, Inc.
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
package io.cryostat.core.net.discovery;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.openjdk.jmc.jdp.client.Discoverable;
import org.openjdk.jmc.jdp.client.DiscoveryEvent;
import org.openjdk.jmc.jdp.client.DiscoveryListener;
import org.openjdk.jmc.jdp.client.JDPClient;

import io.cryostat.core.log.Logger;
import io.cryostat.core.net.discovery.JvmDiscoveryClient.EventKind;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class JvmDiscoveryClientTest {

    JvmDiscoveryClient client;
    @Mock JDPClient jdp;
    @Mock Logger logger;

    @BeforeEach
    void setup() {
        this.client = new JvmDiscoveryClient(jdp, logger);
    }

    @Test
    void shouldNotStartJDPOnInstantiation() {
        Mockito.verifyNoInteractions(jdp);
    }

    @Test
    void shouldStartJDPAndAddListenerOnStart() throws IOException {
        client.start();

        InOrder inOrder = Mockito.inOrder(jdp);
        inOrder.verify(jdp).addDiscoveryListener(Mockito.any());
        inOrder.verify(jdp).start();
    }

    @Test
    void shouldRemoveListenerAndStopJDPOnStop() throws IOException {
        client.stop();

        InOrder inOrder = Mockito.inOrder(jdp);
        inOrder.verify(jdp).stop();
        inOrder.verify(jdp).removeDiscoveryListener(Mockito.any());
    }

    @Test
    void shouldAddAndRemoveSameListenerInstance() throws IOException {
        ArgumentCaptor<DiscoveryListener> startCaptor =
                ArgumentCaptor.forClass(DiscoveryListener.class);
        ArgumentCaptor<DiscoveryListener> stopCaptor =
                ArgumentCaptor.forClass(DiscoveryListener.class);

        client.start();
        Mockito.verify(jdp).addDiscoveryListener(startCaptor.capture());

        client.stop();
        Mockito.verify(jdp).removeDiscoveryListener(stopCaptor.capture());

        MatcherAssert.assertThat(
                startCaptor.getValue(), Matchers.sameInstance(stopCaptor.getValue()));
    }

    @Test
    void shouldReturnJDPDiscoveredTargetList() {
        Discoverable d1 =
                new TestDiscoverable(
                        "com.example.Foo", "service:jmx:rmi:///jndi/rmi://foo:9091/jmxrmi");
        Discoverable d2 =
                new TestDiscoverable(
                        "com.example.Bar", "service:jmx:rmi:///jndi/rmi://bar:9091/jmxrmi");
        Mockito.when(jdp.getDiscoverables()).thenReturn(Set.of(d1, d2));

        List<DiscoveredJvmDescriptor> descriptors = client.getDiscoveredJvmDescriptors();

        Mockito.verify(jdp).getDiscoverables();
        MatcherAssert.assertThat(
                new HashSet<>(descriptors),
                Matchers.equalTo(
                        Set.of(
                                new DiscoveredJvmDescriptor(d1.getPayload()),
                                new DiscoveredJvmDescriptor(d2.getPayload()))));
    }

    @Nested
    class EventListeners {

        List<EventKind> kinds;
        List<DiscoveredJvmDescriptor> jvms;

        @BeforeEach
        void setup() throws IOException {
            kinds = new ArrayList<>();
            jvms = new ArrayList<>();

            client.addListener(
                    e -> {
                        kinds.add(e.getEventKind());
                        jvms.add(e.getJvmDescriptor());
                    });
            client.start();
        }

        @Test
        void testFoundEvent() throws IOException {
            ArgumentCaptor<DiscoveryListener> listenerCaptor =
                    ArgumentCaptor.forClass(DiscoveryListener.class);
            Mockito.verify(jdp).addDiscoveryListener(listenerCaptor.capture());
            DiscoveryListener listener = listenerCaptor.getValue();

            TestDiscoverable discoverable =
                    new TestDiscoverable(
                            "com.example.Foo", "service:jmx:rmi:///jndi/rmi://foo:9091/jmxrmi");
            listener.onDiscovery(new DiscoveryEvent(DiscoveryEvent.Kind.FOUND, discoverable));

            MatcherAssert.assertThat(kinds, Matchers.equalTo(List.of(EventKind.FOUND)));
            MatcherAssert.assertThat(
                    jvms,
                    Matchers.equalTo(
                            List.of(new DiscoveredJvmDescriptor(discoverable.getPayload()))));
        }

        @Test
        void testLostEvent() throws IOException {
            ArgumentCaptor<DiscoveryListener> listenerCaptor =
                    ArgumentCaptor.forClass(DiscoveryListener.class);
            Mockito.verify(jdp).addDiscoveryListener(listenerCaptor.capture());
            DiscoveryListener listener = listenerCaptor.getValue();

            TestDiscoverable discoverable =
                    new TestDiscoverable(
                            "com.example.Foo", "service:jmx:rmi:///jndi/rmi://foo:9091/jmxrmi");
            listener.onDiscovery(new DiscoveryEvent(DiscoveryEvent.Kind.LOST, discoverable));

            MatcherAssert.assertThat(kinds, Matchers.equalTo(List.of(EventKind.LOST)));
            MatcherAssert.assertThat(
                    jvms,
                    Matchers.equalTo(
                            List.of(new DiscoveredJvmDescriptor(discoverable.getPayload()))));
        }

        @Test
        void testChangedEvent() throws IOException {
            ArgumentCaptor<DiscoveryListener> listenerCaptor =
                    ArgumentCaptor.forClass(DiscoveryListener.class);
            Mockito.verify(jdp).addDiscoveryListener(listenerCaptor.capture());
            DiscoveryListener listener = listenerCaptor.getValue();

            TestDiscoverable discoverable =
                    new TestDiscoverable(
                            "com.example.Foo", "service:jmx:rmi:///jndi/rmi://foo:9091/jmxrmi");
            listener.onDiscovery(new DiscoveryEvent(DiscoveryEvent.Kind.CHANGED, discoverable));

            MatcherAssert.assertThat(
                    kinds, Matchers.equalTo(List.of(EventKind.LOST, EventKind.FOUND)));
            MatcherAssert.assertThat(
                    jvms,
                    Matchers.equalTo(
                            List.of(
                                    new DiscoveredJvmDescriptor(discoverable.getPayload()),
                                    new DiscoveredJvmDescriptor(discoverable.getPayload()))));
        }
    }

    static class TestDiscoverable implements Discoverable {
        final String mainClass;
        final String serviceUrl;

        TestDiscoverable(String mainClass, String serviceUrl) {
            this.mainClass = mainClass;
            this.serviceUrl = serviceUrl;
        }

        @Override
        public String getSessionId() {
            return "unused";
        }

        @Override
        public Map<String, String> getPayload() {
            return Map.of("MAIN_CLASS", mainClass, "JMX_SERVICE_URL", serviceUrl);
        }

        @Override
        public int hashCode() {
            return getPayload().hashCode();
        }

        @Override
        public boolean equals(Object o) {
            if (o == null) {
                return false;
            }
            if (o == this) {
                return true;
            }
            if (o.getClass() != getClass()) {
                return false;
            }
            TestDiscoverable td = (TestDiscoverable) o;
            return Objects.equals(td.mainClass, mainClass)
                    && Objects.equals(td.serviceUrl, serviceUrl);
        }
    }
}
