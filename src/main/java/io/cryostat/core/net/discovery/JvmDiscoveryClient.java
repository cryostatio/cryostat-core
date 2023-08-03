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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.openjdk.jmc.jdp.client.DiscoveryEvent;
import org.openjdk.jmc.jdp.client.DiscoveryListener;
import org.openjdk.jmc.jdp.client.JDPClient;

import io.cryostat.core.log.Logger;

public class JvmDiscoveryClient {

    private final Logger logger;
    private final JDPClient jdp;
    private final Set<Consumer<JvmDiscoveryEvent>> eventListeners;
    private final DiscoveryListener listener;

    // package-private for testing
    JvmDiscoveryClient(JDPClient jdp, Logger logger) {
        this.jdp = jdp;
        this.logger = logger;
        this.eventListeners = new HashSet<>();
        this.listener =
                new DiscoveryListener() {
                    @Override
                    public void onDiscovery(DiscoveryEvent evt) {
                        DiscoveredJvmDescriptor desc =
                                new DiscoveredJvmDescriptor(evt.getDiscoverable().getPayload());
                        switch (evt.getKind()) {
                            case FOUND:
                                eventListeners.forEach(
                                        c ->
                                                c.accept(
                                                        new JvmDiscoveryEvent(
                                                                EventKind.FOUND, desc)));
                                break;
                            case LOST:
                                eventListeners.forEach(
                                        c -> c.accept(new JvmDiscoveryEvent(EventKind.LOST, desc)));
                                break;
                            case CHANGED:
                                eventListeners.forEach(
                                        c ->
                                                c.accept(
                                                        new JvmDiscoveryEvent(
                                                                EventKind.MODIFIED, desc)));
                                break;
                            default:
                                logger.error(
                                        new IllegalArgumentException(evt.getKind().toString()));
                        }
                    }
                };
    }

    public JvmDiscoveryClient(Logger logger) {
        this(new JDPClient(), logger);
    }

    public void start() throws IOException {
        this.logger.info("JDP Discovery started");
        this.jdp.addDiscoveryListener(listener);
        this.jdp.start();
    }

    public void stop() {
        this.jdp.stop();
        this.jdp.removeDiscoveryListener(listener);
        this.logger.info("JDP Discovery stopped");
    }

    public void addListener(Consumer<JvmDiscoveryEvent> listener) {
        this.eventListeners.add(listener);
    }

    public boolean removeListener(Consumer<JvmDiscoveryEvent> listener) {
        return this.eventListeners.remove(listener);
    }

    public List<DiscoveredJvmDescriptor> getDiscoveredJvmDescriptors() {
        return this.jdp.getDiscoverables().stream()
                .map(d -> new DiscoveredJvmDescriptor(d.getPayload()))
                .collect(Collectors.toList());
    }

    public static class JvmDiscoveryEvent {
        private final EventKind eventKind;
        private final DiscoveredJvmDescriptor discoveredJvmDescriptor;

        JvmDiscoveryEvent(EventKind eventKind, DiscoveredJvmDescriptor discoveredJvmDescriptor) {
            this.eventKind = eventKind;
            this.discoveredJvmDescriptor = discoveredJvmDescriptor;
        }

        public EventKind getEventKind() {
            return eventKind;
        }

        public DiscoveredJvmDescriptor getJvmDescriptor() {
            return discoveredJvmDescriptor;
        }
    }

    public enum EventKind {
        FOUND,
        LOST,
        MODIFIED;
    }
}
