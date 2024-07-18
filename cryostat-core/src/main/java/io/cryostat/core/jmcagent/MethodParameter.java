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

public class MethodParameter extends CapturedValue {
    private static final String DEFAULT_PARAMETER_NAME = "New Parameter";
    private static final int DEFAULT_INDEX = 0;

    private static final String XML_TAG_PARAMETER = "parameter";
    private static final String XML_ATTRIBUTE_INDEX = "index";

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
