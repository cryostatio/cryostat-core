/*-
 * #%L
 * Cryostat Core
 * %%
 * Copyright (C) 2020 - 2021 Cryostat
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

import java.util.Collections;
import java.util.Optional;

import org.openjdk.jmc.flightrecorder.configuration.internal.DefaultValueMap;
import org.openjdk.jmc.flightrecorder.configuration.internal.EventOptionDescriptorMapper;
import org.openjdk.jmc.flightrecorder.configuration.internal.EventTypeIDV2;
import org.openjdk.jmc.rjmx.services.jfr.IFlightRecorderService;

import io.cryostat.core.net.JFRConnection;

import org.apache.commons.io.IOUtils;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
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
    @Mock IFlightRecorderService svc;
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
                                        "Low overhead configuration for profiling, typically around 2 % overhead.",
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
        Optional<Document> doc = templateSvc.getXml("Profiling", TemplateType.TARGET);
        Assertions.assertTrue(doc.isPresent());
        Assertions.assertTrue(doc.get().hasSameValue(Jsoup.parse(xmlText, "", Parser.xmlParser())));
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
