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

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class Event {

    private static final String DEFAULT_STRING_FIELD = ""; // $NON-NLS-1$
    private static final boolean DEFAULT_BOOLEAN_FIELD = false;
    private static final Object DEFAULT_OBJECT_FIELD = null;
    private static final String DEFAULT_EVENT_ID = "event.id"; // $NON-NLS-1$
    private static final String DEFAULT_EVENT_NAME = "New Custom Event"; // $NON-NLS-1$
    private static final String DEFAULT_EVENT_CLAZZ = "com.company.project.MyClass"; // $NON-NLS-1$
    private static final String DEFAULT_METHOD_NAME = "myMethod"; // $NON-NLS-1$
    private static final String DEFAULT_METHOD_DESCRIPTOR = "()V"; // $NON-NLS-1$
    private static final String CLAZZ_REGEX =
            "([a-zA-Z_$][a-zA-Z0-9_$]*\\.)*([a-zA-Z_$][a-zA-Z0-9_$]*)"; // $NON-NLS-1$
    private static final String PATH_REGEX = "([^/]+/)*([^/]*)"; // $NON-NLS-1$
    private static final String METHOD_NAME_REGEX = "[a-zA-Z_$][a-zA-Z0-9_$]*"; // $NON-NLS-1$
    private static final String METHOD_DESCRIPTOR_REGEX =
            "\\((\\[*([BCDFIJSZ]|L([a-zA-Z_$][a-zA-Z0-9_$]*/)*[a-zA-Z_$][a-zA-Z0-9_$]*;))*\\)(V|\\[*([BCDFIJSZ]|L([a-zA-Z_$][a-zA-Z0-9_$]*/)*[a-zA-Z_$][a-zA-Z0-9_$]*;))"; // $NON-NLS-1$

    private static final Pattern NAME_WITH_COUNT_PATTERN =
            Pattern.compile("^(.*)\\s*\\((\\d+)\\)$"); // $NON-NLS-1$
    private static final Pattern COUNT_SUFFIX_PATTERN =
            Pattern.compile("^\\s*\\((\\d+)\\)$"); // $NON-NLS-1$

    private static final String XML_TAG_EVENT = "event"; // $NON-NLS-1$
    private static final String XML_TAG_NAME = "label"; // $NON-NLS-1$
    private static final String XML_TAG_DESCRIPTION = "description"; // $NON-NLS-1$
    private static final String XML_TAG_CLASS = "class"; // $NON-NLS-1$
    private static final String XML_TAG_PATH = "path"; // $NON-NLS-1$
    private static final String XML_TAG_STACK_TRACE = "stacktrace"; // $NON-NLS-1$
    private static final String XML_TAG_RETHROW = "rethrow"; // $NON-NLS-1$
    private static final String XML_TAG_LOCATION = "location"; // $NON-NLS-1$
    private static final String XML_TAG_METHOD = "method"; // $NON-NLS-1$
    private static final String XML_TAG_DESCRIPTOR = "descriptor"; // $NON-NLS-1$
    private static final String XML_TAG_PARAMETERS = "parameters"; // $NON-NLS-1$
    private static final String XML_TAG_PARAMETER = "parameter"; // $NON-NLS-1$
    private static final String XML_TAG_FIELDS = "fields"; // $NON-NLS-1$
    private static final String XML_TAG_FIELD = "field"; // $NON-NLS-1$
    private static final String XML_TAG_RETURN_VALUE = "returnvalue"; // $NON-NLS-1$
    private static final String XML_ATTRIBUTE_ID = "id"; // $NON-NLS-1$
    private static final String XML_TAG_METHOD_NAME = "name"; // $NON-NLS-1$

    private String id;
    private String name;
    private String clazz;
    private String description;
    private String path;
    private boolean recordStackTrace;
    private boolean useRethrow;
    private String methodName;
    private String methodDescriptor;
    private Location location;
    private MethodReturnValue returnValue;

    private final List<MethodParameter> parameters = new ArrayList<>();
    private final List<Field> fields = new ArrayList<>();

    enum Location {
        ENTRY,
        EXIT,
        WRAP,
    }

    public Event(Element element) {
        id = element.getAttribute(XML_ATTRIBUTE_ID);
        name = getFirstDirectChildElementByTagName(element, XML_TAG_NAME).getTextContent();
        clazz = getFirstDirectChildElementByTagName(element, XML_TAG_CLASS).getTextContent();

        Element descriptionElement =
                getFirstDirectChildElementByTagName(element, XML_TAG_DESCRIPTION);
        if (descriptionElement != null) {
            description = descriptionElement.getTextContent();
        }

        Element pathElement = getFirstDirectChildElementByTagName(element, XML_TAG_PATH);
        if (pathElement != null) {
            path = pathElement.getTextContent();
        }

        Element stackTraceElement =
                getFirstDirectChildElementByTagName(element, XML_TAG_STACK_TRACE);
        if (stackTraceElement != null) {
            recordStackTrace = Boolean.parseBoolean(stackTraceElement.getTextContent());
        }

        Element rethrowElement = getFirstDirectChildElementByTagName(element, XML_TAG_RETHROW);
        if (rethrowElement != null) {
            useRethrow = Boolean.parseBoolean(rethrowElement.getTextContent());
        }

        Element locationElement = getFirstDirectChildElementByTagName(element, XML_TAG_LOCATION);
        if (locationElement != null) {
            location =
                    Location.valueOf(locationElement.getTextContent().toUpperCase(Locale.ENGLISH));
        }

        Element methodElement = getFirstDirectChildElementByTagName(element, XML_TAG_METHOD);
        methodName =
                getFirstDirectChildElementByTagName(methodElement, XML_TAG_METHOD_NAME)
                        .getTextContent();
        methodDescriptor =
                getFirstDirectChildElementByTagName(methodElement, XML_TAG_DESCRIPTOR)
                        .getTextContent();

        Element parametersElement =
                getFirstDirectChildElementByTagName(methodElement, XML_TAG_PARAMETERS);
        if (parametersElement != null) {
            NodeList parameterNodes = parametersElement.getElementsByTagName(XML_TAG_PARAMETER);
            for (int i = 0; i < parameterNodes.getLength(); i++) {
                parameters.add(createMethodParameter((Element) parameterNodes.item(i)));
            }
        }

        Element returnValueElement =
                getFirstDirectChildElementByTagName(methodElement, XML_TAG_RETURN_VALUE);
        if (returnValueElement != null) {
            returnValue = createMethodReturnValue(returnValueElement);
        }

        Element fieldsElement = getFirstDirectChildElementByTagName(element, XML_TAG_FIELDS);
        if (fieldsElement != null) {
            NodeList fieldNodes = fieldsElement.getElementsByTagName(XML_TAG_FIELD);
            for (int i = 0; i < fieldNodes.getLength(); i++) {
                fields.add(createField((Element) fieldNodes.item(i)));
            }
        }
    }

    private static Element getFirstDirectChildElementByTagName(Element parent, String name) {
        NodeList children = parent.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child instanceof Element && child.getNodeName().equals(name)) {
                return (Element) child;
            }
        }

        return null;
    }

    private MethodParameter createMethodParameter(Element element) {
        return new MethodParameter(this, element);
    }

    private Field createField(Element element) {
        return new Field(this, element);
    }

    private MethodReturnValue createMethodReturnValue(Element element) {
        return new MethodReturnValue(this, element);
    }

    public Element buildElement(Document document) {
        Element element = document.createElement(XML_TAG_EVENT);
        element.setAttribute(XML_ATTRIBUTE_ID, id);

        Element nameElement = document.createElement(XML_TAG_NAME);
        nameElement.setTextContent(name);
        element.appendChild(nameElement);

        if (description != null && !description.isEmpty()) {
            Element descriptionElement = document.createElement(XML_TAG_DESCRIPTION);
            descriptionElement.setTextContent(description);
            element.appendChild(descriptionElement);
        }

        Element classElement = document.createElement(XML_TAG_CLASS);
        classElement.setTextContent(clazz);
        element.appendChild(classElement);

        if (path != null && !path.isEmpty()) {
            Element pathElement = document.createElement(XML_TAG_PATH);
            pathElement.setTextContent(path);
            element.appendChild(pathElement);
        }

        Element stackTraceElement = document.createElement(XML_TAG_STACK_TRACE);
        stackTraceElement.setTextContent(String.valueOf(recordStackTrace));
        element.appendChild(stackTraceElement);

        Element rethrowElement = document.createElement(XML_TAG_RETHROW);
        rethrowElement.setTextContent(String.valueOf(useRethrow));
        element.appendChild(rethrowElement);

        if (location != null) {
            Element locationElement = document.createElement(XML_TAG_LOCATION);
            locationElement.setTextContent(location.toString());
            element.appendChild(locationElement);
        }

        element.appendChild(buildMethodElement(document));

        if (!fields.isEmpty()) {
            Element fieldsElement = document.createElement(XML_TAG_FIELDS);
            for (Field field : fields) {
                fieldsElement.appendChild(field.buildElement(document));
            }
            element.appendChild(fieldsElement);
        }

        return element;
    }

    private Element buildMethodElement(Document document) {
        Element methodElement = document.createElement(XML_TAG_METHOD);
        {
            Element methodNameElement = document.createElement(XML_TAG_METHOD_NAME);
            methodNameElement.setTextContent(methodName);
            methodElement.appendChild(methodNameElement);

            Element methodDescriptorElement = document.createElement(XML_TAG_DESCRIPTOR);
            methodDescriptorElement.setTextContent(methodDescriptor);
            methodElement.appendChild(methodDescriptorElement);

            if (!parameters.isEmpty()) {
                Element methodParametersElement = document.createElement(XML_TAG_PARAMETERS);
                for (MethodParameter methodParameter : parameters) {
                    methodParametersElement.appendChild(methodParameter.buildElement(document));
                }
                methodElement.appendChild(methodParametersElement);
            }

            if (returnValue != null) {
                methodElement.appendChild(returnValue.buildElement(document));
            }
        }

        return methodElement;
    }
}
