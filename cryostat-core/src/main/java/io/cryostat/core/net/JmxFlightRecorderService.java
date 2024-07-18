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
package io.cryostat.core.net;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.openjdk.jmc.common.unit.IConstrainedMap;
import org.openjdk.jmc.common.unit.IDescribedMap;
import org.openjdk.jmc.common.unit.IOptionDescriptor;
import org.openjdk.jmc.common.unit.IQuantity;
import org.openjdk.jmc.common.unit.QuantityConversionException;
import org.openjdk.jmc.flightrecorder.configuration.FlightRecorderException;
import org.openjdk.jmc.flightrecorder.configuration.IFlightRecorderService;
import org.openjdk.jmc.flightrecorder.configuration.IRecordingDescriptor;
import org.openjdk.jmc.flightrecorder.configuration.events.EventOptionID;
import org.openjdk.jmc.flightrecorder.configuration.events.IEventTypeID;
import org.openjdk.jmc.flightrecorder.configuration.events.IEventTypeInfo;
import org.openjdk.jmc.rjmx.common.ConnectionException;
import org.openjdk.jmc.rjmx.common.ServiceNotAvailableException;
import org.openjdk.jmc.rjmx.common.services.jfr.internal.FlightRecorderServiceFactory;

import io.cryostat.core.EventOptionsBuilder;
import io.cryostat.core.EventOptionsBuilder.EventOptionException;
import io.cryostat.core.EventOptionsBuilder.EventTypeException;
import io.cryostat.libcryostat.templates.Template;
import io.cryostat.libcryostat.templates.TemplateType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JmxFlightRecorderService implements CryostatFlightRecorderService {

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final JFRJMXConnection conn;

    JmxFlightRecorderService(JFRJMXConnection conn) {
        this.conn = conn;
    }

    protected IFlightRecorderService tryConnect() throws FlightRecorderException {
        try {
            if (!conn.isConnected()) {
                conn.connect();
            }
            IFlightRecorderService service =
                    new FlightRecorderServiceFactory().getServiceInstance(conn.getHandle());
            if (service == null || !conn.isConnected()) {
                throw new ConnectionException(
                        String.format(
                                "Could not connect to remote target %s",
                                conn.connectionDescriptor.createJMXServiceURL().toString()));
            }
            return service;
        } catch (IOException | ServiceNotAvailableException e) {
            throw new FlightRecorderException("Connection failed", e);
        }
    }

    @Override
    public List<IRecordingDescriptor> getAvailableRecordings() throws FlightRecorderException {
        return tryConnect().getAvailableRecordings();
    }

    @Override
    public IRecordingDescriptor getSnapshotRecording() throws FlightRecorderException {
        return tryConnect().getSnapshotRecording();
    }

    @Override
    public IRecordingDescriptor getUpdatedRecordingDescription(IRecordingDescriptor descriptor)
            throws FlightRecorderException {
        return tryConnect().getUpdatedRecordingDescription(descriptor);
    }

    @Override
    public IRecordingDescriptor start(
            IConstrainedMap<String> recordingOptions, IConstrainedMap<EventOptionID> eventOptions)
            throws FlightRecorderException {
        return tryConnect().start(recordingOptions, eventOptions);
    }

    @Override
    public void stop(IRecordingDescriptor descriptor) throws FlightRecorderException {
        tryConnect().stop(descriptor);
    }

    @Override
    public void close(IRecordingDescriptor descriptor) throws FlightRecorderException {
        tryConnect().close(descriptor);
    }

    @Override
    public Map<String, IOptionDescriptor<?>> getAvailableRecordingOptions()
            throws FlightRecorderException {
        return tryConnect().getAvailableRecordingOptions();
    }

    @Override
    public IConstrainedMap<String> getRecordingOptions(IRecordingDescriptor recording)
            throws FlightRecorderException {
        return tryConnect().getRecordingOptions(recording);
    }

    @Override
    public Collection<? extends IEventTypeInfo> getAvailableEventTypes()
            throws FlightRecorderException {
        return tryConnect().getAvailableEventTypes();
    }

    @Override
    public Map<? extends IEventTypeID, ? extends IEventTypeInfo> getEventTypeInfoMapByID()
            throws FlightRecorderException {
        return tryConnect().getEventTypeInfoMapByID();
    }

    @Override
    public IConstrainedMap<EventOptionID> getCurrentEventTypeSettings()
            throws FlightRecorderException {
        return tryConnect().getCurrentEventTypeSettings();
    }

    @Override
    public IConstrainedMap<EventOptionID> getEventSettings(IRecordingDescriptor recording)
            throws FlightRecorderException {
        return tryConnect().getEventSettings(recording);
    }

    @Override
    public InputStream openStream(IRecordingDescriptor descriptor, boolean removeOnClose)
            throws FlightRecorderException {
        return tryConnect().openStream(descriptor, removeOnClose);
    }

    @Override
    public InputStream openStream(
            IRecordingDescriptor descriptor,
            IQuantity startTime,
            IQuantity endTime,
            boolean removeOnClose)
            throws FlightRecorderException {
        return tryConnect().openStream(descriptor, startTime, endTime, removeOnClose);
    }

    @Override
    public InputStream openStream(
            IRecordingDescriptor descriptor, IQuantity lastPartDuration, boolean removeOnClose)
            throws FlightRecorderException {
        return tryConnect().openStream(descriptor, lastPartDuration, removeOnClose);
    }

    @Override
    public List<String> getServerTemplates() throws FlightRecorderException {
        return tryConnect().getServerTemplates();
    }

    @Override
    public void updateEventOptions(
            IRecordingDescriptor descriptor, IConstrainedMap<EventOptionID> options)
            throws FlightRecorderException {
        tryConnect().updateEventOptions(descriptor, options);
    }

    @Override
    public void updateRecordingOptions(
            IRecordingDescriptor descriptor, IConstrainedMap<String> options)
            throws FlightRecorderException {
        tryConnect().updateRecordingOptions(descriptor, options);
    }

    @Override
    public boolean isEnabled() {
        try {
            return tryConnect().isEnabled();
        } catch (FlightRecorderException e) {
            logger.error("Connection failed", e);
            return false;
        }
    }

    @Override
    public void enable() throws FlightRecorderException {
        tryConnect().enable();
    }

    @Override
    public String getVersion() {
        try {
            return tryConnect().getVersion();
        } catch (FlightRecorderException e) {
            logger.error("Connection failed", e);
            return "unknown";
        }
    }

    @Override
    public IDescribedMap<String> getDefaultRecordingOptions() {
        try {
            return tryConnect().getDefaultRecordingOptions();
        } catch (FlightRecorderException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public IDescribedMap<EventOptionID> getDefaultEventOptions() {
        try {
            return tryConnect().getDefaultEventOptions();
        } catch (FlightRecorderException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public IRecordingDescriptor start(IConstrainedMap<String> recordingOptions, Template template)
            throws io.cryostat.core.FlightRecorderException, FlightRecorderException,
                    ConnectionException, IOException, FlightRecorderException,
                    ServiceNotAvailableException, QuantityConversionException, EventOptionException,
                    EventTypeException {
        return tryConnect()
                .start(recordingOptions, enableEvents(template.getName(), template.getType()));
    }

    private IConstrainedMap<EventOptionID> enableEvents(
            String templateName, TemplateType templateType)
            throws ConnectionException, IOException, io.cryostat.core.FlightRecorderException,
                    FlightRecorderException, ServiceNotAvailableException,
                    QuantityConversionException, EventOptionException, EventTypeException {
        if (templateName.equals("ALL")) {
            return enableAllEvents();
        }
        // if template type not specified, try to find a Custom template by that name. If none,
        // fall back on finding a Target built-in template by the name. If not, throw an
        // exception and bail out.
        TemplateType type = getPreferredTemplateType(conn, templateName, templateType);
        return conn.getTemplateService().getEvents(templateName, type).get();
    }

    private IConstrainedMap<EventOptionID> enableAllEvents()
            throws ConnectionException, IOException, FlightRecorderException,
                    ServiceNotAvailableException, QuantityConversionException, EventOptionException,
                    EventTypeException {
        EventOptionsBuilder builder = new EventOptionsBuilder.Factory().create(conn);

        for (IEventTypeInfo eventTypeInfo : conn.getService().getAvailableEventTypes()) {
            builder.addEvent(eventTypeInfo.getEventTypeID().getFullKey(), "enabled", "true");
        }

        return builder.build();
    }

    private TemplateType getPreferredTemplateType(
            JFRConnection connection, String templateName, TemplateType templateType)
            throws io.cryostat.core.FlightRecorderException {
        if (templateType != null) {
            return templateType;
        }
        List<Template> matchingNameTemplates =
                connection.getTemplateService().getTemplates().stream()
                        .filter(t -> t.getName().equals(templateName))
                        .collect(Collectors.toList());
        boolean custom =
                matchingNameTemplates.stream()
                        .anyMatch(t -> t.getType().equals(TemplateType.CUSTOM));
        if (custom) {
            return TemplateType.CUSTOM;
        }
        boolean target =
                matchingNameTemplates.stream()
                        .anyMatch(t -> t.getType().equals(TemplateType.TARGET));
        if (target) {
            return TemplateType.TARGET;
        }
        throw new IllegalArgumentException(
                String.format("Invalid/unknown event template %s", templateName));
    }
}
