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
