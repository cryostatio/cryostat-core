/*
 * Copyright The Cryostat Authors
 *
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
        NONE,
        BYTES,
        TIMESTAMP,
        MILLIS,
        NANOS,
        TICKS,
        ADDRESS,
        OS_THREAD,
        JAVA_THREAD,
        STACK_TRACE,
        CLASS,
        PERCENTAGE
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

    protected void copyContentToWorkingCopy(CapturedValue copy) {
        copy.name = name;
        copy.description = description;
        copy.contentType = contentType;
        copy.relationKey = relationKey;
        copy.converter = converter;
    }
}
