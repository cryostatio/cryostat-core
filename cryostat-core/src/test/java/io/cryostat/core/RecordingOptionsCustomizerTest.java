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

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import org.openjdk.jmc.common.unit.QuantityConversionException;
import org.openjdk.jmc.flightrecorder.configuration.recording.RecordingOptionsBuilder;

import io.cryostat.libcryostat.tui.ClientWriter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RecordingOptionsCustomizerTest {

    RecordingOptionsCustomizer customizer;
    @Mock ClientWriter cw;
    @Mock RecordingOptionsBuilder builder;

    @BeforeEach
    void setup() {
        customizer = new RecordingOptionsCustomizer(cw);
    }

    @Test
    void shouldApplyToDisk() throws QuantityConversionException {
        customizer.set(RecordingOptionsCustomizer.OptionKey.TO_DISK, "true");
        customizer.apply(builder);
        verify(builder).toDisk(true);
        verifyNoMoreInteractions(builder);
        verifyNoInteractions(cw);
    }

    @Test
    void shouldApplyMaxAge() throws QuantityConversionException {
        customizer.set(RecordingOptionsCustomizer.OptionKey.MAX_AGE, "123");
        customizer.apply(builder);
        verify(builder).maxAge(123);
        verifyNoMoreInteractions(builder);
        verifyNoInteractions(cw);
    }

    @Test
    void shouldMutateAndUndoChangesInternally() throws QuantityConversionException {
        customizer.set(RecordingOptionsCustomizer.OptionKey.MAX_AGE, "123");
        customizer.set(RecordingOptionsCustomizer.OptionKey.MAX_AGE, "456");
        customizer.apply(builder);
        verify(builder).maxAge(456);
        verifyNoMoreInteractions(builder);
        verifyNoInteractions(cw);
    }

    @Test
    void shouldApplyMaxSize() throws QuantityConversionException {
        customizer.set(RecordingOptionsCustomizer.OptionKey.MAX_SIZE, "123");
        customizer.apply(builder);
        verify(builder).maxSize(123);
        verifyNoMoreInteractions(builder);
        verifyNoInteractions(cw);
    }

    @Test
    void shouldUnset() throws QuantityConversionException {
        customizer.set(RecordingOptionsCustomizer.OptionKey.MAX_SIZE, "123");
        customizer.unset(RecordingOptionsCustomizer.OptionKey.MAX_SIZE);
        customizer.apply(builder);
        verifyNoMoreInteractions(builder);
        verifyNoInteractions(cw);
    }

    @Test
    void shouldPrintExceptions() throws QuantityConversionException {
        when(builder.maxSize(Mockito.anyLong())).thenThrow(UnsupportedOperationException.class);
        customizer.set(RecordingOptionsCustomizer.OptionKey.MAX_SIZE, "123");
        customizer.apply(builder);
        verify(cw).println(ArgumentMatchers.any(UnsupportedOperationException.class));
        verifyNoMoreInteractions(builder);
        verifyNoMoreInteractions(cw);
    }
}
