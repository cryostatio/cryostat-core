package com.redhat.rhjmc.containerjfr.core.reports;

import java.io.InputStream;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import com.redhat.rhjmc.containerjfr.core.log.Logger;

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
        Mockito.verifyZeroInteractions(logger);
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
        Mockito.verifyZeroInteractions(logger);
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
                                + "   <a id=\"hl\" href=\"http://example.com\">link text</a>\n"
                                + "   <a href=\"http://localhost:1234\">hello</a>\n"
                                + "  </div>\n"
                                + " </body>\n"
                                + "</html>"));

        Mockito.verify(reporter).apply(Mockito.any());
        Mockito.verifyZeroInteractions(logger);
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
        Mockito.verifyZeroInteractions(logger);
    }
}
