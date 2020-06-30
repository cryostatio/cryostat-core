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
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;
import org.jsoup.select.Elements;

import org.openjdk.jmc.common.unit.IConstrainedMap;
import org.openjdk.jmc.common.unit.SimpleConstrainedMap;
import org.openjdk.jmc.common.unit.UnitLookup;
import org.openjdk.jmc.flightrecorder.configuration.events.EventOptionID;
import org.openjdk.jmc.flightrecorder.controlpanel.ui.configuration.model.xml.XMLModel;
import org.openjdk.jmc.flightrecorder.controlpanel.ui.model.EventConfiguration;

import com.redhat.rhjmc.containerjfr.core.FlightRecorderException;
import com.redhat.rhjmc.containerjfr.core.sys.Environment;
import com.redhat.rhjmc.containerjfr.core.sys.FileSystem;

public class LocalStorageTemplateService extends AbstractTemplateService
        implements MutableTemplateService {

    public static final String TEMPLATE_PATH = "CONTAINER_JFR_TEMPLATE_PATH";

    private final FileSystem fs;
    private final Environment env;

    public LocalStorageTemplateService(FileSystem fs, Environment env) {
        this.fs = fs;
        this.env = env;
    }

    @Override
    public void addTemplate(InputStream templateStream)
            throws InvalidXmlException, InvalidEventTemplateException, IOException {
        if (!env.hasEnv(TEMPLATE_PATH)) {
            throw new IOException(
                    String.format(
                            "Template directory does not exist, must be set using environment variable %s",
                            TEMPLATE_PATH));
        }
        Path dir = fs.pathOf(env.getEnv(TEMPLATE_PATH));
        if (!fs.exists(dir) || !fs.isDirectory(dir) || !fs.isReadable(dir) || !fs.isWritable(dir)) {
            throw new IOException(
                    String.format(
                            "Template directory %s does not exist, is not a directory, or does not have appropriate permissions",
                            dir.toString()));
        }
        try (templateStream) {
            Document doc =
                    Jsoup.parse(
                            templateStream, StandardCharsets.UTF_8.name(), "", Parser.xmlParser());
            Elements els = doc.getElementsByTag("configuration");
            if (els.isEmpty()) {
                throw new InvalidXmlException("Document did not contain \"configuration\" element");
            }
            if (els.size() > 1) {
                throw new InvalidXmlException(
                        "Document contains multiple \"configuration\" elements");
            }
            Element configuration = els.first();
            if (!configuration.hasAttr("label")) {
                throw new InvalidXmlException(
                        "Configuration element did not have \"label\" attribute");
            }
            // side effect of validation, throws ParseException or IllegalArgumentException if
            // invalid
            EventConfiguration.createModel(doc.toString());

            String templateName = configuration.attr("label");
            fs.writeString(fs.pathOf(env.getEnv(TEMPLATE_PATH), templateName), doc.toString());
        } catch (IOException ioe) {
            throw new InvalidXmlException("Unable to parse XML stream", ioe);
        } catch (ParseException | IllegalArgumentException e) {
            throw new InvalidEventTemplateException("Invalid XML", e);
        }
    }

    @Override
    public void deleteTemplate(String templateName) throws IOException {
        if (!env.hasEnv(TEMPLATE_PATH)) {
            throw new IOException(
                    String.format(
                            "Template directory does not exist, must be set using environment variable %s",
                            TEMPLATE_PATH));
        }
        Path dir = fs.pathOf(env.getEnv(TEMPLATE_PATH));
        if (!fs.exists(dir) || !fs.isDirectory(dir) || !fs.isReadable(dir) || !fs.isWritable(dir)) {
            throw new IOException(
                    String.format(
                            "Template directory %s does not exist, is not a directory, or does not have appropriate permissions",
                            dir.toString()));
        }
        fs.deleteIfExists(fs.pathOf(env.getEnv(TEMPLATE_PATH), templateName));
    }

    @Override
    public Optional<Document> getXml(String templateName) throws FlightRecorderException {
        for (Path path : getLocalTemplates()) {
            try (InputStream stream = fs.newInputStream(path)) {
                Document doc =
                        Jsoup.parse(stream, StandardCharsets.UTF_8.name(), "", Parser.xmlParser());
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
                if (configuration.attr("label").equals(templateName)) {
                    return Optional.of(doc);
                }
            } catch (IOException e) {
                throw new FlightRecorderException(e);
            }
        }
        return Optional.empty();
    }

    @Override
    public Optional<IConstrainedMap<EventOptionID>> getEventsByTemplateName(String templateName)
            throws FlightRecorderException {
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
                                                new SimpleConstrainedMap<>(
                                                        UnitLookup.PLAIN_TEXT.getPersister())));
    }

    protected List<Path> getLocalTemplates() throws FlightRecorderException {
        if (!env.hasEnv(TEMPLATE_PATH)) {
            return Collections.emptyList();
        }
        String dirName = env.getEnv(TEMPLATE_PATH);
        Path dir = fs.pathOf(dirName);
        if (!fs.isDirectory(dir) || !fs.isReadable(dir)) {
            throw new FlightRecorderException(
                    new IOException(String.format("%s is not a readable directory", dirName)));
        }
        try {
            return fs.listDirectoryChildren(dir).stream()
                    .map(name -> fs.pathOf(dirName, name))
                    .collect(Collectors.toList());
        } catch (IOException e) {
            throw new FlightRecorderException(e);
        }
    }

    @Override
    protected List<XMLModel> getTemplateModels() throws FlightRecorderException {
        try {
            List<XMLModel> models = new ArrayList<>();
            for (Path path : getLocalTemplates()) {
                try (InputStream stream = fs.newInputStream(path)) {
                    models.add(EventConfiguration.createModel(stream));
                }
            }
            return models;
        } catch (IOException | ParseException e) {
            throw new FlightRecorderException(e);
        }
    }
}
