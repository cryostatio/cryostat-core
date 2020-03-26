package com.redhat.rhjmc.containerjfr.core.templates;

import java.util.List;

import org.openjdk.jmc.common.unit.IConstrainedMap;
import org.openjdk.jmc.flightrecorder.configuration.events.EventOptionID;

import com.redhat.rhjmc.containerjfr.core.FlightRecorderException;

public interface TemplateService {

    List<Template> getTemplates() throws FlightRecorderException;

    IConstrainedMap<EventOptionID> getEventsByTemplateName(String templateName)
            throws FlightRecorderException;

    @SuppressWarnings("serial")
    public static class UnknownEventTemplateException extends Exception {
        public UnknownEventTemplateException(String templateName) {
            super(String.format("Unknown event template \"%s\"", templateName));
        }
    }
}
