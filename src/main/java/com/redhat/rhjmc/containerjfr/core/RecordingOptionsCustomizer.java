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
package com.redhat.rhjmc.containerjfr.core;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Callable;

import org.openjdk.jmc.common.unit.IConstrainedMap;
import org.openjdk.jmc.flightrecorder.configuration.recording.RecordingOptionsBuilder;

import com.redhat.rhjmc.containerjfr.core.net.JFRConnection;

public class RecordingOptionsCustomizer {

    private final Map<OptionKey, String> customizers;
    private final Callable<RecordingOptionsBuilder> builderProvider;

    public RecordingOptionsCustomizer(JFRConnection connection) {
        this(() -> new RecordingOptionsBuilder(connection.getService()));
    }

    // testing-only constructor
    RecordingOptionsCustomizer(Callable<RecordingOptionsBuilder> builderProvider) {
        this.customizers = new HashMap<>();
        this.builderProvider = builderProvider;
    }

    public RecordingOptionsCustomizer set(OptionKey key, String value) {
        customizers.put(key, value);
        return this;
    }

    public RecordingOptionsCustomizer unset(OptionKey key) {
        customizers.remove(key);
        return this;
    }

    public IConstrainedMap<String> asMap() throws FlightRecorderException {
        try {
            RecordingOptionsBuilder builder = builderProvider.call();
            for (Map.Entry<OptionKey, String> entry : customizers.entrySet()) {
                builder.addByKey(entry.getKey().getOptionName(), entry.getValue());
            }
            return builder.build();
        } catch (Exception e) {
            throw new FlightRecorderException(e);
        }
    }

    public enum OptionKey {
        NAME("name"),
        DURATION("duration"),
        MAX_AGE("maxAge"),
        MAX_SIZE("maxSize"),
        TO_DISK("toDisk"),
        ;

        private final String name;

        OptionKey(String name) {
            this.name = name;
        }

        public String getOptionName() {
            return this.name;
        }

        public static Optional<OptionKey> fromOptionName(String optionName) {
            OptionKey key = null;
            for (OptionKey k : OptionKey.values()) {
                if (k.name.equals(optionName)) {
                    key = k;
                }
            }
            return Optional.ofNullable(key);
        }
    }
}
