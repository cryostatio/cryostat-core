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
package io.cryostat.core.util;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.RunnableFuture;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.openjdk.jmc.common.item.IItemCollection;
import org.openjdk.jmc.common.util.IPreferenceValueProvider;
import org.openjdk.jmc.common.util.TypedPreference;
import org.openjdk.jmc.flightrecorder.rules.IResult;
import org.openjdk.jmc.flightrecorder.rules.IResultValueProvider;
import org.openjdk.jmc.flightrecorder.rules.IRule;
import org.openjdk.jmc.flightrecorder.rules.TypedResult;
import org.openjdk.jmc.flightrecorder.rules.util.RulesToolkit.EventAvailability;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class RuleFilterParserTest {

    RuleFilterParser parser;
    @Mock Collection<IRule> iRules;
    Set<String> ruleSet;
    Set<String> topicSet;
    TestRule rule0, rule1, rule2, rule3, rule4;

    @BeforeEach
    void setup() {
        rule0 = new TestRule("Rule0", "Topic0");
        rule1 = new TestRule("Rule1", "Topic1");
        rule2 = new TestRule("Rule2", "Topic2");
        rule3 = new TestRule("Rule3", "Topic0");
        rule4 = new TestRule("Rule4", "Topic0");

        ruleSet = Set.of("Rule0", "Rule1", "Rule2", "Rule3", "Rule4");
        topicSet = Set.of("Topic0", "Topic1", "Topic2");

        Mockito.when(iRules.stream()).thenReturn(Stream.of(rule0, rule1, rule2, rule3, rule4));

        parser = new RuleFilterParser(ruleSet, topicSet);
    }

    @Test
    void shouldAcceptAllRules() {
        String rawFilter = "Rule0,Rule1, rule4,     rule2, rule3   ";

        Predicate<IRule> result = parser.parse(rawFilter);
        Collection<IRule> rules = iRules.stream().filter(result).collect(Collectors.toList());

        MatcherAssert.assertThat(result, Matchers.notNullValue());
        MatcherAssert.assertThat(rules, Matchers.hasSize(5));
    }

    @Test
    void shouldAcceptSomeRulesOfTopic() {
        String rawFilter = " topic0 ";

        Predicate<IRule> result = parser.parse(rawFilter);
        Collection<IRule> rules = iRules.stream().filter(result).collect(Collectors.toList());

        MatcherAssert.assertThat(result, Matchers.notNullValue());
        MatcherAssert.assertThat(rules, Matchers.hasSize(3));
    }

    @Test
    void shouldAcceptBothNoOverlap() {
        String rawFilter = "Topic2, Rule2, Rule4";

        Predicate<IRule> result = parser.parse(rawFilter);
        Collection<IRule> rules = iRules.stream().filter(result).collect(Collectors.toList());

        MatcherAssert.assertThat(result, Matchers.notNullValue());
        MatcherAssert.assertThat(rules, Matchers.hasSize(2));
    }

    @ParameterizedTest
    @NullAndEmptySource
    void shouldAcceptAllWhenFilterBlank(String rawFilter) {
        Predicate<IRule> result = parser.parse(rawFilter);
        Collection<IRule> rules = iRules.stream().filter(result).collect(Collectors.toList());

        MatcherAssert.assertThat(result, Matchers.notNullValue());
        MatcherAssert.assertThat(rules, Matchers.hasSize(5));
    }

    private class TestRule implements IRule {
        private String id;
        private String topic;

        private TestRule(String id, String topic) {
            this.id = id;
            this.topic = topic;
        }

        @Override
        public RunnableFuture<IResult> createEvaluation(
                IItemCollection items,
                IPreferenceValueProvider valueProvider,
                IResultValueProvider dependencyResults) {
            return null;
        }

        @Override
        public Collection<TypedPreference<?>> getConfigurationAttributes() {
            return null;
        }

        @Override
        public String getId() {
            return id;
        }

        @Override
        public String getName() {
            return null;
        }

        @Override
        public String getTopic() {
            return topic;
        }

        @Override
        public Map<String, EventAvailability> getRequiredEvents() {
            return null;
        }

        @Override
        public Collection<TypedResult<?>> getResults() {
            return null;
        }
    }
}
