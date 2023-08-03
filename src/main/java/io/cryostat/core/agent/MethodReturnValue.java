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

import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class MethodReturnValue extends CapturedValue {

    private static final String XML_TAG_RETURN_VALUE = "returnvalue";

    private final Event event;

    MethodReturnValue(Event event) {
        super();

        this.event = event;
    }

    MethodReturnValue(Event event, Element element) {
        super(element);

        this.event = event;
    }

    @Override
    public Element buildElement(Document document) {
        Element element = super.buildElement(document);
        return (Element) document.renameNode(element, null, XML_TAG_RETURN_VALUE);
    }

    public MethodReturnValue createWorkingCopy() {
        MethodReturnValue copy = new MethodReturnValue(event);
        return copy;
    }
}
