package com.redhat.rhjmc.containerjfr.core;

import java.util.HashMap;
import java.util.Map;

import org.openjdk.jmc.common.unit.IConstrainedMap;
import org.openjdk.jmc.common.unit.IConstraint;
import org.openjdk.jmc.common.unit.IMutableConstrainedMap;
import org.openjdk.jmc.common.unit.IOptionDescriptor;
import org.openjdk.jmc.common.unit.QuantityConversionException;
import org.openjdk.jmc.flightrecorder.configuration.events.EventOptionID;
import org.openjdk.jmc.flightrecorder.configuration.events.IEventTypeID;
import org.openjdk.jmc.rjmx.services.jfr.IEventTypeInfo;

import com.redhat.rhjmc.containerjfr.core.net.JFRConnection;

public class EventOptionsCustomizer {

    private final JFRConnection connection;
    private final IMutableConstrainedMap<EventOptionID> map;
    private Map<IEventTypeID, Map<String, IOptionDescriptor<?>>> knownTypes;
    private Map<String, IEventTypeID> eventIds;

    public EventOptionsCustomizer(JFRConnection connection) {
        this.connection = connection;
        this.map = connection.getService().getDefaultEventOptions().emptyWithSameConstraints();
    }

    public EventOptionsCustomizer set(String typeId, String option, String value)
            throws FlightRecorderException, EventTypeException, EventOptionException,
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

    public IConstrainedMap<EventOptionID> asMap() {
        return map.mutableCopy();
    }

    private boolean isInitialized() {
        return knownTypes != null;
    }

    private void initialize() throws FlightRecorderException {
        knownTypes = new HashMap<>();
        eventIds = new HashMap<>();
        try {
            for (IEventTypeInfo eventTypeInfo : connection.getService().getAvailableEventTypes()) {
                eventIds.put(
                        eventTypeInfo.getEventTypeID().getFullKey(),
                        eventTypeInfo.getEventTypeID());
                knownTypes.putIfAbsent(
                        eventTypeInfo.getEventTypeID(),
                        new HashMap<>(eventTypeInfo.getOptionDescriptors()));
            }
        } catch (org.openjdk.jmc.rjmx.services.jfr.FlightRecorderException e) {
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
