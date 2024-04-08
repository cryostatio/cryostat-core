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
package io.cryostat.core.agent;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * Abstraction class for parsing and handling captured values (method parameters, fields,
 * converters)
 */
public class CapturedValue {

    private static final String DEFAULT_STRING_FIELD = "";
    private static final Object DEFAULT_OBJECT_TYPE = null;
    private static final String CONVERTER_REGEX =
            "([a-zA-Z_$][a-zA-Z0-9_$]*\\.)*([a-zA-Z_$][a-zA-Z0-9_$]*)";

    private static final String XML_TAG_CAPTURED_VALUE = "capturedvalue";
    private static final String XML_TAG_NAME = "name";
    private static final String XML_TAG_DESCRIPTION = "description";
    private static final String XML_TAG_CONTENT_TYPE = "contenttype";
    private static final String XML_TAG_RELATION_KEY = "relationkey";
    private static final String XML_TAG_CONVERTER = "converter";

    private String name;
    private String description;
    private ContentType contentType;
    private String relationKey;
    private String converter;

    enum ContentType {
        NONE("None"),
        BYTES("Bytes"),
        TIMESTAMP("Timestamp"),
        MILLIS("Millis"),
        NANOS("Nanos"),
        TICKS("Ticks"),
        ADDRESS("Address"),
        OS_THREAD("OSThread"),
        JAVA_THREAD("JavaThread"),
        STACK_TRACE("StackTrace"),
        CLASS("Class"),
        PERCENTAGE("Percentage");

        private final String contentType;

        ContentType(String contentType) {
            this.contentType = contentType;
        }

        public String toString() {
            return contentType;
        }
    }

    CapturedValue() {
        name = DEFAULT_STRING_FIELD;
        description = DEFAULT_STRING_FIELD;
        contentType = (ContentType) DEFAULT_OBJECT_TYPE;
        relationKey = DEFAULT_STRING_FIELD;
        converter = DEFAULT_STRING_FIELD;
    }

    CapturedValue(Element element) {
        this();

        NodeList elements;
        elements = element.getElementsByTagName(XML_TAG_NAME);
        if (elements.getLength() != 0) {
            name = elements.item(0).getTextContent();
        }

        elements = element.getElementsByTagName(XML_TAG_DESCRIPTION);
        if (elements.getLength() != 0) {
            description = elements.item(0).getTextContent();
        }

        elements = element.getElementsByTagName(XML_TAG_CONTENT_TYPE);
        if (elements.getLength() != 0) {
            contentType =
                    ContentType.valueOf(
                            elements.item(0).getTextContent().toUpperCase(Locale.ENGLISH));
        }

        elements = element.getElementsByTagName(XML_TAG_RELATION_KEY);
        if (elements.getLength() != 0) {
            relationKey = elements.item(0).getTextContent();
        }

        elements = element.getElementsByTagName(XML_TAG_CONVERTER);
        if (elements.getLength() != 0) {
            converter = elements.item(0).getTextContent();
        }
    }

    public Element buildElement(Document document) {
        Element element = document.createElement(XML_TAG_CAPTURED_VALUE);

        if (name != null && !name.isEmpty()) {
            Element nameElement = document.createElement(XML_TAG_NAME);
            nameElement.setTextContent(name);
            element.appendChild(nameElement);
        }

        if (description != null && !description.isEmpty()) {
            Element descriptionElement = document.createElement(XML_TAG_DESCRIPTION);
            descriptionElement.setTextContent(description);
            element.appendChild(descriptionElement);
        }

        if (contentType != null) {
            Element contentTypeElement = document.createElement(XML_TAG_CONTENT_TYPE);
            contentTypeElement.setTextContent(contentType.toString());
            element.appendChild(contentTypeElement);
        }

        if (relationKey != null && !relationKey.isEmpty()) {
            Element relationKeyElement = document.createElement(XML_TAG_RELATION_KEY);
            relationKeyElement.setTextContent(relationKey);
            element.appendChild(relationKeyElement);
        }

        if (converter != null && !converter.isEmpty()) {
            Element converterElement = document.createElement(XML_TAG_CONVERTER);
            converterElement.setTextContent(converter);
            element.appendChild(converterElement);
        }

        return element;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public ContentType getContentType() {
        return contentType;
    }

    public void setContentType(ContentType contentType) {
        this.contentType = contentType;
    }

    public String getRelationKey() {
        return relationKey;
    }

    public void setRelationKey(String relationKey) {
        if (relationKey != null && !relationKey.isEmpty()) {
            relationKey = relationKey.trim();
            try {
                new URI(relationKey);
            } catch (URISyntaxException e) {
                throw new IllegalArgumentException(
                        "Relational Key has incorrect syntax: " + relationKey);
            }
        }

        this.relationKey = relationKey;
    }

    public String getConverter() {
        return converter;
    }

    public void setConverter(String converter) {
        if (converter != null && !converter.isEmpty()) {
            converter = converter.trim();
            if (!converter.matches(CONVERTER_REGEX)) {
                throw new IllegalArgumentException("Converter has incorrect syntax: " + converter);
            }
        }

        this.converter = converter;
    }

    @Override
    protected final void finalize() {}

    protected void copyContentToWorkingCopy(CapturedValue copy) {
        copy.name = name;
        copy.description = description;
        copy.contentType = contentType;
        copy.relationKey = relationKey;
        copy.converter = converter;
    }
}
