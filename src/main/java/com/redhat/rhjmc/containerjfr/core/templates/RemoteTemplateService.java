package com.redhat.rhjmc.containerjfr.core.templates;

import java.io.IOException;
import java.text.ParseException;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.openjdk.jmc.common.unit.IConstrainedMap;
import org.openjdk.jmc.flightrecorder.configuration.events.EventOptionID;
import org.openjdk.jmc.flightrecorder.controlpanel.ui.configuration.model.xml.XMLModel;
import org.openjdk.jmc.flightrecorder.controlpanel.ui.configuration.model.xml.XMLTagInstance;
import org.openjdk.jmc.flightrecorder.controlpanel.ui.model.EventConfiguration;

import com.redhat.rhjmc.containerjfr.core.FlightRecorderException;
import com.redhat.rhjmc.containerjfr.core.log.Logger;
import com.redhat.rhjmc.containerjfr.core.net.JFRConnection;

public class RemoteTemplateService implements TemplateService {

    private final JFRConnection conn;

    public RemoteTemplateService(JFRConnection conn) {
        this.conn = conn;
    }

    @Override
    public List<Template> getTemplates() throws FlightRecorderException {
        try {
            return getRemoteTemplateModels().stream()
                    .map(xml -> xml.getRoot())
                    .map(
                            root ->
                                    new Template(
                                            getAttributeValue(root, "label"),
                                            getAttributeValue(root, "description"),
                                            getAttributeValue(root, "provider")))
                    .collect(Collectors.toList());
        } catch (Exception e) {
            throw new FlightRecorderException(e);
        }
    }

    @Override
    public IConstrainedMap<EventOptionID> getEventsByTemplateName(String templateName)
            throws FlightRecorderException {
        try {
            XMLModel model =
                    getRemoteTemplateModels().stream()
                            .filter(
                                    m ->
                                            m.getRoot().getAttributeInstances().stream()
                                                    .anyMatch(
                                                            attr ->
                                                                    attr.getAttribute()
                                                                                    .getName()
                                                                                    .equals("label")
                                                                            && attr.getValue()
                                                                                    .equals(
                                                                                            templateName)))
                            .findFirst()
                            .orElseThrow(() -> new UnknownEventTemplateException(templateName));

            return new EventConfiguration(model)
                    .getEventOptions(
                            conn.getService().getDefaultEventOptions().emptyWithSameConstraints());
        } catch (Exception e) {
            throw new FlightRecorderException(e);
        }
    }

    private List<XMLModel> getRemoteTemplateModels() throws FlightRecorderException {
        try {
            return conn.getService().getServerTemplates().stream()
                    .map(
                            xmlText -> {
                                try {
                                    return EventConfiguration.createModel(xmlText);
                                } catch (ParseException | IOException e) {
                                    Logger.INSTANCE.warn(e);
                                    return null;
                                }
                            })
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            throw new FlightRecorderException(e);
        }
    }

    private String getAttributeValue(XMLTagInstance node, String valueKey) {
        return node.getAttributeInstances().stream()
                .filter(i -> Objects.equals(valueKey, i.getAttribute().getName()))
                .map(i -> i.getValue())
                .findFirst()
                .get();
    }
}
