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
package io.cryostat.core.reports;

import java.io.IOException;
import java.io.InputStream;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;

import org.openjdk.jmc.flightrecorder.CouldNotLoadRecordingException;
import org.openjdk.jmc.flightrecorder.rules.report.html.JfrHtmlRulesReport;

import io.cryostat.core.log.Logger;

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
