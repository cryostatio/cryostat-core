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
package io.cryostat.core;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import org.openjdk.jmc.common.unit.QuantityConversionException;
import org.openjdk.jmc.flightrecorder.configuration.recording.RecordingOptionsBuilder;

import io.cryostat.core.tui.ClientWriter;
import io.cryostat.core.util.CheckedConsumer;

public class RecordingOptionsCustomizer
        implements Function<RecordingOptionsBuilder, RecordingOptionsBuilder> {

    private final Map<OptionKey, CustomizerConsumer> customizers;
    private final ClientWriter cw;

    public RecordingOptionsCustomizer(ClientWriter cw) {
        this.customizers = new HashMap<>();
        this.cw = cw;
    }

    @Override
    public RecordingOptionsBuilder apply(RecordingOptionsBuilder builder) {
        this.customizers.values().forEach(c -> c.accept(builder));
        return builder;
    }

    public void set(OptionKey key, String value) {
        CustomizerConsumer consumer = key.mapper.apply(value);
        consumer.setClientWriter(cw);
        customizers.put(key, consumer);
    }

    public void unset(OptionKey key) {
        customizers.remove(key);
    }

    public enum OptionKey {
        MAX_AGE(
                "maxAge",
                v ->
                        new CustomizerConsumer() {
                            @Override
                            public void acceptThrows(RecordingOptionsBuilder t)
                                    throws NumberFormatException, QuantityConversionException {
                                t.maxAge(Long.parseLong(v));
                            }
                        }),
        MAX_SIZE(
                "maxSize",
                v ->
                        new CustomizerConsumer() {
                            @Override
                            public void acceptThrows(RecordingOptionsBuilder t)
                                    throws NumberFormatException, QuantityConversionException {
                                t.maxSize(Long.parseLong(v));
                            }
                        }),
        TO_DISK(
                "toDisk",
                v ->
                        new CustomizerConsumer() {
                            @Override
                            public void acceptThrows(RecordingOptionsBuilder t)
                                    throws NumberFormatException, QuantityConversionException {
                                t.toDisk(Boolean.parseBoolean(v));
                            }
                        }),
        ;

        private final String name;
        private final Function<String, CustomizerConsumer> mapper;

        OptionKey(String name, Function<String, CustomizerConsumer> mapper) {
            this.name = name;
            this.mapper = mapper;
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

    private abstract static class CustomizerConsumer
            implements CheckedConsumer<RecordingOptionsBuilder> {
        private Optional<ClientWriter> cw = Optional.empty();

        void setClientWriter(ClientWriter cw) {
            this.cw = Optional.of(cw);
        }

        @Override
        public void handleException(Exception e) {
            cw.ifPresent(w -> w.println(e));
        }
    }
}
