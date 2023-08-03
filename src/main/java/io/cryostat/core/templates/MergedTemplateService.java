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

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.openjdk.jmc.common.unit.IConstrainedMap;
import org.openjdk.jmc.flightrecorder.configuration.events.EventOptionID;

import io.cryostat.core.FlightRecorderException;
import io.cryostat.core.net.JFRConnection;
import io.cryostat.core.sys.Environment;
import io.cryostat.core.sys.FileSystem;

import org.jsoup.nodes.Document;

public class MergedTemplateService implements MutableTemplateService {

    protected final RemoteTemplateService remote;
    protected final LocalStorageTemplateService local;

    public MergedTemplateService(JFRConnection conn, FileSystem fs, Environment env) {
        this.remote = new RemoteTemplateService(conn);
        this.local = new LocalStorageTemplateService(fs, env);
    }

    @Override
    public List<Template> getTemplates() throws FlightRecorderException {
        List<Template> templates = new ArrayList<>();
        templates.addAll(remote.getTemplates());
        templates.addAll(local.getTemplates());
        return templates;
    }

    @Override
    public Optional<Document> getXml(String templateName, TemplateType type)
            throws FlightRecorderException {
        switch (type) {
            case CUSTOM:
                return local.getXml(templateName, type);
            case TARGET:
                return remote.getXml(templateName, type);
            default:
                return Optional.empty();
        }
    }

    @Override
    public Optional<IConstrainedMap<EventOptionID>> getEvents(
            String templateName, TemplateType type) throws FlightRecorderException {
        switch (type) {
            case CUSTOM:
                return local.getEvents(templateName, type);
            case TARGET:
                return remote.getEvents(templateName, type);
            default:
                return Optional.empty();
        }
    }

    @Override
    public Template addTemplate(InputStream templateStream)
            throws InvalidXmlException, InvalidEventTemplateException, IOException {
        return local.addTemplate(templateStream);
    }

    @Override
    public void deleteTemplate(String templateName)
            throws IOException, InvalidEventTemplateException {
        local.deleteTemplate(templateName);
    }
}
