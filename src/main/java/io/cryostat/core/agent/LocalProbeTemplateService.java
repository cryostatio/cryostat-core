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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import io.cryostat.core.FlightRecorderException;
import io.cryostat.core.sys.Environment;
import io.cryostat.core.sys.FileSystem;

import org.xml.sax.SAXException;

public class LocalProbeTemplateService extends AbstractProbeTemplateService {

    public static final String TEMPLATE_PATH = "CRYOSTAT_PROBE_TEMPLATE_PATH";

    private final FileSystem fs;
    private final Environment env;

    public LocalProbeTemplateService(FileSystem fs, Environment env) {
        this.fs = fs;
        this.env = env;
    }

    public void addTemplate(InputStream inputStream, String filename) throws IOException {
        if (!env.hasEnv(TEMPLATE_PATH)) {
            throw new IOException(
                    String.format(
                            "Probe template directory does not exist, must be set using environment variable %s",
                            TEMPLATE_PATH));
        }
        Path probeTemplateDirectory = fs.pathOf(env.getEnv(TEMPLATE_PATH));
        // Sanity check the directory is set up correctly
        if (!fs.exists(probeTemplateDirectory)
                || !fs.isDirectory(probeTemplateDirectory)
                || !fs.isReadable(probeTemplateDirectory)
                || !fs.isWritable(probeTemplateDirectory)) {
            throw new IOException(
                    String.format(
                            "Probe template directory %s does not exist, is not a directory, or has incorrect permissions.",
                            probeTemplateDirectory.toString()));
        }
        try (inputStream) {
            ProbeTemplate template = new ProbeTemplate();
            // If validation fails this will throw a ProbeValidationException with details
            template.deserialize(inputStream);
            Path path = fs.pathOf(env.getEnv(TEMPLATE_PATH), filename);
            if (fs.exists(path)) {
                throw new Exception(
                        String.format(
                                "Event template \"%s\" already exists", template.getFileName()));
            }
            fs.writeString(
                    path,
                    new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))
                            .lines()
                            .collect(Collectors.joining("\n")));
        } catch (ProbeValidationException pve) {
            // rethrow for http handler
            throw pve;
        } catch (Exception e) {
            // ignore
        }
    }

    public void deleteTemplate(String templateName) throws Exception {
        // Sanity Check
        if (!env.hasEnv(TEMPLATE_PATH)) {
            throw new IOException(
                    String.format(
                            "Probe template directory does not exist, must be set using environment variable %s",
                            TEMPLATE_PATH));
        }
        Path probeTemplateDirectory = fs.pathOf(env.getEnv(TEMPLATE_PATH));
        if (!fs.exists(probeTemplateDirectory)
                || !fs.isDirectory(probeTemplateDirectory)
                || !fs.isReadable(probeTemplateDirectory)
                || !fs.isWritable(probeTemplateDirectory)) {
            throw new IOException(
                    String.format(
                            "Probe template directory %s does not exist, is not a directory, or has incorrect permissions.",
                            probeTemplateDirectory.toString()));
        }
        if (!fs.deleteIfExists(fs.pathOf(env.getEnv(TEMPLATE_PATH), templateName))) {
            throw new IOException(
                    String.format("Probe template \"%s\" does not exist", templateName));
        }
    }

    public String getTemplate(String templateName) throws Exception {
        if (!env.hasEnv(TEMPLATE_PATH)) {
            throw new IOException(
                    String.format(
                            "Probe template directory does not exist, must be set using environment variable %s",
                            TEMPLATE_PATH));
        }
        Path probeTemplateDirectory = fs.pathOf(env.getEnv(TEMPLATE_PATH));
        if (!fs.exists(probeTemplateDirectory)
                || !fs.isDirectory(probeTemplateDirectory)
                || !fs.isReadable(probeTemplateDirectory)
                || !fs.isWritable(probeTemplateDirectory)) {
            throw new IOException(
                    String.format(
                            "Probe template directory %s does not exist, is not a directory, or has incorrect permissions.",
                            probeTemplateDirectory.toString()));
        }
        Path probeTemplatePath = fs.pathOf(env.getEnv(TEMPLATE_PATH), templateName);
        ProbeTemplate template = new ProbeTemplate();
        template.deserialize(fs.newInputStream(probeTemplatePath));
        return template.serialize();
    }

    protected List<Path> getLocalTemplates() throws IOException {
        if (!env.hasEnv(TEMPLATE_PATH)) {
            return Collections.emptyList();
        }
        String dirName = env.getEnv(TEMPLATE_PATH);
        Path dir = fs.pathOf(dirName);
        if (!fs.isDirectory(dir) || !fs.isReadable(dir)) {
            throw new IOException(String.format("%s is not a readable directory", dirName));
        }
        try {
            return fs.listDirectoryChildren(dir).stream()
                    .map(name -> fs.pathOf(dirName, name))
                    .collect(Collectors.toList());
        } catch (IOException e) {
            throw e;
        }
    }

    @Override
    public List<ProbeTemplate> getTemplates() throws FlightRecorderException {
        try {
            List<ProbeTemplate> templates = new ArrayList<>();
            for (Path path : getLocalTemplates()) {
                try (InputStream stream = fs.newInputStream(path)) {
                    ProbeTemplate template = new ProbeTemplate();
                    template.deserialize(stream);
                    templates.add(template);
                }
            }
            return templates;
        } catch (IOException | SAXException e) {
            throw new FlightRecorderException(e);
        }
    }
}
