/*-
 * #%L
 * Container JFR Core
 * %%
 * Copyright (C) 2020 Red Hat, Inc.
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
package com.redhat.rhjmc.containerjfr.core.net.discovery;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.openjdk.jmc.jdp.client.DiscoveryEvent.Kind;
import org.openjdk.jmc.jdp.client.JDPClient;

import com.redhat.rhjmc.containerjfr.core.log.Logger;

public class JvmDiscoveryClient {

    private final Logger logger;
    private final JDPClient jdp;
    private final Set<Consumer<JvmDiscoveryEvent>> eventListeners;

    public JvmDiscoveryClient(Logger logger) {
        this.logger = logger;
        this.jdp = new JDPClient();
        this.eventListeners = new HashSet<>();

        this.jdp.addDiscoveryListener(
                evt -> {
                    DiscoveredJvmDescriptor desc =
                            new DiscoveredJvmDescriptor(evt.getDiscoverable().getPayload());
                    JvmDiscoveryEvent jde =
                            new JvmDiscoveryEvent(EventKind.fromJmcKind(evt.getKind()), desc);
                    eventListeners.forEach(c -> c.accept(jde));
                });
    }

    public void start() throws IOException {
        this.logger.info("JDP Discovery started");
        this.jdp.start();
    }

    public void stop() {
        this.logger.info("JDP Discovery stopped");
        this.jdp.stop();
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
        CHANGED,
        FOUND,
        LOST,
        ;

        private static EventKind fromJmcKind(Kind kind) {
            switch (kind) {
                case CHANGED:
                    return EventKind.CHANGED;
                case FOUND:
                    return EventKind.FOUND;
                case LOST:
                    return EventKind.LOST;
                default:
                    throw new IllegalArgumentException(kind.toString());
            }
        }
    }
}
