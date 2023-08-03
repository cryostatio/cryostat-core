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
