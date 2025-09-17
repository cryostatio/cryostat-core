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

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.file.Paths;
import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.openjdk.jmc.flightrecorder.rules.RuleRegistry;

import io.cryostat.core.reports.InterruptibleReportGenerator.AnalysisResult;

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

    private static final int CUSTOM_RULES_SIZE = 1; // QuarkusRestRule

    @Mock InputStream recording;

    InterruptibleReportGenerator generator;

    @BeforeEach()
    void setup() throws Exception {
        generator = new InterruptibleReportGenerator(Executors.newWorkStealingPool(1));
    }

    @Test
    void shouldProduceEvalMap() throws Exception {
        try (InputStream is = new FileInputStream(getJfrFile())) {
            Future<Map<String, AnalysisResult>> scoreMap =
                    generator.generateEvalMapInterruptibly(is, rule -> true);

            Map<String, AnalysisResult> s = scoreMap.get();

            MatcherAssert.assertThat(s.entrySet(), Matchers.not(Matchers.empty()));
            for (var entry : s.entrySet()) {
                MatcherAssert.assertThat(
                        entry.getKey(), Matchers.not(Matchers.emptyOrNullString()));
                MatcherAssert.assertThat(
                        entry.getValue().getName(), Matchers.not(Matchers.emptyOrNullString()));
                MatcherAssert.assertThat(
                        entry.getValue().getTopic(), Matchers.not(Matchers.emptyOrNullString()));
            }
        }
    }

    @Test
    void shouldBeCancellableEvalMap() throws Exception {
        try (InputStream is = new FileInputStream(getJfrFile())) {
            Future<Map<String, AnalysisResult>> scoreMap =
                    generator.generateEvalMapInterruptibly(is, rule -> true);

            scoreMap.cancel(true);
            Assertions.assertThrows(CancellationException.class, scoreMap::get);
        }
    }

    @Test
    void shouldProduceEvalMapWithFilteredRules() throws Exception {
        // with rule filtered out, result should always be -1.0 (N/A)
        try (InputStream is = new FileInputStream(getJfrFile())) {
            Future<Map<String, AnalysisResult>> scoreMap =
                    generator.generateEvalMapInterruptibly(
                            is, rule -> !"PID1Rule".equals(rule.getId()));

            MatcherAssert.assertThat(
                    scoreMap.get().size(),
                    Matchers.equalTo(RuleRegistry.getRules().size() + CUSTOM_RULES_SIZE));
            MatcherAssert.assertThat(
                    scoreMap.get().get("PID1Rule"), Matchers.not(Matchers.nullValue()));
            MatcherAssert.assertThat(
                    scoreMap.get().get("PID1Rule").getScore(), Matchers.equalTo(-1.0));
        }

        // when rule is not filtered, rule should be processed and score should be determined
        try (InputStream is = new FileInputStream(getJfrFile())) {
            Future<Map<String, AnalysisResult>> scoreMap =
                    generator.generateEvalMapInterruptibly(is, rule -> true);

            MatcherAssert.assertThat(
                    scoreMap.get().size(),
                    Matchers.equalTo(RuleRegistry.getRules().size() + CUSTOM_RULES_SIZE));
            MatcherAssert.assertThat(
                    scoreMap.get().get("PID1Rule").getScore(), Matchers.greaterThanOrEqualTo(0.0));
        }
    }

    private synchronized File getJfrFile() throws Exception {
        return Paths.get(getClass().getResource("/profiling_sample.jfr").toURI()).toFile();
    }
}
