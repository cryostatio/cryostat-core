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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.openjdk.jmc.flightrecorder.rules.IRule;
import org.openjdk.jmc.flightrecorder.rules.RuleRegistry;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RuleFilterParser {

    public static final String ALL_WILDCARD_TOKEN = "*";
    public static final String NEGATION_PREFIX_TOKEN = "!";

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final Set<String> ruleIds;
    private final Set<String> ruleTopics;

    RuleFilterParser(Set<String> rules, Set<String> topics) {
        this.ruleIds = rules;
        this.ruleTopics = topics;
    }

    public RuleFilterParser() {
        this.ruleIds =
                RuleRegistry.getRules().stream()
                        .map(rule -> rule.getId())
                        .collect(Collectors.toSet());
        this.ruleTopics =
                RuleRegistry.getRules().stream()
                        .map(rule -> rule.getTopic())
                        .collect(Collectors.toSet());
    }

    public Predicate<IRule> parse(String rawFilter) {
        if (StringUtils.isBlank(rawFilter)) {
            return (r) -> true;
        }
        SortedSet<String> keys = new TreeSet<>(new FilterComparator());
        keys.addAll(
                Arrays.asList(rawFilter.split(",")).stream()
                        .map(String::strip)
                        .collect(Collectors.toSet()));
        Predicate<IRule> combinedPredicate = (r) -> false;
        for (String key : keys) {
            boolean negated = key.startsWith(NEGATION_PREFIX_TOKEN);
            if (negated) {
                key = key.substring(1);
            }
            final String fKey = key;
            Predicate<IRule> predicate;
            if (ALL_WILDCARD_TOKEN.equals(fKey)) {
                predicate = (rule) -> true;
            } else if (ruleIds.stream().anyMatch(key::equalsIgnoreCase)) {
                predicate = (rule) -> rule.getId().equalsIgnoreCase(fKey);
            } else if (ruleTopics.stream().anyMatch(key::equalsIgnoreCase)) {
                predicate = (rule) -> rule.getTopic().equalsIgnoreCase(fKey);
            } else {
                logger.warn(
                        "Filter \"{}\" did not match any known rule IDs or topics, ignoring.", key);
                continue;
            }
            if (negated) {
                combinedPredicate = combinedPredicate.and(predicate.negate());
            } else {
                combinedPredicate = combinedPredicate.or(predicate);
            }
        }
        return combinedPredicate;
    }

    static class FilterComparator implements Comparator<String>, Serializable {
        @Override
        public int compare(String a, String b) {
            if (ALL_WILDCARD_TOKEN.equals(a)) {
                return -1;
            }
            if (ALL_WILDCARD_TOKEN.equals(b)) {
                return 1;
            }
            if (a != null && b != null) {
                if (a.startsWith(NEGATION_PREFIX_TOKEN)) {
                    return 1;
                }
                if (b.startsWith(NEGATION_PREFIX_TOKEN)) {
                    return -1;
                }
            }
            return StringUtils.compare(a, b);
        }
    }

    public static class Builder {
        private final RuleFilterParser rfp;
        private final List<String> l = new ArrayList<>();

        private Builder(RuleFilterParser rfp) {
            this.rfp = rfp;
        }

        public Builder acceptAll() {
            return with(RuleFilterParser.ALL_WILDCARD_TOKEN);
        }

        public Builder with(String s) {
            l.add(s);
            return this;
        }

        public Builder without(String s) {
            return with(RuleFilterParser.NEGATION_PREFIX_TOKEN + s);
        }

        public Predicate<IRule> build() {
            return rfp.parse(String.join(",", l));
        }

        public static Builder create() {
            return create(new RuleFilterParser());
        }

        static Builder create(RuleFilterParser rfp) {
            return new Builder(rfp);
        }
    }
}
