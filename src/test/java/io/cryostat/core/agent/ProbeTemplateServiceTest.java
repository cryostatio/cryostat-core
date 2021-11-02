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
    String xmlText;

    @BeforeEach
    void setup() throws IOException {
        xmlText = IOUtils.toString(this.getClass().getResourceAsStream("template.xml"));
        this.service = new LocalProbeTemplateService(fs, env);
    }

    @Test
    void addTemplateShouldWriteTemplate() throws Exception {
        Mockito.when(env.hasEnv(TEMPLATE_PATH)).thenReturn(true);
        Mockito.when(env.getEnv(TEMPLATE_PATH)).thenReturn("/templates");

        Path path = Mockito.mock(Path.class);
        Mockito.when(fs.pathOf("/templates")).thenReturn(path);

        Path templatePath = Mockito.mock(Path.class);
        Mockito.when(fs.pathOf("/templates", "template.xml")).thenReturn(templatePath);

        InputStream stream = IOUtils.toInputStream(xmlText);

        Mockito.when(fs.exists(path)).thenReturn(true);
        Mockito.when(fs.isDirectory(path)).thenReturn(true);
        Mockito.when(fs.isReadable(path)).thenReturn(true);
        Mockito.when(fs.isWritable(path)).thenReturn(true);

        service.addTemplate(stream, "template.xml");

        Mockito.verify(fs).writeString(fs.pathOf(env.getEnv(TEMPLATE_PATH), "template.xml"), "");
    }

    @Test
    void deleteTemplateShouldDeleteFile() throws Exception {
        Mockito.when(env.hasEnv(LocalProbeTemplateService.TEMPLATE_PATH)).thenReturn(true);
        Mockito.when(env.getEnv(LocalProbeTemplateService.TEMPLATE_PATH)).thenReturn("/templates");

        Path path = Mockito.mock(Path.class);
        Path templatePath = Mockito.mock(Path.class);
        Mockito.when(fs.pathOf("/templates")).thenReturn(path);
        Mockito.when(fs.exists(path)).thenReturn(true);
        Mockito.when(fs.isDirectory(path)).thenReturn(true);
        Mockito.when(fs.isReadable(path)).thenReturn(true);
        Mockito.when(fs.isWritable(path)).thenReturn(true);
        Mockito.when(fs.pathOf("/templates", "template.xml")).thenReturn(templatePath);
        Mockito.when(fs.deleteIfExists(templatePath)).thenReturn(true);

        service.deleteTemplate("template.xml");

        Mockito.verify(fs).deleteIfExists(templatePath);
    }
}
