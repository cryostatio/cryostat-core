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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ExtendWith(MockitoExtension.class)
class JvmDiscoveryClientTest {

    JvmDiscoveryClient client;
    @Mock JDPClient jdp;
    @Mock Logger logger = LoggerFactory.getLogger(getClass());

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

            MatcherAssert.assertThat(kinds, Matchers.equalTo(List.of(EventKind.MODIFIED)));
            MatcherAssert.assertThat(
                    jvms,
                    Matchers.equalTo(
                            List.of(new DiscoveredJvmDescriptor(discoverable.getPayload()))));
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
