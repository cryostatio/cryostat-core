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
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.RunnableFuture;
import java.util.stream.Collectors;

import org.openjdk.jmc.common.io.IOToolkit;
import org.openjdk.jmc.common.item.IItemCollection;
import org.openjdk.jmc.common.util.IPreferenceValueProvider;
import org.openjdk.jmc.common.util.XmlToolkit;
import org.openjdk.jmc.flightrecorder.CouldNotLoadRecordingException;
import org.openjdk.jmc.flightrecorder.JfrLoaderToolkit;
import org.openjdk.jmc.flightrecorder.rules.IRule;
import org.openjdk.jmc.flightrecorder.rules.Result;
import org.openjdk.jmc.flightrecorder.rules.RuleRegistry;
import org.openjdk.jmc.flightrecorder.rules.report.html.internal.HtmlResultGroup;
import org.openjdk.jmc.flightrecorder.rules.report.html.internal.HtmlResultProvider;
import org.openjdk.jmc.flightrecorder.rules.report.html.internal.RulesHtmlToolkit;

import io.cryostat.core.log.Logger;

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
public class InterruptibleReportGenerator {

    private final Logger logger;
    private final Set<ReportTransformer> transformers;

    private final ExecutorService executor;
    private final ExecutorService qThread = Executors.newCachedThreadPool();

    public InterruptibleReportGenerator(
            Logger logger, Set<ReportTransformer> transformers, ExecutorService executor) {
        this.logger = logger;
        this.transformers = transformers;
        this.executor = executor;
    }

    public Future<String> generateReportInterruptibly(InputStream recording) {
        return qThread.submit(
                () -> {
                    // this is generally a re-implementation of JMC JfrHtmlRulesReport#createReport,
                    // but calling our cancellable evalute() method rather than the
                    // RulesToolkit.evaluateParallel as explained further down.
                    List<Future<Result>> resultFutures = new ArrayList<>();
                    try {
                        resultFutures.addAll(
                                evaluate(
                                                RuleRegistry.getRules(),
                                                JfrLoaderToolkit.loadEvents(recording))
                                        .stream()
                                        .map(executor::submit)
                                        .collect(Collectors.toList()));
                        Collection<Result> results = new HashSet<>();
                        for (Future<Result> future : resultFutures) {
                            results.add(future.get());
                        }

                        List<HtmlResultGroup> groups = loadResultGroups();

                        return transform(
                                RulesHtmlToolkit.generateStructuredHtml(
                                        new SimpleResultProvider(results, groups),
                                        groups,
                                        new HashMap<String, Boolean>(),
                                        true));
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
                        return "<html>"
                                + " <head></head>"
                                + " <body>"
                                + "  <div>"
                                + e.getMessage()
                                + "  </div>"
                                + " </body>"
                                + "</html>";
                    }
                });
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

    private List<Callable<Result>> evaluate(Collection<IRule> rules, IItemCollection items) {
        List<Callable<Result>> callables = new ArrayList<>(rules.size());
        for (IRule rule : rules) {
            RunnableFuture<Result> result =
                    rule.evaluate(items, IPreferenceValueProvider.DEFAULT_VALUES);
            callables.add(
                    () -> {
                        logger.trace("Processing rule {}", rule.getName());
                        // TODO it would be very nice to record a JFR event here to pick up in
                        // Cryostat's own profiling template, to compare the different rule
                        // implementations and the time they each take to process with various
                        // source recording settings
                        result.run();
                        return result.get();
                    });
        }
        return callables;
    }

    private static class SimpleResultProvider implements HtmlResultProvider {
        private Map<String, Collection<Result>> resultsByTopic = new HashMap<>();
        private Set<String> unmappedTopics;

        public SimpleResultProvider(Collection<Result> results, List<HtmlResultGroup> groups) {
            for (Result result : results) {
                String topic = result.getRule().getTopic();
                if (topic == null) {
                    // Magic string to denote null topic
                    topic = "";
                }
                Collection<Result> topicResults = resultsByTopic.get(topic);
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
        public Collection<Result> getResults(Collection<String> topics) {
            Collection<String> topics2 = topics;
            if (topics2.contains("")) {
                topics2 = new HashSet<>(topics);
                topics2.addAll(unmappedTopics);
            }
            Collection<Result> results = new HashSet<>();
            for (String topic : topics2) {
                Collection<Result> topicResults = resultsByTopic.get(topic);
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
}
