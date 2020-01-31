package com.redhat.rhjmc.containerjfr.core.reports;

import java.io.IOException;
import java.io.InputStream;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;

import org.openjdk.jmc.flightrecorder.CouldNotLoadRecordingException;
import org.openjdk.jmc.flightrecorder.rules.report.html.JfrHtmlRulesReport;

import com.redhat.rhjmc.containerjfr.core.log.Logger;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

public class ReportGenerator {

    private final Logger logger;
    private final Function<InputStream, String> reporter;
    private final Set<ReportTransformer> transformers;

    public ReportGenerator(Logger logger, Set<ReportTransformer> transformers) {
        this(
                logger,
                is -> {
                    try {
                        return JfrHtmlRulesReport.createReport(is);
                    } catch (IOException | CouldNotLoadRecordingException e) {
                        logger.warn(e);
                        return "<html>"
                                + " <head></head>"
                                + " <body>"
                                + "  <div>"
                                + e.getMessage()
                                + "  </div>"
                                + " </body>"
                                + "</html>";
                    }
                },
                transformers);
    }

    // testing-only constructor
    ReportGenerator(
            Logger logger,
            Function<InputStream, String> reporter,
            Set<ReportTransformer> transformers) {
        this.logger = logger;
        this.reporter = reporter;
        this.transformers = new TreeSet<>(transformers);
    }

    // testing-only
    void setTransformers(Set<ReportTransformer> transformers) {
        this.transformers.clear();
        this.transformers.addAll(transformers);
    }

    public String generateReport(InputStream recording) {
        String report = reporter.apply(recording);
        if (!transformers.isEmpty()) {
            try {
                Document document = Jsoup.parse(report);
                transformers.forEach(
                        t -> {
                            document.select(t.selector())
                                    .forEach(
                                            el -> {
                                                el.html(t.innerHtml(el.html()));
                                                t.attributes()
                                                        .entrySet()
                                                        .forEach(
                                                                e ->
                                                                        el.attr(
                                                                                e.getKey(),
                                                                                e.getValue()));
                                            });
                        });
                return document.outerHtml();
            } catch (Exception e) {
                logger.warn(e);
            }
        }
        return report;
    }
}
