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

import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.openjdk.jmc.flightrecorder.rules.IRule;
import org.openjdk.jmc.flightrecorder.rules.RuleRegistry;

import org.apache.commons.lang3.StringUtils;

public class RuleFilterParser {
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
        if (StringUtils.isNotBlank(rawFilter)) {
            String[] filterArray = rawFilter.split(",");
            Predicate<IRule> combinedPredicate = (r) -> false;
            for (String filter : filterArray) {
                String cleanFilter = filter.trim();
                if (ruleIds.stream().anyMatch(cleanFilter::equalsIgnoreCase)) {
                    Predicate<IRule> pr = (rule) -> rule.getId().equalsIgnoreCase(cleanFilter);
                    combinedPredicate = combinedPredicate.or(pr);
                } else if (ruleTopics.stream().anyMatch(cleanFilter::equalsIgnoreCase)) {
                    Predicate<IRule> pr = (rule) -> rule.getTopic().equalsIgnoreCase(cleanFilter);
                    combinedPredicate = combinedPredicate.or(pr);
                }
            }
            return combinedPredicate;
        } else {
            return (r) -> true;
        }
    }
}
