/*-
 * #%L
 * Container JFR Core
 * %%
 * Copyright (C) 2020 Red Hat, Inc.
 * %%
 * The Universal Permissive License (UPL), Version 1.0
 *
 * Subject to the condition set forth below, permission is hereby granted to any
 * person obtaining a copy of this software, associated documentation and/or data
 * (collectively the "Software"), free of charge and under any and all copyright
 * rights in the Software, and any and all patent rights owned or freely
 * licensable by each licensor hereunder covering either (i) the unmodified
 * Software as contributed to or provided by such licensor, or (ii) the Larger
 * Works (as defined below), to deal in both
 *
 * (a) the Software, and
 * (b) any piece of software and/or hardware listed in the lrgrwrks.txt file if
 * one is included with the Software (each a "Larger Work" to which the Software
 * is contributed by such licensors),
 *
 * without restriction, including without limitation the rights to copy, create
 * derivative works of, display, perform, and distribute the Software and make,
 * use, sell, offer for sale, import, export, have made, and have sold the
 * Software and the Larger Work(s), and to sublicense the foregoing rights on
 * either these or other terms.
 *
 * This license is subject to the following condition:
 * The above copyright notice and either this complete permission notice or at
 * a minimum a reference to the UPL must be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 * #L%
 */
package com.redhat.rhjmc.containerjfr.core.templates;

import java.io.IOException;
import java.text.ParseException;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;
import org.jsoup.select.Elements;

import org.openjdk.jmc.common.unit.IConstrainedMap;
import org.openjdk.jmc.flightrecorder.configuration.events.EventOptionID;
import org.openjdk.jmc.flightrecorder.controlpanel.ui.configuration.model.xml.XMLModel;
import org.openjdk.jmc.flightrecorder.controlpanel.ui.configuration.model.xml.XMLTagInstance;
import org.openjdk.jmc.flightrecorder.controlpanel.ui.model.EventConfiguration;

import com.redhat.rhjmc.containerjfr.core.FlightRecorderException;
import com.redhat.rhjmc.containerjfr.core.log.Logger;
import com.redhat.rhjmc.containerjfr.core.net.JFRConnection;

public class RemoteTemplateService implements TemplateService {

    private final JFRConnection conn;

    public RemoteTemplateService(JFRConnection conn) {
        this.conn = conn;
    }

    @Override
    public List<Template> getTemplates() throws FlightRecorderException {
        try {
            return getRemoteTemplateModels().stream()
                    .map(xml -> xml.getRoot())
                    .map(
                            root ->
                                    new Template(
                                            getAttributeValue(root, "label"),
                                            getAttributeValue(root, "description"),
                                            getAttributeValue(root, "provider")))
                    .collect(Collectors.toList());
        } catch (Exception e) {
            throw new FlightRecorderException(e);
        }
    }

    @Override
    public Document getXml(String templateName) throws FlightRecorderException {
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
                                            "Document contains multiple \"configuration\" elements");
                                }
                                Element configuration = els.first();
                                if (!configuration.hasAttr("label")) {
                                    throw new MalformedXMLException(
                                            "Configuration element did not have \"label\" attribute");
                                }
                                return configuration.attr("label").equals(templateName);
                            })
                    .findFirst()
                    .orElseThrow(() -> new UnknownEventTemplateException(templateName));
        } catch (Exception e) {
            throw new FlightRecorderException(e);
        }
    }

    @Override
    public IConstrainedMap<EventOptionID> getEventsByTemplateName(String templateName)
            throws FlightRecorderException {
        try {
            XMLModel model =
                    getRemoteTemplateModels().stream()
                            .filter(
                                    m ->
                                            m.getRoot().getAttributeInstances().stream()
                                                    .anyMatch(
                                                            attr ->
                                                                    attr.getAttribute()
                                                                                    .getName()
                                                                                    .equals("label")
                                                                            && attr.getValue()
                                                                                    .equals(
                                                                                            templateName)))
                            .findFirst()
                            .orElseThrow(() -> new UnknownEventTemplateException(templateName));

            return new EventConfiguration(model)
                    .getEventOptions(
                            conn.getService().getDefaultEventOptions().emptyWithSameConstraints());
        } catch (Exception e) {
            throw new FlightRecorderException(e);
        }
    }

    private List<XMLModel> getRemoteTemplateModels() throws FlightRecorderException {
        try {
            return conn.getService().getServerTemplates().stream()
                    .map(
                            xmlText -> {
                                try {
                                    return EventConfiguration.createModel(xmlText);
                                } catch (ParseException | IOException e) {
                                    Logger.INSTANCE.warn(e);
                                    return null;
                                }
                            })
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            throw new FlightRecorderException(e);
        }
    }

    private String getAttributeValue(XMLTagInstance node, String valueKey) {
        return node.getAttributeInstances().stream()
                .filter(i -> Objects.equals(valueKey, i.getAttribute().getName()))
                .map(i -> i.getValue())
                .findFirst()
                .get();
    }
}
