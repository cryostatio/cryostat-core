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
package io.cryostat.core.jmcagent;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class ProbeTemplate {

    private static final String DEFAULT_FILE_NAME = "new_file.xml";
    private static final String DEFAULT_CLASS_PREFIX = "__JFREvent";

    private static final String XML_TAG_JFR_AGENT = "jfragent";
    private static final String XML_TAG_CONFIG = "config";
    private static final String XML_TAG_CLASS_PREFIX = "classprefix";
    private static final String XML_TAG_ALLOW_TO_STRING = "allowtostring";
    private static final String XML_TAG_ALLOW_CONVERTER = "allowconverter";
    private static final String XML_TAG_EVENTS = "events";
    private static final String XML_TAG_EVENT = "event";

    private String fileName;
    private String classPrefix;
    private boolean allowToString;
    private boolean allowConverter;

    private List<Event> events;

    public ProbeTemplate(
            String fileName, String classPrefix, boolean allowToString, boolean allowConverter) {
        this.fileName = fileName;
        this.classPrefix = classPrefix;
        this.allowToString = allowToString;
        this.allowConverter = allowConverter;
        this.events = new ArrayList<>();
    }

    public ProbeTemplate() {
        this.fileName = DEFAULT_FILE_NAME;
        this.classPrefix = DEFAULT_CLASS_PREFIX;
        this.allowConverter = false;
        this.allowToString = false;
        this.events = new ArrayList<>();
    }

    public void deserialize(InputStream xmlStream) throws IOException, SAXException {
        JMCAgentXMLStream stream = new JMCAgentXMLStream(xmlStream);
        stream.mark(1); // arbitrary readLimit > 0
        ProbeValidator validator = new ProbeValidator();
        validator.validate(new StreamSource(stream));
        stream.reset();
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder;

        try {
            builder = factory.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            throw new IllegalStateException(e);
        }

        Document document = builder.parse(stream);
        stream.trulyClose();
        NodeList elements;

        // parse global configurations
        // Note: we don't worry about hierarchy here and directly get nodes by tag name, since the
        // validation already
        // guaranteed a correct structure and tag names are unique.
        elements = document.getElementsByTagName(XML_TAG_CONFIG);
        if (elements.getLength() != 0) {
            Element configElement = (Element) elements.item(0);

            elements = configElement.getElementsByTagName(XML_TAG_CLASS_PREFIX);
            if (elements.getLength() != 0) {
                classPrefix = elements.item(0).getTextContent();
            }

            elements = configElement.getElementsByTagName(XML_TAG_ALLOW_TO_STRING);
            if (elements.getLength() != 0) {
                allowToString = Boolean.parseBoolean(elements.item(0).getTextContent());
            }

            elements = configElement.getElementsByTagName(XML_TAG_ALLOW_CONVERTER);
            if (elements.getLength() != 0) {
                allowConverter = Boolean.parseBoolean(elements.item(0).getTextContent());
            }
        }

        elements = document.getElementsByTagName(XML_TAG_EVENTS);
        if (elements.getLength() != 0) {
            Element eventsElement = (Element) elements.item(0);
            elements = eventsElement.getElementsByTagName(XML_TAG_EVENT);
            for (int i = 0; i < elements.getLength(); i++) {
                events.add(createEvent((Element) elements.item(i)));
            }
        }
    }

    public String serialize() {
        Document document = buildDocument();

        TransformerFactory tf = TransformerFactory.newInstance();
        Transformer transformer;
        try {
            transformer = tf.newTransformer();
        } catch (TransformerConfigurationException e) {
            // This should not happen anyway
            throw new IllegalStateException(e);
        }
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        StringWriter writer = new StringWriter(2000);
        try {
            transformer.transform(new DOMSource(document), new StreamResult(writer));
        } catch (TransformerException e) {
            // This should not happen anyway
            throw new IllegalStateException(e);
        }

        return writer.getBuffer().toString();
    }

    public Document buildDocument() {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder;
        try {
            builder = factory.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            // This should not happen anyway
            throw new IllegalStateException(e);
        }

        Document document = builder.newDocument();

        Element jfrAgentElement = document.createElement(XML_TAG_JFR_AGENT);
        document.appendChild(jfrAgentElement);

        jfrAgentElement.appendChild(buildConfigElement(document));

        Element eventsElement = document.createElement(XML_TAG_EVENTS);
        for (Event event : events) {
            eventsElement.appendChild(event.buildElement(document));
        }
        jfrAgentElement.appendChild(eventsElement);

        return document;
    }

    private Element buildConfigElement(Document document) {
        Element element = document.createElement(XML_TAG_CONFIG);

        Element classPrefixElement = document.createElement(XML_TAG_CLASS_PREFIX);
        classPrefixElement.setTextContent(classPrefix != null ? classPrefix : "");
        element.appendChild(classPrefixElement);

        Element allowToStringElement = document.createElement(XML_TAG_ALLOW_TO_STRING);
        allowToStringElement.setTextContent(String.valueOf(allowToString));
        element.appendChild(allowToStringElement);

        Element allowConverterElement = document.createElement(XML_TAG_ALLOW_CONVERTER);
        allowConverterElement.setTextContent(String.valueOf(allowConverter));
        element.appendChild(allowConverterElement);

        return element;
    }

    private Event createEvent(Element element) {
        return new Event(element);
    }

    public String getClassPrefix() {
        return classPrefix;
    }

    public void setAllowToString(boolean allowed) {
        allowToString = allowed;
    }

    public boolean getAllowToString() {
        return allowToString;
    }

    public void setAllowConverter(boolean allowed) {
        allowConverter = allowed;
    }

    public boolean getAllowConverter() {
        return allowConverter;
    }

    public Event[] getEvents() {
        return events.toArray(new Event[0]);
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String name) {
        this.fileName = name;
    }
}
