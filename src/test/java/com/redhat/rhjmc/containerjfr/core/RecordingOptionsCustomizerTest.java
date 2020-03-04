package com.redhat.rhjmc.containerjfr.core;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import java.util.Optional;
import java.util.UUID;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import org.openjdk.jmc.flightrecorder.configuration.recording.RecordingOptionsBuilder;

import com.redhat.rhjmc.containerjfr.core.RecordingOptionsCustomizer.OptionKey;
import com.redhat.rhjmc.containerjfr.core.net.JFRConnection;

@ExtendWith(MockitoExtension.class)
class RecordingOptionsCustomizerTest {

    RecordingOptionsCustomizer customizer;
    @Mock JFRConnection connection;
    @Mock RecordingOptionsBuilder builder;

    @BeforeEach
    void setup() {
        customizer = new RecordingOptionsCustomizer(() -> builder);
    }

    @ParameterizedTest
    @EnumSource(RecordingOptionsCustomizer.OptionKey.class)
    void shouldApplyKeysAndValues(RecordingOptionsCustomizer.OptionKey option) throws Exception {
        String value = String.valueOf(option.hashCode()); // select some arbitrary value to assign
        customizer.set(option, value);
        customizer.asMap();
        verify(builder).addByKey(option.getOptionName(), value);
    }

    @Test
    void shouldMutateAndUndoChangesInternally() throws Exception {
        customizer.set(RecordingOptionsCustomizer.OptionKey.MAX_AGE, "123");
        customizer.set(RecordingOptionsCustomizer.OptionKey.MAX_AGE, "456");
        customizer.set(RecordingOptionsCustomizer.OptionKey.MAX_AGE, "257");
        customizer.asMap();
        InOrder inOrder = Mockito.inOrder(builder);
        inOrder.verify(builder)
                .addByKey(RecordingOptionsCustomizer.OptionKey.MAX_AGE.getOptionName(), "257");
        inOrder.verify(builder).build();
        verifyNoMoreInteractions(builder);
    }

    @Test
    void shouldUnset() throws Exception {
        customizer.set(RecordingOptionsCustomizer.OptionKey.MAX_SIZE, "123");
        customizer.unset(RecordingOptionsCustomizer.OptionKey.MAX_SIZE);
        customizer.asMap();
        verify(builder).build();
        verifyNoMoreInteractions(builder);
    }

    @Test
    void shouldPropagateBuilderException() throws Exception {
        Assertions.assertThrows(
                FlightRecorderException.class,
                () -> {
                    new RecordingOptionsCustomizer(
                                    () -> {
                                        throw new IllegalArgumentException("foo");
                                    })
                            .set(OptionKey.NAME, "recordingName")
                            .asMap();
                });
    }

    @Test
    void shouldBeSelectableFromOptionName() {
        Optional<OptionKey> key = OptionKey.fromOptionName("toDisk");
        Assertions.assertTrue(key.isPresent());
        MatcherAssert.assertThat(key.get(), Matchers.is(OptionKey.TO_DISK));
    }

    @Test
    void shouldReturnEmptyIfOptionNameUnrecognized() {
        Optional<OptionKey> key = OptionKey.fromOptionName(UUID.randomUUID().toString());
        Assertions.assertFalse(key.isPresent());
    }
}
