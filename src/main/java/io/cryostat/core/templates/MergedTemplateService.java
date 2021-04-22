/*
 * Copyright the Cryostat Authors
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
package io.cryostat.core.templates;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.openjdk.jmc.common.unit.IConstrainedMap;
import org.openjdk.jmc.flightrecorder.configuration.events.EventOptionID;

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
    public List<Template> getTemplates() throws Exception {
        List<Template> templates = new ArrayList<>();
        templates.addAll(remote.getTemplates());
        templates.addAll(local.getTemplates());
        return templates;
    }

    @Override
    public Optional<Document> getXml(String templateName, TemplateType type) throws Exception {
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
            String templateName, TemplateType type) throws Exception {
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
    public void addTemplate(InputStream templateStream)
            throws InvalidXmlException, InvalidEventTemplateException, IOException {
        local.addTemplate(templateStream);
    }

    @Override
    public void deleteTemplate(String templateName)
            throws IOException, InvalidEventTemplateException {
        local.deleteTemplate(templateName);
    }
}
