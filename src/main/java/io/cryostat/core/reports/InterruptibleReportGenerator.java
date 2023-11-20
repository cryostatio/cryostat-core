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
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
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
import org.openjdk.jmc.flightrecorder.rules.IRecordingSetting;
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
import org.apache.commons.io.input.CountingInputStream;

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

    public Future<Map<String, AnalysisResult>> generateEvalMapInterruptibly(
            InputStream recording, Predicate<IRule> predicate) {
        Objects.requireNonNull(recording);
        Objects.requireNonNull(predicate);
        return qThread.submit(
                () -> {
                    try {
                        Collection<IResult> results =
                                generateResultHelper(recording, predicate).left;
                        Map<String, AnalysisResult> evalMap = new HashMap<String, AnalysisResult>();
                        for (var eval : results) {
                            IQuantity scoreQuantity = eval.getResult(TypedResult.SCORE);
                            double score;
                            if (scoreQuantity != null) {
                                score = scoreQuantity.doubleValue();
                            } else {
                                score = eval.getSeverity().getLimit();
                            }
                            evalMap.put(eval.getRule().getId(), new AnalysisResult(score, eval));
                        }
                        return evalMap;
                    } catch (InterruptedException
                            | IOException
                            | ExecutionException
                            | CouldNotLoadRecordingException e) {
                        throw new CompletionException(e);
                    }
                });
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
            for (Future<?> f : resultFutures.values()) {
                if (!f.isDone()) {
                    f.cancel(true);
                }
            }
            logger.warn(e);
            throw e;
        }
    }

    public static class AnalysisResult {
        private String name;
        private String topic;
        private double score;
        private Evaluation evaluation;

        AnalysisResult() {}

        AnalysisResult(String name, String topic, double score, Evaluation evaluation) {
            this.name = name;
            this.topic = topic;
            this.score = score;
            this.evaluation = evaluation;
        }

        AnalysisResult(double score, IResult result) {
            this(
                    result.getRule().getName(),
                    result.getRule().getTopic(),
                    score,
                    new Evaluation(result));
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

        public Evaluation getEvaluation() {
            return evaluation;
        }

        public static class Evaluation {
            private String summary;
            private String explanation;
            private String solution;
            private List<Suggestion> suggestions;

            Evaluation() {}

            Evaluation(IResult result) {
                this.summary = ResultToolkit.populateMessage(result, result.getSummary(), false);
                this.explanation =
                        ResultToolkit.populateMessage(result, result.getExplanation(), false);
                this.solution = ResultToolkit.populateMessage(result, result.getSolution(), false);
                this.suggestions =
                        result.suggestRecordingSettings().stream()
                                .map(Suggestion::new)
                                .collect(Collectors.toList());
            }

            public String getSummary() {
                return summary;
            }

            public String getExplanation() {
                return explanation;
            }

            public String getSolution() {
                return solution;
            }

            public List<Suggestion> getSuggestions() {
                return Collections.unmodifiableList(suggestions);
            }

            public static class Suggestion {
                private String name;
                private String setting;
                private String value;

                Suggestion() {}

                Suggestion(IRecordingSetting setting) {
                    this.name = setting.getSettingName();
                    this.setting = setting.getSettingFor();
                    this.value = setting.getSettingValue();
                }

                public String getName() {
                    return name;
                }

                public String getSetting() {
                    return setting;
                }

                public String getValue() {
                    return value;
                }
            }
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
}
