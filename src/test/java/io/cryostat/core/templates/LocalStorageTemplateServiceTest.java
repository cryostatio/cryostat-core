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
import java.nio.charset.StandardCharsets;
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
    String profilingXmlText;
    String multiwordXmlText;

    @BeforeEach
    void setup() throws IOException {
        profilingXmlText =
                IOUtils.toString(
                        this.getClass().getResourceAsStream("profile.jfc"), StandardCharsets.UTF_8);
        multiwordXmlText =
                IOUtils.toString(
                        this.getClass().getResourceAsStream("multiword_label.jfc"),
                        StandardCharsets.UTF_8);
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
        Mockito.when(fs.pathOf(Mockito.eq("/templates"), Mockito.anyString()))
                .thenReturn(templatePath);

        InputStream stream = IOUtils.toInputStream(profilingXmlText, StandardCharsets.UTF_8);
        Mockito.when(fs.newInputStream(templatePath)).thenReturn(stream);

        Mockito.when(fs.isDirectory(path)).thenReturn(true);
        Mockito.when(fs.isReadable(path)).thenReturn(true);

        MatcherAssert.assertThat(
                service.getTemplates(),
                Matchers.equalTo(
                        Collections.singletonList(
                                new Template(
                                        "Profiling",
                                        "Low overhead configuration for profiling, typically around"
                                                + " 2 % overhead.",
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
        Mockito.when(fs.pathOf(Mockito.eq("/templates"), Mockito.anyString()))
                .thenReturn(templatePath);

        InputStream stream = IOUtils.toInputStream(profilingXmlText, StandardCharsets.UTF_8);
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
        Mockito.when(fs.pathOf(Mockito.eq("/templates"), Mockito.anyString()))
                .thenReturn(templatePath);

        InputStream stream = IOUtils.toInputStream(profilingXmlText, StandardCharsets.UTF_8);
        Mockito.when(fs.newInputStream(templatePath)).thenReturn(stream);

        Mockito.when(fs.isDirectory(path)).thenReturn(true);
        Mockito.when(fs.isReadable(path)).thenReturn(true);

        Optional<Document> doc = service.getXml("Profiling", TemplateType.CUSTOM);
        Assertions.assertTrue(doc.isPresent());
        Assertions.assertTrue(
                doc.get().hasSameValue(Jsoup.parse(profilingXmlText, "", Parser.xmlParser())));
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
        Mockito.when(fs.pathOf(Mockito.eq("/templates"), Mockito.anyString()))
                .thenReturn(templatePath);

        Template t =
                service.addTemplate(
                        IOUtils.toInputStream(profilingXmlText, StandardCharsets.UTF_8));

        ArgumentCaptor<String> contentCaptor = ArgumentCaptor.forClass(String.class);
        Mockito.verify(fs).writeString(Mockito.eq(templatePath), contentCaptor.capture());
        String after = contentCaptor.getValue();
        int distance = LevenshteinDistance.getDefaultInstance().apply(profilingXmlText, after);
        // 1710 is just the experimentally determined LD. The XML is transformed somewhat when
        // parsed and re-serialized. Jsoup somehow determines that the document before and after
        // does not have the same value, but it clearly does from an actual inspection.
        MatcherAssert.assertThat(distance, Matchers.is(1710));

        MatcherAssert.assertThat(t.getName(), Matchers.equalTo("Profiling"));
        MatcherAssert.assertThat(
                t.getDescription(),
                Matchers.equalTo(
                        "Low overhead configuration for profiling, typically around 2 %"
                                + " overhead."));
        MatcherAssert.assertThat(t.getProvider(), Matchers.equalTo("Oracle"));
        MatcherAssert.assertThat(t.getType(), Matchers.equalTo(TemplateType.CUSTOM));
    }

    @Test
    void addTemplateShouldSanitizeLabelAttribute() throws Exception {
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
        Mockito.when(fs.pathOf(Mockito.eq("/templates"), Mockito.anyString()))
                .thenReturn(templatePath);

        Template t =
                service.addTemplate(
                        IOUtils.toInputStream(multiwordXmlText, StandardCharsets.UTF_8));

        ArgumentCaptor<String> contentCaptor = ArgumentCaptor.forClass(String.class);
        Mockito.verify(fs).writeString(Mockito.eq(templatePath), contentCaptor.capture());
        String after = contentCaptor.getValue();
        int distance = LevenshteinDistance.getDefaultInstance().apply(profilingXmlText, after);
        // 1788 is just the experimentally determined LD. The XML is transformed somewhat when
        // parsed and re-serialized. Jsoup somehow determines that the document before and after
        // does not have the same value, but it clearly does from an actual inspection.
        MatcherAssert.assertThat(distance, Matchers.is(1788));

        // it's "Multiword Label" in the source document, and we expect the local storage template
        // to sanitize that and replace the whitespace with an underscore
        MatcherAssert.assertThat(t.getName(), Matchers.equalTo("Multiword_Label"));

        MatcherAssert.assertThat(
                t.getDescription(),
                Matchers.equalTo("Event Template with multiple words in the label"));
        MatcherAssert.assertThat(t.getProvider(), Matchers.equalTo("Cryostat"));
        MatcherAssert.assertThat(t.getType(), Matchers.equalTo(TemplateType.CUSTOM));
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
        Mockito.when(fs.pathOf(Mockito.eq("/templates"), Mockito.anyString()))
                .thenReturn(templatePath);
        Mockito.when(fs.deleteIfExists(templatePath)).thenReturn(true);

        service.deleteTemplate("Profiling");

        Mockito.verify(fs).deleteIfExists(templatePath);
    }
}
