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
package io.cryostat.core.reports;

import java.io.InputStream;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import io.cryostat.core.log.Logger;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ReportGeneratorTest {

    static final String SAMPLE_REPORT =
            "<html>"
                    + "<head></head>"
                    + "<body>"
                    + "<div class=\"foo\">"
                    + "<a id=\"hl\" href=\"http://example.com\">link text</a>"
                    + "</div>"
                    + "</body>"
                    + "</html>";

    @Mock Logger logger;
    @Mock Function<InputStream, String> reporter;
    @Mock InputStream recording;
    Set<ReportTransformer> transformers;
    ReportGenerator generator;

    @BeforeEach()
    void setup() {
        transformers = new HashSet<>();
        generator = new ReportGenerator(logger, reporter, transformers);
    }

    @Test
    void shouldReturnOriginalReportIfNoTransformers() {
        Mockito.when(reporter.apply(Mockito.any())).thenReturn(SAMPLE_REPORT);

        String report = generator.generateReport(recording);
        MatcherAssert.assertThat(report, Matchers.is(SAMPLE_REPORT));

        Mockito.verify(reporter).apply(Mockito.any());
        Mockito.verifyNoInteractions(logger);
    }

    @Test
    void shouldApplySingleReplacementTransformation() {
        Mockito.when(reporter.apply(Mockito.any())).thenReturn(SAMPLE_REPORT);

        generator.setTransformers(
                Set.of(
                        new ReportTransformer() {
                            @Override
                            public String innerHtml(String s) {
                                return s.replaceAll("link text", "new description");
                            }

                            @Override
                            public String selector() {
                                return "#hl";
                            }

                            @Override
                            public int priority() {
                                return 0;
                            }
                        }));

        String report = generator.generateReport(recording);
        MatcherAssert.assertThat(
                report,
                Matchers.is(
                        "<html>\n"
                                + " <head></head>\n"
                                + " <body>\n"
                                + "  <div class=\"foo\">\n"
                                + "   <a id=\"hl\" href=\"http://example.com\">new description</a>\n"
                                + "  </div>\n"
                                + " </body>\n"
                                + "</html>"));

        Mockito.verify(reporter).apply(Mockito.any());
        Mockito.verifyNoInteractions(logger);
    }

    @Test
    void shouldApplySingleAppendTransformation() {
        Mockito.when(reporter.apply(Mockito.any())).thenReturn(SAMPLE_REPORT);

        generator.setTransformers(
                Set.of(
                        new ReportTransformer() {
                            @Override
                            public String innerHtml(String s) {
                                return s + "<a href=\"http://localhost:1234\">hello</a>";
                            }

                            @Override
                            public Map<String, String> attributes() {
                                return Map.of("data-someprop", "someval");
                            }

                            @Override
                            public String selector() {
                                return ".foo";
                            }

                            @Override
                            public int priority() {
                                return 0;
                            }
                        }));

        String report = generator.generateReport(recording);
        MatcherAssert.assertThat(
                report,
                Matchers.is(
                        "<html>\n"
                                + " <head></head>\n"
                                + " <body>\n"
                                + "  <div class=\"foo\" data-someprop=\"someval\">\n"
                                + "   <a id=\"hl\" href=\"http://example.com\">link text</a>"
                                + "<a href=\"http://localhost:1234\">hello</a>\n"
                                + "  </div>\n"
                                + " </body>\n"
                                + "</html>"));

        Mockito.verify(reporter).apply(Mockito.any());
        Mockito.verifyNoInteractions(logger);
    }

    @Test
    void shouldApplyMultipleTransformationsAccordingToPriority() {
        Mockito.when(reporter.apply(Mockito.any()))
                .thenReturn(
                        "<html>"
                                + "<head></head>"
                                + "<body>"
                                + "<div>"
                                + "foo"
                                + "</div>"
                                + "</body>"
                                + "</html>");

        generator.setTransformers(
                Set.of(
                        new ReportTransformer() {
                            @Override
                            public String innerHtml(String s) {
                                return s.replaceAll("foo", "bar");
                            }

                            @Override
                            public String selector() {
                                return "div";
                            }

                            @Override
                            public int priority() {
                                return 0;
                            }
                        },
                        new ReportTransformer() {
                            @Override
                            public String innerHtml(String s) {
                                return s.replaceAll("123", "final");
                            }

                            @Override
                            public String selector() {
                                return "div";
                            }

                            @Override
                            public int priority() {
                                return 2;
                            }
                        },
                        new ReportTransformer() {
                            @Override
                            public String innerHtml(String s) {
                                return s.replaceAll("bar", "123");
                            }

                            @Override
                            public String selector() {
                                return "div";
                            }

                            @Override
                            public int priority() {
                                return 1;
                            }
                        }));

        String report = generator.generateReport(recording);
        MatcherAssert.assertThat(
                report,
                Matchers.is(
                        "<html>\n"
                                + " <head></head>\n"
                                + " <body>\n"
                                + "  <div>\n"
                                + "   final\n"
                                + "  </div>\n"
                                + " </body>\n"
                                + "</html>"));

        Mockito.verify(reporter).apply(Mockito.any());
        Mockito.verifyNoInteractions(logger);
    }
}
