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

import static io.cryostat.core.agent.LocalProbeTemplateService.TEMPLATE_PATH;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;

import io.cryostat.core.sys.Environment;
import io.cryostat.core.sys.FileSystem;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProbeTemplateServiceTest {

    LocalProbeTemplateService service;
    @Mock FileSystem fs;
    @Mock Environment env;
    @Mock Path probeTemplatePath;
    String xmlText;

    @BeforeEach
    void setup() throws IOException {
        xmlText = IOUtils.toString(this.getClass().getResourceAsStream("template.xml"));
        Mockito.when(env.hasEnv(LocalProbeTemplateService.TEMPLATE_PATH)).thenReturn(true);
        Mockito.when(env.getEnv(LocalProbeTemplateService.TEMPLATE_PATH)).thenReturn("/templates");
        Mockito.when(fs.pathOf("/templates")).thenReturn(probeTemplatePath);
        Mockito.when(fs.exists(probeTemplatePath)).thenReturn(true);
        Mockito.when(fs.isDirectory(probeTemplatePath)).thenReturn(true);
        Mockito.when(fs.isReadable(probeTemplatePath)).thenReturn(true);
        Mockito.when(fs.isWritable(probeTemplatePath)).thenReturn(true);
        this.service = new LocalProbeTemplateService(fs, env);
    }

    @Test
    void addTemplateShouldWriteTemplate() throws Exception {

        Path templatePath = Mockito.mock(Path.class);
        Mockito.when(fs.pathOf("/templates", "template.xml")).thenReturn(templatePath);

        InputStream stream = IOUtils.toInputStream(xmlText);
        service.addTemplate(stream, "template.xml");

        Mockito.verify(fs).writeString(fs.pathOf(env.getEnv(TEMPLATE_PATH), "template.xml"), "");
    }

    @Test
    void deleteTemplateShouldDeleteFile() throws Exception {

        Path path = Mockito.mock(Path.class);
        Path templatePath = Mockito.mock(Path.class);
        Mockito.when(fs.pathOf("/templates", "template.xml")).thenReturn(templatePath);
        Mockito.when(fs.deleteIfExists(templatePath)).thenReturn(true);

        service.deleteTemplate("template.xml");

        Mockito.verify(fs).deleteIfExists(templatePath);
    }
}
