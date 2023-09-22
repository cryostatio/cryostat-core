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

import io.cryostat.core.log.Logger;
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

    @Mock InputStream recording;
    @Mock Logger logger;
    InterruptibleReportGenerator generator;

    @BeforeEach()
    void setup() throws Exception {
        generator = new InterruptibleReportGenerator(Executors.newWorkStealingPool(1), logger);
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
                        entry.getValue().getName(), Matchers.not(Matchers.emptyOrNullString()));
                MatcherAssert.assertThat(
                        entry.getValue().getTopic(), Matchers.not(Matchers.emptyOrNullString()));
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
