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
package io.cryostat.core.templates;

import java.util.List;
import java.util.Optional;

import org.openjdk.jmc.common.unit.IConstrainedMap;
import org.openjdk.jmc.flightrecorder.configuration.events.EventOptionID;

import io.cryostat.core.FlightRecorderException;

import org.jsoup.nodes.Document;

public interface TemplateService {

    List<Template> getTemplates() throws FlightRecorderException;

    default Optional<Document> getXml(Template template) throws FlightRecorderException {
        return getXml(template.getName(), template.getType());
    }

    Optional<Document> getXml(String templateName, TemplateType type)
            throws FlightRecorderException;

    default Optional<IConstrainedMap<EventOptionID>> getEvents(Template template)
            throws FlightRecorderException {
        return getEvents(template.getName(), template.getType());
    }

    Optional<IConstrainedMap<EventOptionID>> getEvents(String templateName, TemplateType type)
            throws FlightRecorderException;
}
