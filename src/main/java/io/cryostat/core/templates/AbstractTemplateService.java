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
package io.cryostat.core.templates;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.openjdk.jmc.flightrecorder.controlpanel.ui.configuration.model.xml.XMLModel;
import org.openjdk.jmc.flightrecorder.controlpanel.ui.configuration.model.xml.XMLTagInstance;

import io.cryostat.core.FlightRecorderException;

abstract class AbstractTemplateService implements TemplateService {

    @Override
    public List<Template> getTemplates() throws FlightRecorderException {
        return getTemplateModels().stream()
                .map(xml -> xml.getRoot())
                .map(
                        root ->
                                new Template(
                                        getAttributeValue(root, "label"),
                                        getAttributeValue(root, "description"),
                                        getAttributeValue(root, "provider"),
                                        providedTemplateType()))
                .collect(Collectors.toList());
    }

    protected abstract TemplateType providedTemplateType();

    protected abstract List<XMLModel> getTemplateModels() throws FlightRecorderException;

    protected String getAttributeValue(XMLTagInstance node, String valueKey) {
        return node.getAttributeInstances().stream()
                .filter(i -> Objects.equals(valueKey, i.getAttribute().getName()))
                .map(i -> i.getValue())
                .findFirst()
                .get();
    }
}
