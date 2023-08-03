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
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.RunnableFuture;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.openjdk.jmc.common.io.IOToolkit;
import org.openjdk.jmc.common.item.IItemCollection;
import org.openjdk.jmc.common.unit.IQuantity;
import org.openjdk.jmc.common.util.IPreferenceValueProvider;
import org.openjdk.jmc.common.util.Pair;
import org.openjdk.jmc.common.util.XmlToolkit;
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
import org.openjdk.jmc.flightrecorder.rules.report.html.internal.HtmlResultGroup;
import org.openjdk.jmc.flightrecorder.rules.report.html.internal.HtmlResultProvider;
import org.openjdk.jmc.flightrecorder.rules.report.html.internal.RulesHtmlToolkit;
import org.openjdk.jmc.flightrecorder.rules.util.RulesToolkit;

import io.cryostat.core.log.Logger;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jdk.jfr.Category;
import jdk.jfr.Event;
import jdk.jfr.Label;
import jdk.jfr.Name;
import org.apache.commons.io.input.CountingInputStream;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

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

    private final Logger logger;
    private final Set<ReportTransformer> transformers;

    private final ExecutorService executor;
    private final ExecutorService qThread = Executors.newCachedThreadPool();

    @SuppressFBWarnings(
            value = "EI_EXPOSE_REP2",
            justification = "fields are not exposed since there are no getters")
    public InterruptibleReportGenerator(
            Logger logger, Set<ReportTransformer> transformers, ExecutorService executor) {
        this.logger = logger;
        this.transformers = transformers;
        this.executor = executor;
    }

    public Future<ReportResult> generateReportInterruptibly(InputStream recording) {
        return generateReportInterruptibly(recording, rule -> true);
    }

    public Future<ReportResult> generateReportInterruptibly(
            InputStream recording, Predicate<IRule> predicate) {
        Objects.requireNonNull(recording);
        Objects.requireNonNull(predicate);
        return qThread.submit(
                () -> {
                    // this is generally a re-implementation of JMC JfrHtmlRulesReport#createReport,
                    // but calling our cancellable evaluate() method rather than the
                    // RulesToolkit.evaluateParallel as explained further down.
                    try {
                        Pair<Collection<IResult>, Long> helperPair =
                                generateResultHelper(recording, predicate);
                        Collection<IResult> results = helperPair.left;
                        List<HtmlResultGroup> groups = loadResultGroups();

                        String html =
                                transform(
                                        RulesHtmlToolkit.generateStructuredHtml(
                                                new SimpleResultProvider(results, groups),
                                                groups,
                                                new HashMap<String, Boolean>(),
                                                true));
                        long recordingSizeBytes = helperPair.right;
                        int rulesEvaluated = results.size();
                        int rulesApplicable =
                                results.stream()
                                        .filter(result -> result.getSeverity() != Severity.NA)
                                        .collect(Collectors.toList())
                                        .size();
                        return new ReportResult(
                                html,
                                new ReportStats(
                                        recordingSizeBytes, rulesEvaluated, rulesApplicable));

                    } catch (InterruptedException
                            | IOException
                            | ExecutionException
                            | CouldNotLoadRecordingException e) {
                        return new ReportResult(
                                "<html>"
                                        + " <head></head>"
                                        + " <body>"
                                        + "  <div>"
                                        + e.getMessage()
                                        + "  </div>"
                                        + " </body>"
                                        + "</html>");
                    }
                });
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

    String transform(String report) {
        if (transformers.isEmpty()) {
            return report;
        }
        try {
            org.jsoup.nodes.Document document = Jsoup.parse(report);
            transformers.forEach(
                    t -> {
                        document.select(t.selector())
                                .forEach(
                                        el -> {
                                            el.html(t.innerHtml(el.html()));
                                            t.attributes()
                                                    .entrySet()
                                                    .forEach(
                                                            e -> el.attr(e.getKey(), e.getValue()));
                                        });
                    });
            return document.outerHtml();
        } catch (Exception e) {
            logger.warn(e);
            return report;
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

    /**
     * Everything below this point is forked from JMC's JfrHtmlRulesReport and RulesToolkit classes,
     * with minor modifications to {@link #evaluate} (from {@link
     * org.openjdk.jmc.flightrecorder.rules.report.html.internal.RulesHtmlToolkit#evaluateParallel()})
     * to use the ExecutorService field rather than manually-created/started Threads, and to allow
     * cancellation of the ongoing rule evaluations.
     */
    private List<HtmlResultGroup> loadResultGroups() {
        InputStream is = ReportGenerator.class.getResourceAsStream("/resultgroups.xml");
        Document document;
        try {
            document = XmlToolkit.loadDocumentFromStream(is);
        } catch (SAXException e) {
            logger.warn("Could not parse result groups: {}", e.getMessage());
            document = createEmptyGroupsDocument();
        } catch (IOException e) {
            logger.warn("Could not read result groups file: {}", e.getMessage());
            document = createEmptyGroupsDocument();
        } finally {
            IOToolkit.closeSilently(is);
        }
        Element element = document.getDocumentElement();
        return loadResultGroups(element);
    }

    private Document createEmptyGroupsDocument() {
        try {
            return XmlToolkit.createNewDocument("groups");
        } catch (IOException e) {
            logger.error("Internal error while creating empty XML", e);
            return null;
        }
    }

    private List<HtmlResultGroup> loadResultGroups(Element element) {
        List<HtmlResultGroup> groups = new ArrayList<>();

        NodeList childList = element.getChildNodes();
        for (int i = 0; i < childList.getLength(); i++) {
            Node childNode = childList.item(i);
            if (childNode.getNodeName().equals("group") && childNode instanceof Element) {
                groups.add(new SimpleResultGroup((Element) childNode));
            }
        }
        return groups;
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

    private static class SimpleResultProvider implements HtmlResultProvider {
        private Map<String, Collection<IResult>> resultsByTopic = new HashMap<>();
        private Set<String> unmappedTopics;

        public SimpleResultProvider(Collection<IResult> results, List<HtmlResultGroup> groups) {
            for (IResult result : results) {
                String topic = result.getRule().getTopic();
                if (topic == null) {
                    // Magic string to denote null topic
                    topic = "";
                }
                Collection<IResult> topicResults = resultsByTopic.get(topic);
                if (topicResults == null) {
                    topicResults = new HashSet<>();
                    resultsByTopic.put(topic, topicResults);
                }
                topicResults.add(result);
            }

            unmappedTopics = new HashSet<>(resultsByTopic.keySet());
            removeMappedTopics(unmappedTopics, groups);
        }

        private static void removeMappedTopics(
                Set<String> unmappedTopics, List<HtmlResultGroup> groups) {
            for (HtmlResultGroup group : groups) {
                for (String topic : group.getTopics()) {
                    unmappedTopics.remove(topic);
                }
                removeMappedTopics(unmappedTopics, group.getChildren());
            }
        }

        @Override
        public Collection<IResult> getResults(Collection<String> topics) {
            Collection<String> topics2 = topics;
            if (topics2.contains("")) {
                topics2 = new HashSet<>(topics);
                topics2.addAll(unmappedTopics);
            }
            Collection<IResult> results = new HashSet<>();
            for (String topic : topics2) {
                Collection<IResult> topicResults = resultsByTopic.get(topic);
                if (topicResults != null) {
                    results.addAll(topicResults);
                }
            }
            return results;
        }
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

    private static class SimpleResultGroup implements HtmlResultGroup {
        String name;
        String image = null;
        List<HtmlResultGroup> children = new ArrayList<>();
        List<String> topics = new ArrayList<>();

        public SimpleResultGroup(Element element) {
            name = element.getAttribute("name");
            if (element.hasAttribute("image")) {
                image = element.getAttribute("image");
            }

            NodeList childList = element.getChildNodes();
            for (int i = 0; i < childList.getLength(); i++) {
                Node childNode = childList.item(i);
                if (childNode instanceof Element) {
                    Element childElement = (Element) childNode;
                    if (childElement.getNodeName().equals("topic")
                            && childElement.hasAttribute("name")) {
                        topics.add(childElement.getAttribute("name"));
                    } else if (childElement.getNodeName().equals("group")) {
                        children.add(new SimpleResultGroup(childElement));
                    }
                }
            }
        }

        @Override
        public String getId() {
            return Integer.toString(hashCode());
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public String getImage() {
            return image;
        }

        @Override
        public List<HtmlResultGroup> getChildren() {
            return children;
        }

        @Override
        public boolean hasChildren() {
            return !children.isEmpty();
        }

        @Override
        public Collection<String> getTopics() {
            return topics;
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

    public static class ReportResult {
        String html;
        ReportStats reportStats;

        ReportResult(String html) {
            this.html = html;
            this.reportStats = new ReportStats(0, 0, 0);
        }

        ReportResult(String html, ReportStats reportStats) {
            this.html = html;
            this.reportStats = reportStats;
        }

        public String getHtml() {
            return this.html;
        }

        public ReportStats getReportStats() {
            return this.reportStats;
        }
    }

    public static class ReportStats {
        long recordingSizeBytes;
        int rulesEvaluated;
        int rulesApplicable;

        ReportStats(long recordingSizeBytes, int rulesEvaluated, int rulesApplicable) {
            this.recordingSizeBytes = recordingSizeBytes;
            this.rulesEvaluated = rulesEvaluated;
            this.rulesApplicable = rulesApplicable;
        }

        public long getRecordingSizeBytes() {
            return this.recordingSizeBytes;
        }

        public int getRulesEvaluated() {
            return this.rulesEvaluated;
        }

        public int getRulesApplicable() {
            return this.rulesApplicable;
        }
    }
}
