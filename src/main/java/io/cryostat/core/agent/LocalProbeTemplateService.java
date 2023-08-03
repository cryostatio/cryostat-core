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

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import io.cryostat.core.FlightRecorderException;
import io.cryostat.core.sys.Environment;
import io.cryostat.core.sys.FileSystem;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.xml.sax.SAXException;

public class LocalProbeTemplateService implements ProbeTemplateService {

    public static final String TEMPLATE_PATH = "CRYOSTAT_PROBE_TEMPLATE_PATH";

    private final FileSystem fs;
    private final Environment env;

    @SuppressFBWarnings(
            value = "EI_EXPOSE_REP2",
            justification = "fields are not exposed since there are no getters")
    public LocalProbeTemplateService(FileSystem fs, Environment env) throws IOException {
        this.fs = fs;
        this.env = env;
        // Sanity check
        if (!env.hasEnv(TEMPLATE_PATH)) {
            throw new IOException(
                    String.format(
                            "Probe template directory does not exist, must be set using environment"
                                    + " variable %s",
                            TEMPLATE_PATH));
        }
        // Sanity check the directory is set up correctly
        Path probeTemplateDirectory = fs.pathOf(env.getEnv(TEMPLATE_PATH));
        if (!fs.exists(probeTemplateDirectory)
                || !fs.isDirectory(probeTemplateDirectory)
                || !fs.isReadable(probeTemplateDirectory)
                || !fs.isWritable(probeTemplateDirectory)) {
            throw new IOException(
                    String.format(
                            "Probe template directory %s does not exist, is not a directory, or has"
                                    + " incorrect permissions.",
                            probeTemplateDirectory.toString()));
        }
    }

    public ProbeTemplate addTemplate(InputStream inputStream, String filename)
            throws FileAlreadyExistsException, IOException, SAXException {
        Path path = fs.pathOf(env.getEnv(TEMPLATE_PATH), filename);
        if (fs.exists(path)) {
            throw new FileAlreadyExistsException(
                    String.format("Probe template \"%s\" already exists.", filename));
        }
        try (inputStream) {
            ProbeTemplate template = new ProbeTemplate();
            template.setFileName(filename);
            // If validation fails this will throw a ProbeValidationException with details
            template.deserialize(inputStream);
            fs.writeString(path, template.serialize());
            return template;
        }
    }

    public void deleteTemplate(String templateName) throws IOException {
        if (!fs.deleteIfExists(fs.pathOf(env.getEnv(TEMPLATE_PATH), templateName))) {
            throw new IOException(
                    String.format("Probe template \"%s\" does not exist", templateName));
        }
    }

    public String getTemplateContent(String templateName) throws IOException, SAXException {
        Path probeTemplatePath = fs.pathOf(env.getEnv(TEMPLATE_PATH), templateName);
        ProbeTemplate template = new ProbeTemplate();
        template.setFileName(templateName);
        template.deserialize(fs.newInputStream(probeTemplatePath));
        return template.serialize();
    }

    protected List<Path> getLocalTemplates() throws IOException {
        String dirName = env.getEnv(TEMPLATE_PATH);
        Path dir = fs.pathOf(dirName);
        if (!fs.isDirectory(dir) || !fs.isReadable(dir)) {
            throw new IOException(String.format("%s is not a readable directory", dirName));
        }
        return fs.listDirectoryChildren(dir).stream()
                .map(dir::resolve)
                .collect(Collectors.toList());
    }

    @Override
    public List<ProbeTemplate> getTemplates() throws FlightRecorderException {
        try {
            List<ProbeTemplate> templates = new ArrayList<>();
            for (Path path : getLocalTemplates()) {
                if (path != null) {
                    try (InputStream stream = fs.newInputStream(path)) {
                        Path fileName = path.getFileName();
                        if (fileName != null) {
                            ProbeTemplate template = new ProbeTemplate();
                            template.setFileName(fileName.toString());
                            template.deserialize(stream);
                            templates.add(template);
                        }
                    }
                } else {
                    throw new IOException("getLocalTemplates returned null path");
                }
            }
            return templates;
        } catch (IOException | SAXException e) {
            throw new FlightRecorderException("Could not get templates", e);
        }
    }
}
