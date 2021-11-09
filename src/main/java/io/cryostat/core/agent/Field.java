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
