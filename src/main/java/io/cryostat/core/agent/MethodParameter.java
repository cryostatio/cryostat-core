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

public class MethodParameter extends CapturedValue {
    private static final String DEFAULT_PARAMETER_NAME = "New Parameter"; // $NON-NLS-1$
    private static final int DEFAULT_INDEX = 0;

    private static final String XML_TAG_PARAMETER = "parameter"; // $NON-NLS-1$
    private static final String XML_ATTRIBUTE_INDEX = "index"; // $NON-NLS-1$

    private final Event event;

    private int index;

    MethodParameter(Event event) {
        super();
        this.event = event;

        index = DEFAULT_INDEX;
        setName(DEFAULT_PARAMETER_NAME);
    }

    MethodParameter(Event event, Element element) {
        super(element);
        this.event = event;

        index = Integer.parseInt(element.getAttribute(XML_ATTRIBUTE_INDEX));
    }

    @Override
    public Element buildElement(Document document) {
        Element element = super.buildElement(document);
        element = (Element) document.renameNode(element, null, XML_TAG_PARAMETER);
        element.setAttribute(XML_ATTRIBUTE_INDEX, String.valueOf(index));
        return element;
    }

    @Override
    public void setName(String name) {
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("Name cannot be empty or null.");
        }

        super.setName(name);
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        if (index < 0) {
            throw new IllegalArgumentException("Index cannot be negative");
        }

        this.index = index;
    }

    public MethodParameter createWorkingCopy() {
        MethodParameter parameter = new MethodParameter(event);

        copyContentToWorkingCopy(parameter);
        parameter.index = index;

        return parameter;
    }
}
