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

import java.util.Collections;
import java.util.Optional;

import org.openjdk.jmc.flightrecorder.configuration.internal.DefaultValueMap;
import org.openjdk.jmc.flightrecorder.configuration.internal.EventOptionDescriptorMapper;
import org.openjdk.jmc.flightrecorder.configuration.internal.EventTypeIDV2;

import io.cryostat.core.net.CryostatFlightRecorderService;
import io.cryostat.core.net.JFRConnection;

import org.apache.commons.io.IOUtils;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.jsoup.Jsoup;
import org.jsoup.parser.Parser;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RemoteTemplateServiceTest {

    TemplateService templateSvc;
    @Mock JFRConnection conn;
    @Mock CryostatFlightRecorderService svc;
    String xmlText;

    @BeforeEach
    void setup() throws Exception {
        xmlText = IOUtils.toString(this.getClass().getResourceAsStream("profile.jfc"));
        templateSvc = new RemoteTemplateService(conn);
    }

    @Test
    void getNamesShouldReflectRemoteTemplateNames() throws Exception {
        Mockito.when(conn.getService()).thenReturn(svc);
        Mockito.when(svc.getServerTemplates()).thenReturn(Collections.singletonList(xmlText));
        MatcherAssert.assertThat(
                templateSvc.getTemplates(),
                Matchers.equalTo(
                        Collections.singletonList(
                                new Template(
                                        "Profiling",
                                        "Low overhead configuration for profiling, typically around"
                                                + " 2 % overhead.",
                                        "Oracle",
                                        TemplateType.TARGET))));
    }

    @Test
    void getEventsShouldReturnNonEmptyMap() throws Exception {
        Mockito.when(conn.getService()).thenReturn(svc);
        Mockito.when(svc.getServerTemplates()).thenReturn(Collections.singletonList(xmlText));
        Mockito.when(svc.getDefaultEventOptions())
                .thenReturn(
                        new DefaultValueMap(
                                new EventOptionDescriptorMapper(
                                        EventTypeIDV2.class, Collections.emptyMap(), true)));
        // TODO verify actual contents of the profile.jfc?
        MatcherAssert.assertThat(
                templateSvc.getEvents("Profiling", TemplateType.TARGET).get().keySet(),
                Matchers.hasSize(Matchers.greaterThan(0)));
    }

    @Test
    void getEventsShouldReturnEmptyForUnknownName() throws Exception {
        Mockito.when(conn.getService()).thenReturn(svc);
        Mockito.when(svc.getServerTemplates()).thenReturn(Collections.singletonList(xmlText));
        Assertions.assertFalse(templateSvc.getEvents("foo", TemplateType.TARGET).isPresent());
    }

    @Test
    void getEventsShouldReturnEmptyForUnknownType() throws Exception {
        Assertions.assertFalse(templateSvc.getEvents("foo", TemplateType.CUSTOM).isPresent());
    }

    @Test
    void getXmlShouldReturnModelFromRemote() throws Exception {
        Mockito.when(conn.getService()).thenReturn(svc);
        Mockito.when(svc.getServerTemplates()).thenReturn(Collections.singletonList(xmlText));
        Optional<String> doc = templateSvc.getXml("Profiling", TemplateType.TARGET);
        Assertions.assertTrue(doc.isPresent());
        Assertions.assertTrue(
                doc.get().equals(Jsoup.parse(xmlText, "", Parser.xmlParser()).outerHtml()));
    }

    @Test
    void getXmlShouldReturnEmptyForUnknownName() throws Exception {
        Mockito.when(conn.getService()).thenReturn(svc);
        Mockito.when(svc.getServerTemplates()).thenReturn(Collections.singletonList(xmlText));
        Assertions.assertFalse(templateSvc.getXml("foo", TemplateType.TARGET).isPresent());
    }

    @Test
    void getXmlShouldReturnEmptyForUnknownType() throws Exception {
        Assertions.assertFalse(templateSvc.getXml("foo", TemplateType.CUSTOM).isPresent());
    }
}
