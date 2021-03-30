/*-
 * #%L
 * Cryostat Core
 * %%
 * Copyright (C) 2020 - 2021 Cryostat
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
import org.openjdk.jmc.rjmx.services.jfr.IFlightRecorderService;

import io.cryostat.core.log.Logger;
import io.cryostat.core.net.JFRConnection;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;
import org.jsoup.select.Elements;

public class RemoteTemplateService extends AbstractTemplateService implements TemplateService {

    private final JFRConnection conn;

    public RemoteTemplateService(JFRConnection conn) {
        this.conn = conn;
    }

    @Override
    protected TemplateType providedTemplateType() {
        return TemplateType.TARGET;
    }

    @Override
    public Optional<Document> getXml(String templateName, TemplateType type) throws Exception {
        if (!providedTemplateType().equals(type)) {
            return Optional.empty();
        }
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
                .findFirst();
    }

    @Override
    public Optional<IConstrainedMap<EventOptionID>> getEvents(
            String templateName, TemplateType type) throws Exception {
        if (!providedTemplateType().equals(type)) {
            return Optional.empty();
        }
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
    }

    @Override
    protected List<XMLModel> getTemplateModels() throws Exception {
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
    }
}
