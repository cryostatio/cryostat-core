/*
 * Copyright the Cryostat Authors
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
                                        c -> c.accept(new JvmDiscoveryEvent(EventKind.LOST, desc)));
                                eventListeners.forEach(
                                        c ->
                                                c.accept(
                                                        new JvmDiscoveryEvent(
                                                                EventKind.FOUND, desc)));
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
        ;
    }
}
