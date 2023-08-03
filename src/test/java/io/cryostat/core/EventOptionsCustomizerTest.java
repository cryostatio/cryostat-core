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

import java.util.Collections;

import org.openjdk.jmc.common.unit.IConstrainedMap;
import org.openjdk.jmc.common.unit.IConstraint;
import org.openjdk.jmc.common.unit.IDescribedMap;
import org.openjdk.jmc.common.unit.IMutableConstrainedMap;
import org.openjdk.jmc.common.unit.IOptionDescriptor;
import org.openjdk.jmc.flightrecorder.configuration.events.EventOptionID;
import org.openjdk.jmc.flightrecorder.configuration.events.IEventTypeID;
import org.openjdk.jmc.rjmx.services.jfr.IEventTypeInfo;

import io.cryostat.core.EventOptionsCustomizer.EventOptionException;
import io.cryostat.core.EventOptionsCustomizer.EventTypeException;
import io.cryostat.core.EventOptionsCustomizer.OptionValueException;
import io.cryostat.core.net.CryostatFlightRecorderService;
import io.cryostat.core.net.JFRConnection;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EventOptionsCustomizerTest {

    @Mock JFRConnection connection;
    @Mock CryostatFlightRecorderService service;
    @Mock IDescribedMap<EventOptionID> defaultMap;
    @Mock IMutableConstrainedMap<EventOptionID> emptyMap;
    EventOptionsCustomizer customizer;

    @BeforeEach
    void setup() throws Exception {
        Mockito.when(connection.getService()).thenReturn(service);
        Mockito.when(service.getDefaultEventOptions()).thenReturn(defaultMap);
        Mockito.when(defaultMap.emptyWithSameConstraints()).thenReturn(emptyMap);
        customizer = new EventOptionsCustomizer(connection);
    }

    @Test
    void asMapShouldReturnExpectedMapCopy() throws FlightRecorderException {
        IMutableConstrainedMap copy = Mockito.mock(IMutableConstrainedMap.class);
        Mockito.when(emptyMap.mutableCopy()).thenReturn(EventOptionsCustomizer.capture(copy));

        IConstrainedMap<?> map = customizer.asMap();
        MatcherAssert.assertThat(map, Matchers.sameInstance(copy));
    }

    @Test
    void shouldThrowExceptionForUnknownEventType() throws Exception {
        Mockito.when(service.getAvailableEventTypes()).thenReturn(Collections.emptySet());
        Exception e =
                Assertions.assertThrows(
                        EventTypeException.class,
                        () -> {
                            customizer.set("com.example.FooType", "fooOption", "fooVal");
                        });
        MatcherAssert.assertThat(
                e.getMessage(), Matchers.equalTo("Unknown event type \"com.example.FooType\""));
    }

    @Test
    void shouldthrowExceptionForKnownEventWithUnknownOption() throws Exception {
        IEventTypeInfo event = Mockito.mock(IEventTypeInfo.class);
        IEventTypeID typeId = Mockito.mock(IEventTypeID.class);
        Mockito.when(event.getEventTypeID()).thenReturn(typeId);
        Mockito.when(typeId.getFullKey()).thenReturn("com.example.FooType");

        IOptionDescriptor optionDescriptor = Mockito.mock(IOptionDescriptor.class);
        Mockito.when(event.getOptionDescriptors())
                .thenReturn(
                        Collections.singletonMap(
                                "fooOption", EventOptionsCustomizer.capture(optionDescriptor)));

        Mockito.when(service.getAvailableEventTypes())
                .thenReturn(Collections.singleton(EventOptionsCustomizer.capture(event)));

        Exception e =
                Assertions.assertThrows(
                        EventOptionException.class,
                        () -> {
                            customizer.set("com.example.FooType", "barOption", "fooVal");
                        });
        MatcherAssert.assertThat(
                e.getMessage(),
                Matchers.equalTo("Unknown option \"barOption\" for event \"com.example.FooType\""));
    }

    @Test
    void shouldThrowExceptionForInvalidValueToKnownTypeAndOption() throws Exception {
        IEventTypeInfo event = Mockito.mock(IEventTypeInfo.class);
        IEventTypeID typeId = Mockito.mock(IEventTypeID.class);
        Mockito.when(event.getEventTypeID()).thenReturn(typeId);
        Mockito.when(typeId.getFullKey()).thenReturn("com.example.FooType");

        IOptionDescriptor optionDescriptor = Mockito.mock(IOptionDescriptor.class);
        Mockito.when(event.getOptionDescriptors())
                .thenReturn(
                        Collections.singletonMap(
                                "fooOption", EventOptionsCustomizer.capture(optionDescriptor)));

        IConstraint constraint = Mockito.mock(IConstraint.class);
        Mockito.doThrow(IllegalArgumentException.class).when(constraint).validate(Mockito.any());
        Mockito.when(optionDescriptor.getConstraint()).thenReturn(constraint);

        Mockito.when(service.getAvailableEventTypes())
                .thenReturn(Collections.singleton(EventOptionsCustomizer.capture(event)));

        Exception e =
                Assertions.assertThrows(
                        OptionValueException.class,
                        () -> {
                            customizer.set("com.example.FooType", "fooOption", "fooVal");
                        });
        MatcherAssert.assertThat(
                e.getMessage(),
                Matchers.equalTo(
                        "Invalid value \"fooVal\" for event \"com.example.FooType\", option"
                                + " \"fooOption\""));
    }

    @Test
    void shouldAcceptValidEventOptionValues() throws Exception {
        IEventTypeInfo event = Mockito.mock(IEventTypeInfo.class);
        IEventTypeID typeId = Mockito.mock(IEventTypeID.class);
        Mockito.when(event.getEventTypeID()).thenReturn(typeId);
        Mockito.when(typeId.getFullKey()).thenReturn("com.example.FooType");

        IOptionDescriptor optionDescriptor = Mockito.mock(IOptionDescriptor.class);
        Mockito.when(event.getOptionDescriptors())
                .thenReturn(
                        Collections.singletonMap(
                                "fooOption", EventOptionsCustomizer.capture(optionDescriptor)));

        Object parsedValue = new Object();
        IConstraint constraint = Mockito.mock(IConstraint.class);
        Mockito.when(constraint.parseInteractive(Mockito.anyString())).thenReturn(parsedValue);
        Mockito.when(optionDescriptor.getConstraint()).thenReturn(constraint);

        Mockito.when(service.getAvailableEventTypes())
                .thenReturn(Collections.singleton(EventOptionsCustomizer.capture(event)));

        customizer.set("com.example.FooType", "fooOption", "fooVal");

        ArgumentCaptor<EventOptionID> idCaptor = ArgumentCaptor.forClass(EventOptionID.class);
        ArgumentCaptor<Object> valueCaptor = ArgumentCaptor.forClass(Object.class);
        Mockito.verify(emptyMap).put(idCaptor.capture(), valueCaptor.capture());
        Mockito.verify(constraint).validate(parsedValue);

        EventOptionID createdId = idCaptor.getValue();
        MatcherAssert.assertThat(createdId.getEventTypeID(), Matchers.sameInstance(typeId));
        MatcherAssert.assertThat(createdId.getOptionKey(), Matchers.equalTo("fooOption"));
        MatcherAssert.assertThat(valueCaptor.getValue(), Matchers.sameInstance(parsedValue));
    }
}
