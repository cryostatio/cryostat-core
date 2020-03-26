package com.redhat.rhjmc.containerjfr.core.templates;

import java.util.Collections;

import org.apache.commons.io.IOUtils;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import org.openjdk.jmc.flightrecorder.configuration.internal.DefaultValueMap;
import org.openjdk.jmc.flightrecorder.configuration.internal.EventOptionDescriptorMapper;
import org.openjdk.jmc.flightrecorder.configuration.internal.EventTypeIDV2;
import org.openjdk.jmc.rjmx.services.jfr.IFlightRecorderService;

import com.redhat.rhjmc.containerjfr.core.FlightRecorderException;
import com.redhat.rhjmc.containerjfr.core.net.JFRConnection;

@ExtendWith(MockitoExtension.class)
class RemoteTemplateServiceTest {

    TemplateService templateSvc;
    @Mock JFRConnection conn;
    @Mock IFlightRecorderService svc;
    String xmlText;

    @BeforeEach
    void setup() throws Exception {
        xmlText = IOUtils.toString(this.getClass().getResourceAsStream("profile.jfc"));

        Mockito.when(conn.getService()).thenReturn(svc);
        Mockito.when(svc.getServerTemplates()).thenReturn(Collections.singletonList(xmlText));

        templateSvc = new RemoteTemplateService(conn);
    }

    @Test
    void getNamesShouldReflectRemoteTemplateNames() throws Exception {
        MatcherAssert.assertThat(
                templateSvc.getTemplates(),
                Matchers.equalTo(
                        Collections.singletonList(
                                new Template(
                                        "Profiling",
                                        "Low overhead configuration for profiling, typically around 2 % overhead.",
                                        "Oracle"))));
    }

    @Test
    void getEventsByNameShouldReturnNonEmptyMap() throws Exception {
        Mockito.when(svc.getDefaultEventOptions())
                .thenReturn(
                        new DefaultValueMap(
                                new EventOptionDescriptorMapper(
                                        EventTypeIDV2.class, Collections.emptyMap(), true)));
        // TODO verify actual contents of the profile.jfc?
        MatcherAssert.assertThat(
                templateSvc.getEventsByTemplateName("Profiling").keySet(),
                Matchers.hasSize(Matchers.greaterThan(0)));
    }

    @Test
    void getEventsByNameShouldThrowExceptionForUnknownName() throws Exception {
        Assertions.assertThrows(
                FlightRecorderException.class, () -> templateSvc.getEventsByTemplateName("foo"));
    }
}
