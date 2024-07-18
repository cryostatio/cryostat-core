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

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class Field extends CapturedValue {

    private static final String DEFAULT_FIELD_NAME = "New Field";
    private static final String DEFAULT_FIELD_EXPRESSION = "myField";
    private static final String EXPRESSION_REGEX =
            "([a-zA-Z_$][a-zA-Z0-9_$]*\\.)*([a-zA-Z_$][a-zA-Z0-9_$]*)(\\.[a-zA-Z_$][a-zA-Z_$]*)*";

    private static final String XML_TAG_FIELD = "field";
    private static final String XML_TAG_EXPRESSION = "expression";

    private final Event event;

    private String expression;

    Field(Event event) {
        super();
        this.event = event;

        expression = DEFAULT_FIELD_EXPRESSION;
        setName(DEFAULT_FIELD_NAME);
    }

    Field(Event event, Element element) {
        super(element);
        this.event = event;

        NodeList elements = element.getElementsByTagName(XML_TAG_EXPRESSION);
        if (elements.getLength() != 0) {
            expression = elements.item(0).getTextContent();
        }
    }

    @Override
    public Element buildElement(Document document) {
        Element element = super.buildElement(document);
        element = (Element) document.renameNode(element, null, XML_TAG_FIELD);

        Element expressionElement = document.createElement(XML_TAG_EXPRESSION);
        expressionElement.setTextContent(expression);
        element.appendChild(expressionElement);

        return element;
    }

    @Override
    public void setName(String name) {
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("name cannot be empty or null");
        }

        super.setName(name);
    }

    public String getExpression() {
        return expression;
    }

    public void setExpression(String expression) {
        if (expression == null || expression.isEmpty()) {
            throw new IllegalArgumentException("expression cannot be empty or null");
        }

        expression = expression.trim();
        if (!expression.matches(EXPRESSION_REGEX)) {
            throw new IllegalArgumentException("expression has incorrect syntax");
        }

        this.expression = expression;
    }

    public Field createWorkingCopy() {
        Field copy = new Field(event);

        copyContentToWorkingCopy(copy);
        copy.expression = expression;

        return copy;
    }
}
