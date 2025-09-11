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

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;
import java.util.concurrent.RunnableFuture;

import org.openjdk.jmc.common.item.Aggregators;
import org.openjdk.jmc.common.item.IItem;
import org.openjdk.jmc.common.item.IItemCollection;
import org.openjdk.jmc.common.item.ItemFilters;
import org.openjdk.jmc.common.unit.IQuantity;
import org.openjdk.jmc.common.unit.UnitLookup;
import org.openjdk.jmc.common.util.IPreferenceValueProvider;
import org.openjdk.jmc.common.util.TypedPreference;
import org.openjdk.jmc.flightrecorder.JfrAttributes;
import org.openjdk.jmc.flightrecorder.rules.IResult;
import org.openjdk.jmc.flightrecorder.rules.IResultValueProvider;
import org.openjdk.jmc.flightrecorder.rules.IRule;
import org.openjdk.jmc.flightrecorder.rules.ResultBuilder;
import org.openjdk.jmc.flightrecorder.rules.Severity;
import org.openjdk.jmc.flightrecorder.rules.TypedResult;
import org.openjdk.jmc.flightrecorder.rules.util.RulesToolkit;
import org.openjdk.jmc.flightrecorder.rules.util.RulesToolkit.EventAvailability;
import org.openjdk.jmc.flightrecorder.rules.util.RulesToolkit.RequiredEventsBuilder;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

// see org.openjdk.jmc.flightrecorder.rules.jdk.io.SocketWriteRule
public class QuarkusRestRule implements IRule {

    private static final String RESULT_ID = "QuarkusREST";
    private static final String EVENT_TYPE_ID = "quarkus.Rest";

    // TODO localize these strings
    public static final TypedPreference<IQuantity> WRITE_INFO_LIMIT =
            new TypedPreference<>(
                    "io.quarkus.rest.info.limit",
                    "Quarkus REST event duration info limit",
                    "The shortest Quarkus REST event duration that should trigger an info notice",
                    UnitLookup.TIMESPAN,
                    UnitLookup.MILLISECOND.quantity(500));
    public static final TypedPreference<IQuantity> WRITE_WARNING_LIMIT =
            new TypedPreference<>(
                    "io.quarkus.rest.warning.limit",
                    "Quarkus REST event duration warning limit",
                    "The shortest Quarkus REST event duration that should trigger a warning",
                    UnitLookup.TIMESPAN,
                    UnitLookup.MILLISECOND.quantity(2000));

    private static final List<TypedPreference<?>> CONFIG_ATTRIBUTES =
            Arrays.<TypedPreference<?>>asList(WRITE_INFO_LIMIT, WRITE_WARNING_LIMIT);

    public static final TypedResult<IQuantity> LONGEST_RESPONSE_TIME =
            new TypedResult<>(
                    "longestResponseTime",
                    "Longest response (Time)",
                    "The longest time it took to complete a REST response.",
                    UnitLookup.TIMESPAN,
                    IQuantity.class);
    public static final TypedResult<IQuantity> AVERAGE_REST_RESPONSE =
            new TypedResult<>(
                    "averageRestResponse",
                    "Average REST response",
                    "The average duration of all REST responses.",
                    UnitLookup.TIMESPAN,
                    IQuantity.class);

    private static final Collection<TypedResult<?>> RESULT_ATTRIBUTES =
            Arrays.<TypedResult<?>>asList(
                    TypedResult.SCORE, LONGEST_RESPONSE_TIME, AVERAGE_REST_RESPONSE);

    private static final Map<String, EventAvailability> REQUIRED_EVENTS =
            RequiredEventsBuilder.create()
                    .addEventType(EVENT_TYPE_ID, EventAvailability.AVAILABLE)
                    .build();

    @Override
    public RunnableFuture<IResult> createEvaluation(
            final IItemCollection items,
            final IPreferenceValueProvider vp,
            final IResultValueProvider rp) {
        FutureTask<IResult> evaluationTask =
                new FutureTask<>(
                        new Callable<IResult>() {
                            @Override
                            public IResult call() throws Exception {
                                return evaluate(items, vp, rp);
                            }
                        });
        return evaluationTask;
    }

    private IResult evaluate(
            IItemCollection items, IPreferenceValueProvider vp, IResultValueProvider rp) {
        IQuantity infoLimit = vp.getPreferenceValue(WRITE_INFO_LIMIT);
        IQuantity warningLimit = vp.getPreferenceValue(WRITE_WARNING_LIMIT);
        items = items.apply(ItemFilters.type(EVENT_TYPE_ID));
        IItem longestEvent = items.getAggregate(Aggregators.itemWithMax(JfrAttributes.DURATION));
        // We had events, but all got filtered out - say ok, duration 0. Perhaps say "no matching"
        // or something similar.
        if (longestEvent == null) {
            return ResultBuilder.createFor(this, vp)
                    .setSeverity(Severity.OK)
                    .setSummary("There are no Quarkus REST events in this recording.")
                    .build();
        }

        IQuantity maxDuration = RulesToolkit.getValue(longestEvent, JfrAttributes.DURATION);
        double score =
                RulesToolkit.mapExp100(
                        maxDuration.doubleValueIn(UnitLookup.SECOND),
                        infoLimit.doubleValueIn(UnitLookup.SECOND),
                        warningLimit.doubleValueIn(UnitLookup.SECOND));

        Severity severity = Severity.get(score);
        if (severity == Severity.WARNING || severity == Severity.INFO) {
            IQuantity avgDuration =
                    items.getAggregate(Aggregators.avg(EVENT_TYPE_ID, JfrAttributes.DURATION));
            return ResultBuilder.createFor(this, vp)
                    .setSeverity(severity)
                    .setSummary(
                            "There are long REST responses pauses in this recording (the longest is"
                                    + " {longestResponseTime}).")
                    .setExplanation(
                            "The longest REST response took {longestResponseTime}. Average response"
                                    + " time: {averageRestResponse}.")
                    .addResult(TypedResult.SCORE, UnitLookup.NUMBER_UNITY.quantity(score))
                    .addResult(AVERAGE_REST_RESPONSE, avgDuration)
                    .addResult(LONGEST_RESPONSE_TIME, maxDuration)
                    .build();
        }
        return ResultBuilder.createFor(this, vp)
                .setSeverity(severity)
                .setSummary(
                        "No long REST responses were found in this recording (the longest was"
                                + " {longestResponseTime}).")
                .addResult(TypedResult.SCORE, UnitLookup.NUMBER_UNITY.quantity(score))
                .addResult(LONGEST_RESPONSE_TIME, maxDuration)
                .build();
    }

    @Override
    @SuppressFBWarnings("EI_EXPOSE_REP")
    public Collection<TypedPreference<?>> getConfigurationAttributes() {
        return CONFIG_ATTRIBUTES;
    }

    @Override
    public String getId() {
        return RESULT_ID;
    }

    @Override
    public String getName() {
        return "REST response duration";
    }

    @Override
    public String getTopic() {
        return "quarkus";
    }

    @Override
    @SuppressFBWarnings("EI_EXPOSE_REP")
    public Map<String, EventAvailability> getRequiredEvents() {
        return REQUIRED_EVENTS;
    }

    @Override
    @SuppressFBWarnings("EI_EXPOSE_REP")
    public Collection<TypedResult<?>> getResults() {
        return RESULT_ATTRIBUTES;
    }
}
