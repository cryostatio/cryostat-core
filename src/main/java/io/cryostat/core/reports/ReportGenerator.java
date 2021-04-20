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
package io.cryostat.core.reports;

import java.io.IOException;
import java.io.InputStream;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import org.openjdk.jmc.flightrecorder.CouldNotLoadRecordingException;
import org.openjdk.jmc.flightrecorder.rules.report.html.JfrHtmlRulesReport;

import io.cryostat.core.log.Logger;

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
