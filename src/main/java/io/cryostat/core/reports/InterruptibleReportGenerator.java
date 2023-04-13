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

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.RunnableFuture;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.openjdk.jmc.common.io.IOToolkit;
import org.openjdk.jmc.common.item.IItemCollection;
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
                            evalMap.put(
                                    eval.getRule().getId(),
                                    new RuleEvaluation(
                                            eval.getSeverity().getLimit(),
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
        List<Future<IResult>> resultFutures = new ArrayList<>();
        try (CountingInputStream countingRecordingStream = new CountingInputStream(recording)) {
            Collection<IRule> rules =
                    RuleRegistry.getRules().stream().filter(predicate).collect(Collectors.toList());
            resultFutures.addAll(
                    evaluate(rules, JfrLoaderToolkit.loadEvents(countingRecordingStream)).stream()
                            .map(executor::submit)
                            .collect(Collectors.toList()));
            Collection<IResult> results = new HashSet<>();
            for (Future<IResult> future : resultFutures) {
                results.add(future.get());
            }
            long recordingSizeBytes = countingRecordingStream.getByteCount();
            return new Pair<Collection<IResult>, Long>(results, recordingSizeBytes);
        } catch (InterruptedException
                | IOException
                | ExecutionException
                | CouldNotLoadRecordingException e) {
            resultFutures.forEach(
                    f -> {
                        if (!f.isDone()) {
                            f.cancel(true);
                        }
                    });
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

    private List<Callable<IResult>> evaluate(Collection<IRule> rules, IItemCollection items) {
        List<Callable<IResult>> callables = new ArrayList<>(rules.size());
        ResultProvider rp = new ResultProvider();
        Map<Class<? extends IRule>, Severity> evaluatedRules = new HashMap<>();
        for (IRule rule : rules) {
            RunnableFuture<IResult> resultFuture =
                    rule.createEvaluation(items, IPreferenceValueProvider.DEFAULT_VALUES, rp);
            callables.add(
                    () -> {
                        // Check that we can evaluate this rule first, some rules have dependencies
                        // on other rules
                        // and trying to run them too early will throw an NPE from trying to
                        // access entries in the ResultProvider
                        if (shouldEvaluate(evaluatedRules, rule)) {
                            IResult result;
                            logger.trace("Processing rule {}", rule.getName());
                            ReportRuleEvalEvent evt = new ReportRuleEvalEvent(rule.getName());
                            evt.begin();
                            try {
                                // Check that the rule has all of the events it needs to evaluate
                                // first
                                if (!RulesToolkit.matchesEventAvailabilityMap(
                                        items, rule.getRequiredEvents())) {
                                    logger.warn(
                                            "Rule missing required events: {} ", rule.getName());
                                    result =
                                            ResultBuilder.createFor(
                                                            rule,
                                                            IPreferenceValueProvider.DEFAULT_VALUES)
                                                    .setSeverity(Severity.NA)
                                                    .build();
                                } else {
                                    resultFuture.run();
                                    result = resultFuture.get();
                                }
                                evaluatedRules.put(rule.getClass(), result.getSeverity());
                                rp.addResults(result);
                                return result;
                            } finally {
                                evt.end();
                                if (evt.shouldCommit()) {
                                    evt.commit();
                                }
                            }
                        } else {
                            logger.warn("Rule is missing dependencies: {} ", rule.getName());
                            return ResultBuilder.createFor(
                                            rule, IPreferenceValueProvider.DEFAULT_VALUES)
                                    .setSeverity(Severity.NA)
                                    .build();
                        }
                    });
        }
        return callables;
    }

    /** Brought over from org.openjdk.jmc.flightrecorder.rules.jdk.test.TestRulesWithJFR */
    private static boolean shouldEvaluate(
            Map<Class<? extends IRule>, Severity> evaluatedRules, IRule rule) {
        DependsOn dependency = rule.getClass().getAnnotation(DependsOn.class);
        if (dependency != null) {
            Class<? extends IRule> dependencyType = dependency.value();
            if (dependencyType != null) {
                if (evaluatedRules.containsKey(dependencyType)) {
                    if (evaluatedRules.get(dependencyType).compareTo(dependency.severity()) < 0) {
                        return false;
                    }
                    return true;
                }
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
