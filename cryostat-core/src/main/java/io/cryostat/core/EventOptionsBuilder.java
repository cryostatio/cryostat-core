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

import java.io.IOException;
import java.util.Collection;
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
import org.openjdk.jmc.rjmx.common.ServiceNotAvailableException;
import org.openjdk.jmc.rjmx.common.services.jfr.internal.FlightRecorderServiceV2;

import io.cryostat.core.net.JFRConnection;

public class EventOptionsBuilder {

    private final IMutableConstrainedMap<EventOptionID> map;
    private final Map<IEventTypeID, Map<String, IOptionDescriptor<?>>> knownTypes;
    private final Map<String, IEventTypeID> eventIds;

    public EventOptionsBuilder(
            IMutableConstrainedMap<EventOptionID> empty,
            Collection<? extends IEventTypeInfo> eventTypes) {
        this.map = empty.emptyWithSameConstraints();
        this.knownTypes = new HashMap<>();
        this.eventIds = new HashMap<>();

        for (IEventTypeInfo eventTypeInfo : eventTypes) {
            eventIds.put(
                    eventTypeInfo.getEventTypeID().getFullKey(), eventTypeInfo.getEventTypeID());
            knownTypes.putIfAbsent(
                    eventTypeInfo.getEventTypeID(),
                    new HashMap<>(eventTypeInfo.getOptionDescriptors()));
        }
    }

    public EventOptionsBuilder addEvent(String typeId, String option, String value)
            throws EventTypeException, EventOptionException, QuantityConversionException {
        if (!eventIds.containsKey(typeId)) {
            throw new EventTypeException(typeId);
        }
        Map<String, IOptionDescriptor<?>> optionDescriptors = knownTypes.get(eventIds.get(typeId));
        if (!optionDescriptors.containsKey(option)) {
            throw new EventOptionException(typeId, option);
        }
        IConstraint<?> constraint = optionDescriptors.get(option).getConstraint();
        Object parsedValue = constraint.parseInteractive(value);
        constraint.validate(capture(parsedValue));
        this.map.put(new EventOptionID(eventIds.get(typeId), option), parsedValue);

        return this;
    }

    static <T, V> V capture(T t) {
        // TODO clean up this generics hack
        return (V) t;
    }

    public IConstrainedMap<EventOptionID> build() {
        return map.mutableCopy();
    }

    public static class EventTypeException extends Exception {
        EventTypeException(String eventType) {
            super(String.format("Unknown event type \"%s\"", eventType));
        }
    }

    public static class EventOptionException extends Exception {
        EventOptionException(String eventType, String option) {
            super(String.format("Unknown option \"%s\" for event \"%s\"", option, eventType));
        }
    }

    public static class Factory {
        public EventOptionsBuilder create(JFRConnection connection)
                throws IOException,
                        ServiceNotAvailableException,
                        org.openjdk.jmc.flightrecorder.configuration.FlightRecorderException {
            if (!FlightRecorderServiceV2.isAvailable(connection.getHandle())) {
                throw new UnsupportedOperationException("Only FlightRecorder V2 is supported");
            }
            IMutableConstrainedMap<EventOptionID> empty =
                    connection.getService().getDefaultEventOptions().emptyWithSameConstraints();
            Collection<? extends IEventTypeInfo> eventTypes =
                    connection.getService().getAvailableEventTypes();
            return new EventOptionsBuilder(empty, eventTypes);
        }
    }
}
