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
import java.text.ParseException;

import org.openjdk.jmc.common.unit.IConstrainedMap;
import org.openjdk.jmc.common.unit.QuantityConversionException;
import org.openjdk.jmc.flightrecorder.configuration.FlightRecorderException;
import org.openjdk.jmc.flightrecorder.configuration.IFlightRecorderService;
import org.openjdk.jmc.flightrecorder.configuration.IRecordingDescriptor;
import org.openjdk.jmc.flightrecorder.configuration.events.EventConfiguration;
import org.openjdk.jmc.flightrecorder.configuration.events.EventOptionID;
import org.openjdk.jmc.flightrecorder.configuration.model.xml.XMLModel;
import org.openjdk.jmc.rjmx.common.ConnectionException;
import org.openjdk.jmc.rjmx.common.ServiceNotAvailableException;

import io.cryostat.core.EventOptionsBuilder.EventOptionException;
import io.cryostat.core.EventOptionsBuilder.EventTypeException;
import io.cryostat.libcryostat.templates.Template;

public interface CryostatFlightRecorderService extends IFlightRecorderService {

    IRecordingDescriptor start(IConstrainedMap<String> recordingOptions, Template eventTemplate)
            throws io.cryostat.core.FlightRecorderException, FlightRecorderException,
                    ConnectionException, ParseException, IOException, FlightRecorderException,
                    ServiceNotAvailableException, QuantityConversionException, EventOptionException,
                    EventTypeException;

    default IRecordingDescriptor start(IConstrainedMap<String> recordingOptions, String template)
            throws io.cryostat.core.FlightRecorderException, FlightRecorderException,
                    ConnectionException, ParseException, IOException, ServiceNotAvailableException,
                    QuantityConversionException, EventOptionException, EventTypeException {
        XMLModel model = EventConfiguration.createModel(template);
        IConstrainedMap<EventOptionID> eventOptions =
                new EventConfiguration(model)
                        .getEventOptions(getDefaultEventOptions().emptyWithSameConstraints());
        return start(recordingOptions, eventOptions);
    }
}
