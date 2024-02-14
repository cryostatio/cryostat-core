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

import java.io.IOException;
import java.text.ParseException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.openjdk.jmc.common.unit.IConstrainedMap;
import org.openjdk.jmc.flightrecorder.configuration.events.EventOptionID;
import org.openjdk.jmc.flightrecorder.controlpanel.ui.configuration.model.xml.XMLModel;
import org.openjdk.jmc.flightrecorder.controlpanel.ui.model.EventConfiguration;
import org.openjdk.jmc.rjmx.ServiceNotAvailableException;
import org.openjdk.jmc.rjmx.services.jfr.IFlightRecorderService;

import io.cryostat.core.FlightRecorderException;
import io.cryostat.core.net.JFRConnection;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RemoteTemplateService extends AbstractTemplateService {

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final JFRConnection conn;

    public RemoteTemplateService(JFRConnection conn) {
        this.conn = conn;
    }

    @Override
    protected TemplateType providedTemplateType() {
        return TemplateType.TARGET;
    }

    @Override
    public Optional<Document> getXml(String templateName, TemplateType type)
            throws FlightRecorderException {
        if (!providedTemplateType().equals(type)) {
            return Optional.empty();
        }
        try {
            return conn.getService().getServerTemplates().stream()
                    .map(xmlText -> Jsoup.parse(xmlText, "", Parser.xmlParser()))
                    .filter(
                            doc -> {
                                Elements els = doc.getElementsByTag("configuration");
                                if (els.isEmpty()) {
                                    throw new MalformedXMLException(
                                            "Document did not contain \"configuration\" element");
                                }
                                if (els.size() > 1) {
                                    throw new MalformedXMLException(
                                            "Document contains multiple \"configuration\""
                                                    + " elements");
                                }
                                Element configuration = els.first();
                                if (!configuration.hasAttr("label")) {
                                    throw new MalformedXMLException(
                                            "Configuration element did not have \"label\""
                                                    + " attribute");
                                }
                                return configuration.attr("label").equals(templateName);
                            })
                    .findFirst();
        } catch (org.openjdk.jmc.rjmx.services.jfr.FlightRecorderException
                | IOException
                | ServiceNotAvailableException e) {
            throw new FlightRecorderException("Could not get XML", e);
        }
    }

    @Override
    public Optional<IConstrainedMap<EventOptionID>> getEvents(
            String templateName, TemplateType type) throws FlightRecorderException {
        if (!providedTemplateType().equals(type)) {
            return Optional.empty();
        }
        try {
            IFlightRecorderService service = conn.getService();
            return getTemplateModels().stream()
                    .filter(
                            m ->
                                    m.getRoot().getAttributeInstances().stream()
                                            .anyMatch(
                                                    attr ->
                                                            attr.getAttribute()
                                                                            .getName()
                                                                            .equals("label")
                                                                    && attr.getValue()
                                                                            .equals(templateName)))
                    .findFirst()
                    .map(
                            model ->
                                    new EventConfiguration(model)
                                            .getEventOptions(
                                                    service.getDefaultEventOptions()
                                                            .emptyWithSameConstraints()));
        } catch (IOException | ServiceNotAvailableException e) {
            throw new FlightRecorderException("Could not get events", e);
        }
    }

    @Override
    protected List<XMLModel> getTemplateModels() throws FlightRecorderException {
        try {
            return conn.getService().getServerTemplates().stream()
                    .map(
                            xmlText -> {
                                try {
                                    return EventConfiguration.createModel(xmlText);
                                } catch (ParseException | IOException e) {
                                    logger.warn("Exception thrown", e);
                                    return null;
                                }
                            })
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
        } catch (org.openjdk.jmc.rjmx.services.jfr.FlightRecorderException
                | IOException
                | ServiceNotAvailableException e) {
            throw new FlightRecorderException("Could not get template models", e);
        }
    }
}
