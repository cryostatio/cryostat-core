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

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.RunnableFuture;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.openjdk.jmc.common.item.IItemCollection;
import org.openjdk.jmc.common.unit.IQuantity;
import org.openjdk.jmc.common.util.IPreferenceValueProvider;
import org.openjdk.jmc.common.util.Pair;
import org.openjdk.jmc.flightrecorder.CouldNotLoadRecordingException;
import org.openjdk.jmc.flightrecorder.JfrLoaderToolkit;
import org.openjdk.jmc.flightrecorder.rules.DependsOn;
import org.openjdk.jmc.flightrecorder.rules.IResult;
import org.openjdk.jmc.flightrecorder.rules.IRule;
import org.openjdk.jmc.flightrecorder.rules.ResultBuilder;
import org.openjdk.jmc.flightrecorder.rules.ResultProvider;
import org.openjdk.jmc.flightrecorder.rules.ResultToolkit;
import org.openjdk.jmc.flightrecorder.rules.RuleRegistry;
import org.openjdk.jmc.flightrecorder.rules.Severity;
import org.openjdk.jmc.flightrecorder.rules.TypedResult;
import org.openjdk.jmc.flightrecorder.rules.util.RulesToolkit;

import io.cryostat.core.log.Logger;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jdk.jfr.Category;
import jdk.jfr.Event;
import jdk.jfr.Label;
import jdk.jfr.Name;
import org.apache.commons.io.input.CountingInputStream;
import org.apache.commons.lang3.StringUtils;

/**
 * Re-implementation of {@link ReportGenerator} where the report generation task is represented by a
 * {@link Future}, allowing callers to cancel ongoing rules report analyses or to easily time out on
 * analysis requests. This should eventually replace {@link ReportGenerator} entirely - there should
 * only be benefits to using this implementation.
 */
@SuppressFBWarnings(
        value = "THROWS_METHOD_THROWS_CLAUSE_BASIC_EXCEPTION",
        justification = "There are no basic exceptions being thrown")
public class InterruptibleReportGenerator {

    private final ExecutorService qThread = Executors.newCachedThreadPool();
    private final ExecutorService executor;
    private final Logger logger;

    @SuppressFBWarnings(
            value = "EI_EXPOSE_REP2",
            justification = "fields are not exposed since there are no getters")
    public InterruptibleReportGenerator(ExecutorService executor, Logger logger) {
        this.executor = executor;
        this.logger = logger;
    }

    public Future<Map<String, RuleEvaluation>> generateEvalMapInterruptibly(
            InputStream recording, Predicate<IRule> predicate) {
        Objects.requireNonNull(recording);
        Objects.requireNonNull(predicate);
        return qThread.submit(
                () -> {
                    try {
                        Collection<IResult> results =
                                generateResultHelper(recording, predicate).left;
                        Map<String, RuleEvaluation> evalMap = new HashMap<String, RuleEvaluation>();
                        for (var eval : results) {
                            IQuantity scoreQuantity = eval.getResult(TypedResult.SCORE);
                            double score;
                            if (scoreQuantity != null) {
                                score = scoreQuantity.doubleValue();
                            } else {
                                score = eval.getSeverity().getLimit();
                            }
                            evalMap.put(
                                    eval.getRule().getId(),
                                    new RuleEvaluation(
                                            score,
                                            eval.getRule().getName(),
                                            eval.getRule().getTopic(),
                                            getTextDescription(eval)));
                        }
                        return evalMap;
                    } catch (InterruptedException
                            | IOException
                            | ExecutionException
                            | CouldNotLoadRecordingException e) {
                        return Map.of(
                                e.getClass().toString(),
                                new RuleEvaluation(
                                        Severity.NA.getLimit(),
                                        e.getClass().getSimpleName(),
                                        "report_failure",
                                        e.getMessage()));
                    }
                });
    }

    private String getTextDescription(IResult result) {
        StringBuilder sb = new StringBuilder();

        if (StringUtils.isNotBlank(result.getSummary())) {
            sb.append("Summary:\n");
            sb.append(ResultToolkit.populateMessage(result, result.getSummary(), false));
            sb.append("\n\n");
        }

        if (StringUtils.isNotBlank(result.getExplanation())) {
            sb.append("Explanation:\n");
            sb.append(ResultToolkit.populateMessage(result, result.getExplanation(), false));
            sb.append("\n\n");
        }

        if (StringUtils.isNotBlank(result.getSolution())) {
            sb.append("Solution:\n");
            sb.append(ResultToolkit.populateMessage(result, result.getSolution(), false));
            sb.append("\n\n");
        }

        if (!result.suggestRecordingSettings().isEmpty()) {
            sb.append("Suggested settings:\n");
            for (var suggestion : result.suggestRecordingSettings()) {
                sb.append(
                        String.format(
                                "%s %s=%s",
                                suggestion.getSettingFor(),
                                suggestion.getSettingName(),
                                suggestion.getSettingValue()));
            }
        }

        return sb.toString().strip();
    }

    private Pair<Collection<IResult>, Long> generateResultHelper(
            InputStream recording, Predicate<IRule> predicate)
            throws InterruptedException, IOException, ExecutionException,
                    CouldNotLoadRecordingException {
        Collection<IRule> rules =
                RuleRegistry.getRules().stream().filter(predicate).collect(Collectors.toList());
        ResultProvider resultProvider = new ResultProvider();
        Map<IRule, Future<IResult>> resultFutures = new HashMap<>();
        Queue<RunnableFuture<IResult>> futureQueue = new ConcurrentLinkedQueue<>();
        // Map using the rule name as a key, and a Pair containing the rule (left) and it's
        // dependency (right)
        Map<String, Pair<IRule, IRule>> rulesWithDependencies = new HashMap<>();
        Map<IRule, IResult> computedResults = new HashMap<>();
        try (CountingInputStream countingRecordingStream = new CountingInputStream(recording)) {
            IItemCollection items = JfrLoaderToolkit.loadEvents(countingRecordingStream);
            for (IRule rule : rules) {
                if (RulesToolkit.matchesEventAvailabilityMap(items, rule.getRequiredEvents())) {
                    if (hasDependency(rule)) {
                        IRule depRule =
                                rules.stream()
                                        .filter(r -> r.getId().equals(getRuleDependencyName(rule)))
                                        .findFirst()
                                        .orElse(null);
                        rulesWithDependencies.put(rule.getId(), new Pair<>(rule, depRule));
                    } else {
                        RunnableFuture<IResult> resultFuture =
                                rule.createEvaluation(
                                        items,
                                        IPreferenceValueProvider.DEFAULT_VALUES,
                                        resultProvider);
                        resultFutures.put(rule, resultFuture);
                        futureQueue.add(resultFuture);
                    }
                } else {
                    resultFutures.put(
                            rule,
                            CompletableFuture.completedFuture(
                                    ResultBuilder.createFor(
                                                    rule, IPreferenceValueProvider.DEFAULT_VALUES)
                                            .setSeverity(Severity.NA)
                                            .build()));
                }
            }
            for (Entry<String, Pair<IRule, IRule>> entry : rulesWithDependencies.entrySet()) {
                IRule rule = entry.getValue().left;
                IRule depRule = entry.getValue().right;
                Future<IResult> depResultFuture = resultFutures.get(depRule);
                if (depResultFuture == null) {
                    resultFutures.put(
                            rule,
                            CompletableFuture.completedFuture(
                                    ResultBuilder.createFor(
                                                    rule, IPreferenceValueProvider.DEFAULT_VALUES)
                                            .setSeverity(Severity.NA)
                                            .build()));
                } else {
                    IResult depResult = null;
                    if (!depResultFuture.isDone()) {
                        ((Runnable) depResultFuture).run();
                        try {
                            depResult = depResultFuture.get();
                            resultProvider.addResults(depResult);
                            computedResults.put(depRule, depResult);
                        } catch (InterruptedException | ExecutionException e) {
                            logger.warn("Error retrieving results for rule: " + depResult);
                        }
                    } else {
                        depResult = computedResults.get(depRule);
                    }
                    if (depResult != null && shouldEvaluate(rule, depResult)) {
                        RunnableFuture<IResult> resultFuture =
                                rule.createEvaluation(
                                        items,
                                        IPreferenceValueProvider.DEFAULT_VALUES,
                                        resultProvider);
                        resultFutures.put(rule, resultFuture);
                        futureQueue.add(resultFuture);
                    } else {
                        resultFutures.put(
                                rule,
                                CompletableFuture.completedFuture(
                                        ResultBuilder.createFor(
                                                        rule,
                                                        IPreferenceValueProvider.DEFAULT_VALUES)
                                                .setSeverity(Severity.NA)
                                                .build()));
                    }
                }
            }
            RuleEvaluator re = new RuleEvaluator(futureQueue);
            executor.submit(re);
            Collection<IResult> results = new HashSet<IResult>();
            for (Future<IResult> future : resultFutures.values()) {
                results.add(future.get());
            }
            return new Pair<Collection<IResult>, Long>(
                    results, countingRecordingStream.getByteCount());
        } catch (InterruptedException
                | IOException
                | ExecutionException
                | CouldNotLoadRecordingException e) {
            for (Future f : resultFutures.values()) {
                if (!f.isDone()) {
                    f.cancel(true);
                }
            }
            logger.warn(e);
            throw e;
        }
    }

    public static class RuleEvaluation {
        private double score;
        private String name;
        private String topic;
        private String description;

        RuleEvaluation(double score, String name, String topic, String description) {
            this.score = score;
            this.name = name;
            this.topic = topic;
            this.description = description;
        }

        public double getScore() {
            return score;
        }

        public String getName() {
            return name;
        }

        public String getTopic() {
            return topic;
        }

        public String getDescription() {
            return description;
        }
    }

    private static String getRuleDependencyName(IRule rule) {
        DependsOn dependency = rule.getClass().getAnnotation(DependsOn.class);
        Class<? extends IRule> dependencyType = dependency.value();
        return dependencyType.getSimpleName();
    }

    private static boolean hasDependency(IRule rule) {
        DependsOn dependency = rule.getClass().getAnnotation(DependsOn.class);
        return dependency != null;
    }

    /** Brought over from org.openjdk.jmc.flightrecorder.rules.jdk.util.RulesToolkit */
    private static boolean shouldEvaluate(IRule rule, IResult depResult) {
        DependsOn dependency = rule.getClass().getAnnotation(DependsOn.class);
        if (dependency != null) {
            if (depResult.getSeverity().compareTo(dependency.severity()) < 0) {
                return false;
            }
        }
        return true;
    }

    private static class RuleEvaluator implements Runnable {
        private Queue<RunnableFuture<IResult>> futureQueue;

        public RuleEvaluator(Queue<RunnableFuture<IResult>> futureQueue) {
            this.futureQueue = futureQueue;
        }

        @Override
        public void run() {
            RunnableFuture<IResult> resultFuture;
            while ((resultFuture = futureQueue.poll()) != null) {
                resultFuture.run();
            }
        }
    }

    @Name("io.cryostat.core.reports.InterruptibleReportGenerator.ReportRuleEvalEvent")
    @Label("Report Rule Evaluation")
    @Category("Cryostat")
    @SuppressFBWarnings(
            value = "URF_UNREAD_FIELD",
            justification = "The event fields are recorded with JFR instead of accessed directly")
    public static class ReportRuleEvalEvent extends Event {

        String ruleName;

        ReportRuleEvalEvent(String ruleName) {
            this.ruleName = ruleName;
        }
    }

    @Name("io.cryostat.core.reports.InterruptibleReportGenerator.ReportGenerationEvent")
    @Label("Report Generation")
    @Category("Cryostat")
    @SuppressFBWarnings(
            value = "URF_UNREAD_FIELD",
            justification = "The event fields are recorded with JFR instead of accessed directly")
    public static class ReportGenerationEvent extends Event {

        String recordingName;
        int rulesEvaluated;
        int rulesApplicable;
        long recordingSizeBytes;

        public ReportGenerationEvent(String recordingName) {
            this.recordingName = recordingName;
        }

        public void setRulesEvaluated(int rulesEvaluated) {
            this.rulesEvaluated = rulesEvaluated;
        }

        public void setRulesApplicable(int rulesApplicable) {
            this.rulesApplicable = rulesApplicable;
        }

        public void setRecordingSizeBytes(long recordingSizeBytes) {
            this.recordingSizeBytes = recordingSizeBytes;
        }
    }
}
