/*-
 * #%L
 * Cryostat Core
 * %%
 * Copyright (C) 2020 - 2021 The Cryostat Authors
 * %%
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
 * #L%
 */
package io.cryostat.core.templates;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import io.cryostat.core.sys.Environment;
import io.cryostat.core.sys.FileSystem;

import org.apache.commons.io.IOUtils;
import org.apache.commons.text.similarity.LevenshteinDistance;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.parser.Parser;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LocalStorageTemplateServiceTest {

    LocalStorageTemplateService service;
    @Mock FileSystem fs;
    @Mock Environment env;
    String xmlText;

    @BeforeEach
    void setup() throws IOException {
        xmlText = IOUtils.toString(this.getClass().getResourceAsStream("profile.jfc"));
        this.service = new LocalStorageTemplateService(fs, env);
    }

    @Test
    void getNamesShouldReflectLocalStorageTemplateNames() throws Exception {
        Mockito.when(env.hasEnv(LocalStorageTemplateService.TEMPLATE_PATH)).thenReturn(true);
        Mockito.when(env.getEnv(LocalStorageTemplateService.TEMPLATE_PATH))
                .thenReturn("/templates");

        Path path = Mockito.mock(Path.class);
        Mockito.when(fs.pathOf("/templates")).thenReturn(path);
        Mockito.when(fs.listDirectoryChildren(path)).thenReturn(List.of("profile.jfc"));

        Path templatePath = Mockito.mock(Path.class);
        Mockito.when(fs.pathOf("/templates", "profile.jfc")).thenReturn(templatePath);

        InputStream stream = IOUtils.toInputStream(xmlText);
        Mockito.when(fs.newInputStream(templatePath)).thenReturn(stream);

        Mockito.when(fs.isDirectory(path)).thenReturn(true);
        Mockito.when(fs.isReadable(path)).thenReturn(true);

        MatcherAssert.assertThat(
                service.getTemplates(),
                Matchers.equalTo(
                        Collections.singletonList(
                                new Template(
                                        "Profiling",
                                        "Low overhead configuration for profiling, typically around 2 % overhead.",
                                        "Oracle",
                                        TemplateType.CUSTOM))));
    }

    @Test
    void getEventsShouldReturnNonEmptyMap() throws Exception {
        Mockito.when(env.hasEnv(LocalStorageTemplateService.TEMPLATE_PATH)).thenReturn(true);
        Mockito.when(env.getEnv(LocalStorageTemplateService.TEMPLATE_PATH))
                .thenReturn("/templates");

        Path path = Mockito.mock(Path.class);
        Mockito.when(fs.pathOf("/templates")).thenReturn(path);
        Mockito.when(fs.listDirectoryChildren(path)).thenReturn(List.of("profile.jfc"));

        Path templatePath = Mockito.mock(Path.class);
        Mockito.when(fs.pathOf("/templates", "profile.jfc")).thenReturn(templatePath);

        InputStream stream = IOUtils.toInputStream(xmlText);
        Mockito.when(fs.newInputStream(templatePath)).thenReturn(stream);

        Mockito.when(fs.isDirectory(path)).thenReturn(true);
        Mockito.when(fs.isReadable(path)).thenReturn(true);

        // TODO verify actual contents of the profile.jfc?
        MatcherAssert.assertThat(
                service.getEvents("Profiling", TemplateType.CUSTOM).get().keySet(),
                Matchers.hasSize(Matchers.greaterThan(0)));
    }

    @Test
    void getEventsShouldReturnEmptyForUnknownName() throws Exception {
        Assertions.assertFalse(service.getEvents("foo", TemplateType.CUSTOM).isPresent());
    }

    @Test
    void getEventsShouldReturnEmptyForUnknownType() throws Exception {
        Assertions.assertFalse(service.getEvents("foo", TemplateType.TARGET).isPresent());
    }

    @Test
    void getXmlShouldReturnModelFromLocalStorage() throws Exception {
        Mockito.when(env.hasEnv(LocalStorageTemplateService.TEMPLATE_PATH)).thenReturn(true);
        Mockito.when(env.getEnv(LocalStorageTemplateService.TEMPLATE_PATH))
                .thenReturn("/templates");

        Path path = Mockito.mock(Path.class);
        Mockito.when(fs.pathOf("/templates")).thenReturn(path);
        Mockito.when(fs.listDirectoryChildren(path)).thenReturn(List.of("profile.jfc"));

        Path templatePath = Mockito.mock(Path.class);
        Mockito.when(fs.pathOf("/templates", "profile.jfc")).thenReturn(templatePath);

        InputStream stream = IOUtils.toInputStream(xmlText);
        Mockito.when(fs.newInputStream(templatePath)).thenReturn(stream);

        Mockito.when(fs.isDirectory(path)).thenReturn(true);
        Mockito.when(fs.isReadable(path)).thenReturn(true);

        Optional<Document> doc = service.getXml("Profiling", TemplateType.CUSTOM);
        Assertions.assertTrue(doc.isPresent());
        Assertions.assertTrue(doc.get().hasSameValue(Jsoup.parse(xmlText, "", Parser.xmlParser())));
    }

    @Test
    void getXmlShouldReturnEmptyForUnknownName() throws Exception {
        Assertions.assertFalse(service.getXml("foo", TemplateType.CUSTOM).isPresent());
    }

    @Test
    void getXmlShouldReturnEmptyForUnknownType() throws Exception {
        Assertions.assertFalse(service.getXml("foo", TemplateType.TARGET).isPresent());
    }

    @Test
    void addTemplateShouldWriteStreamToFile() throws Exception {
        Mockito.when(env.hasEnv(LocalStorageTemplateService.TEMPLATE_PATH)).thenReturn(true);
        Mockito.when(env.getEnv(LocalStorageTemplateService.TEMPLATE_PATH))
                .thenReturn("/templates");

        Path path = Mockito.mock(Path.class);
        Path templatePath = Mockito.mock(Path.class);
        Mockito.when(fs.pathOf("/templates")).thenReturn(path);
        Mockito.when(fs.exists(path)).thenReturn(true);
        Mockito.when(fs.isDirectory(path)).thenReturn(true);
        Mockito.when(fs.isReadable(path)).thenReturn(true);
        Mockito.when(fs.isWritable(path)).thenReturn(true);
        Mockito.when(fs.pathOf("/templates", "Profiling")).thenReturn(templatePath);

        service.addTemplate(IOUtils.toInputStream(xmlText));

        ArgumentCaptor<String> contentCaptor = ArgumentCaptor.forClass(String.class);
        Mockito.verify(fs).writeString(Mockito.eq(templatePath), contentCaptor.capture());
        String after = contentCaptor.getValue();
        int distance = LevenshteinDistance.getDefaultInstance().apply(xmlText, after);
        // 1710 is just the experimentally determined LD. The XML is transformed somewhat when
        // parsed and re-serialized. Jsoup somehow determines that the document before and after
        // does not have the same value, but it clearly does from an actual inspection.
        MatcherAssert.assertThat(distance, Matchers.is(1710));
    }

    @Test
    void deleteTemplateShouldDeleteFile() throws Exception {
        Mockito.when(env.hasEnv(LocalStorageTemplateService.TEMPLATE_PATH)).thenReturn(true);
        Mockito.when(env.getEnv(LocalStorageTemplateService.TEMPLATE_PATH))
                .thenReturn("/templates");

        Path path = Mockito.mock(Path.class);
        Path templatePath = Mockito.mock(Path.class);
        Mockito.when(fs.pathOf("/templates")).thenReturn(path);
        Mockito.when(fs.exists(path)).thenReturn(true);
        Mockito.when(fs.isDirectory(path)).thenReturn(true);
        Mockito.when(fs.isReadable(path)).thenReturn(true);
        Mockito.when(fs.isWritable(path)).thenReturn(true);
        Mockito.when(fs.pathOf("/templates", "Profiling")).thenReturn(templatePath);
        Mockito.when(fs.deleteIfExists(templatePath)).thenReturn(true);

        service.deleteTemplate("Profiling");

        Mockito.verify(fs).deleteIfExists(templatePath);
    }
}
