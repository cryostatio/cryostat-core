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

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Function;

import io.cryostat.core.log.Logger;
import io.cryostat.core.reports.InterruptibleReportGenerator.ReportResult;
import io.cryostat.core.reports.InterruptibleReportGenerator.RuleEvaluation;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class InterruptibleReportGeneratorTest {

    @Mock Logger logger;
    @Mock Function<InputStream, String> reporter;
    @Mock InputStream recording;
    Set<ReportTransformer> transformers;
    InterruptibleReportGenerator generator;

    @BeforeEach()
    void setup() throws Exception {
        transformers = new HashSet<>();
        generator =
                new InterruptibleReportGenerator(
                        logger, transformers, Executors.newWorkStealingPool(1));
    }

    @Test
    void shouldProduceReport() throws Exception {
        try (InputStream is = new FileInputStream(getJfrFile())) {
            Future<ReportResult> report = generator.generateReportInterruptibly(is);
            MatcherAssert.assertThat(
                    report.get().getHtml(), Matchers.not(Matchers.emptyOrNullString()));
            MatcherAssert.assertThat(
                    report.get().getReportStats(), Matchers.not(Matchers.nullValue()));
        }
    }

    @Test
    void shouldBeCancellable() throws Exception {
        try (InputStream is = new FileInputStream(getJfrFile())) {
            Future<ReportResult> report = generator.generateReportInterruptibly(is);
            report.cancel(true);
            Assertions.assertThrows(CancellationException.class, report::get);
        }
    }

    @Test
    void shouldThrowNullRecording() throws Exception {
        Assertions.assertThrows(
                NullPointerException.class,
                () -> {
                    generator.generateReportInterruptibly(null).get();
                });
    }

    @Test
    void shouldThrowNullPredicate() throws Exception {
        try (InputStream is = new FileInputStream(getJfrFile())) {
            Assertions.assertThrows(
                    NullPointerException.class,
                    () -> {
                        generator.generateReportInterruptibly(is, null).get();
                    });
        }
    }

    @Test
    void shouldProduceReportWithFilteredRules() throws Exception {
        try (InputStream is = new FileInputStream(getJfrFile())) {
            Future<ReportResult> report =
                    generator.generateReportInterruptibly(
                            is, rule -> rule.getId() == "ClassLeak" || rule.getId() == "Errors");
            MatcherAssert.assertThat(
                    report.get().getHtml(), Matchers.not(Matchers.emptyOrNullString()));
            MatcherAssert.assertThat(
                    report.get().getReportStats(), Matchers.not(Matchers.nullValue()));
            MatcherAssert.assertThat(
                    report.get().getReportStats().rulesEvaluated, Matchers.equalTo(2));
        }
    }

    @Test
    void shouldProduceReportWithFilteredRulesAndTopics() throws Exception {
        try (InputStream is = new FileInputStream(getJfrFile())) {
            Future<ReportResult> report =
                    generator.generateReportInterruptibly(
                            is,
                            rule ->
                                    rule.getId() == "ClassLeak"
                                            || rule.getId() == "SystemGc"
                                            || rule.getTopic() == "garbage_collection");
            MatcherAssert.assertThat(
                    report.get().getHtml(), Matchers.not(Matchers.emptyOrNullString()));
            MatcherAssert.assertThat(
                    report.get().getReportStats(), Matchers.not(Matchers.nullValue()));
            MatcherAssert.assertThat(
                    report.get().getReportStats().rulesEvaluated, Matchers.equalTo(10));
        }
    }

    @Test
    void shouldProduceEmptyReport() throws Exception {
        try (InputStream is = new FileInputStream(getJfrFile())) {
            Future<ReportResult> report =
                    generator.generateReportInterruptibly(is, rule -> rule.getId() == "AFakeRule");
            MatcherAssert.assertThat(
                    report.get().getHtml(), Matchers.not(Matchers.emptyOrNullString()));
            MatcherAssert.assertThat(
                    report.get().getReportStats(), Matchers.not(Matchers.nullValue()));
            MatcherAssert.assertThat(
                    report.get().getReportStats().rulesEvaluated, Matchers.equalTo(0));
        }
    }

    @Test
    void shouldProduceEvalMap() throws Exception {
        try (InputStream is = new FileInputStream(getJfrFile())) {
            Future<Map<String, RuleEvaluation>> scoreMap =
                    generator.generateEvalMapInterruptibly(is, rule -> true);

            Map<String, RuleEvaluation> s = scoreMap.get();

            MatcherAssert.assertThat(s.entrySet(), Matchers.not(Matchers.empty()));
            for (var entry : s.entrySet()) {
                MatcherAssert.assertThat(
                        entry.getKey(), Matchers.not(Matchers.emptyOrNullString()));
                MatcherAssert.assertThat(
                        entry.getValue().getScore(), Matchers.not(Matchers.notANumber()));
                MatcherAssert.assertThat(
                        entry.getValue().getDescription(),
                        Matchers.not(Matchers.emptyOrNullString()));
            }
        }
    }

    @Test
    void shouldBeCancellableEvalMap() throws Exception {
        try (InputStream is = new FileInputStream(getJfrFile())) {
            Future<Map<String, RuleEvaluation>> scoreMap =
                    generator.generateEvalMapInterruptibly(is, rule -> true);

            scoreMap.cancel(true);
            Assertions.assertThrows(CancellationException.class, scoreMap::get);
        }
    }

    @Test
    void shouldProduceEvalMapWithFilteredRules() throws Exception {
        try (InputStream is = new FileInputStream(getJfrFile())) {
            Future<Map<String, RuleEvaluation>> scoreMap =
                    generator.generateEvalMapInterruptibly(is, rule -> rule.getId() == "ClassLeak");

            MatcherAssert.assertThat(
                    scoreMap.get().get("ClassLeak"), Matchers.not(Matchers.nullValue()));
            MatcherAssert.assertThat(scoreMap.get().size(), Matchers.equalTo(1));
        }
    }

    private synchronized File getJfrFile() throws Exception {
        return Paths.get(getClass().getResource("/profiling_sample.jfr").toURI()).toFile();
    }
}
