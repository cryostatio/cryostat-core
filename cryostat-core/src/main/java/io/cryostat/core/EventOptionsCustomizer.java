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

import org.openjdk.jmc.common.unit.IConstrainedMap;
import org.openjdk.jmc.common.unit.IConstraint;
import org.openjdk.jmc.common.unit.IMutableConstrainedMap;
import org.openjdk.jmc.common.unit.IOptionDescriptor;
import org.openjdk.jmc.common.unit.QuantityConversionException;
import org.openjdk.jmc.flightrecorder.configuration.events.EventOptionID;
import org.openjdk.jmc.flightrecorder.configuration.events.IEventTypeID;
import org.openjdk.jmc.flightrecorder.configuration.events.IEventTypeInfo;

import io.cryostat.core.net.JFRConnection;

public class EventOptionsCustomizer {

    private final JFRConnection connection;
    private IMutableConstrainedMap<EventOptionID> map;
    private Map<IEventTypeID, Map<String, IOptionDescriptor<?>>> knownTypes;
    private Map<String, IEventTypeID> eventIds;

    public EventOptionsCustomizer(JFRConnection connection) {
        this.connection = connection;
    }

    public EventOptionsCustomizer set(String typeId, String option, String value)
            throws FlightRecorderException,
                    EventTypeException,
                    EventOptionException,
                    OptionValueException {
        if (!isInitialized()) {
            initialize();
        }
        if (!eventIds.containsKey(typeId)) {
            throw new EventTypeException(typeId);
        }
        Map<String, IOptionDescriptor<?>> optionDescriptors = knownTypes.get(eventIds.get(typeId));
        if (!optionDescriptors.containsKey(option)) {
            throw new EventOptionException(typeId, option);
        }
        IConstraint<?> constraint = optionDescriptors.get(option).getConstraint();
        try {
            Object parsedValue = constraint.parseInteractive(value);
            constraint.validate(capture(parsedValue));
            this.map.put(new EventOptionID(eventIds.get(typeId), option), parsedValue);
        } catch (IllegalArgumentException | QuantityConversionException e) {
            throw new OptionValueException(typeId, option, value, e);
        } catch (Exception e) {
            throw new FlightRecorderException(e);
        }
        return this;
    }

    public IConstrainedMap<EventOptionID> asMap() throws FlightRecorderException {
        if (!isInitialized()) {
            initialize();
        }
        return map.mutableCopy();
    }

    private boolean isInitialized() {
        return knownTypes != null;
    }

    private void initialize() throws FlightRecorderException {
        try {
            this.map =
                    this.connection
                            .getService()
                            .getDefaultEventOptions()
                            .emptyWithSameConstraints();
            this.knownTypes = new HashMap<>();
            this.eventIds = new HashMap<>();
            for (IEventTypeInfo eventTypeInfo : connection.getService().getAvailableEventTypes()) {
                eventIds.put(
                        eventTypeInfo.getEventTypeID().getFullKey(),
                        eventTypeInfo.getEventTypeID());
                knownTypes.putIfAbsent(
                        eventTypeInfo.getEventTypeID(),
                        new HashMap<>(eventTypeInfo.getOptionDescriptors()));
            }
        } catch (Exception e) {
            throw new FlightRecorderException(e);
        }
    }

    static <T, V> V capture(T t) {
        // TODO clean up this generics hack
        return (V) t;
    }

    @SuppressWarnings("serial")
    public static class EventTypeException extends Exception {
        EventTypeException(String eventType) {
            super(String.format("Unknown event type \"%s\"", eventType));
        }
    }

    @SuppressWarnings("serial")
    public static class EventOptionException extends Exception {
        EventOptionException(String eventType, String option) {
            super(String.format("Unknown option \"%s\" for event \"%s\"", option, eventType));
        }
    }

    @SuppressWarnings("serial")
    public static class OptionValueException extends Exception {
        OptionValueException(String eventType, String option, String value, Throwable cause) {
            super(
                    String.format(
                            "Invalid value \"%s\" for event \"%s\", option \"%s\"",
                            value, eventType, option),
                    cause);
        }
    }
}
