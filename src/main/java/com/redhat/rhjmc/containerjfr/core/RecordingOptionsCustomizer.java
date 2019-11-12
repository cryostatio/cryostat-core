package com.redhat.rhjmc.containerjfr.core;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Callable;

import com.redhat.rhjmc.containerjfr.core.net.JFRConnection;

import org.openjdk.jmc.common.unit.IConstrainedMap;
import org.openjdk.jmc.flightrecorder.configuration.recording.RecordingOptionsBuilder;

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
