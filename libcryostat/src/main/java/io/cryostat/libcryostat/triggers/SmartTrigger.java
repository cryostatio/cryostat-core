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
package io.cryostat.libcryostat.triggers;

import java.time.Duration;
import java.util.Date;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SmartTrigger {

    private static final String DURATION_PATTERN_STR =
            "(TargetDuration\\s*[<>=]+\\s*duration\\(['\"](\\d+[sSmMhH]+)['\"]\\))";
    private static final String DEFINITION_PATTERN_STR = "(.+)\\s*(?:;)\\s*" + DURATION_PATTERN_STR;
    private static final Pattern DEFINITION_PATTERN = Pattern.compile(DEFINITION_PATTERN_STR);

    public enum TriggerState {
        /* Newly Created or Condition not met. */
        NEW,
        /* Condition has been met but target Duration has not */
        WAITING_HIGH,
        /* Condition has not been met and target Duration has not been met */
        WAITING_LOW,
        /* Conditions have been met and recording has started */
        COMPLETE
    };

    // Unique UUID to identify the smart trigger
    private final String id;
    private final String expression;
    private final String durationConstraint;
    private final String triggerCondition;
    private final String recordingTemplateName;
    private final Duration targetDuration;
    /* Keep track of the time the condition was first met for
     * sustained durations
     */
    private volatile Date firstMetTime;
    private volatile TriggerState state;

    public SmartTrigger(String id, String expression, String templateName) {
        this.expression = expression;
        this.recordingTemplateName = templateName;
        this.id = id;
        this.state = TriggerState.NEW;
        Matcher m = DEFINITION_PATTERN.matcher(expression);
        if (m.matches()) {
            triggerCondition = m.group(1).replaceAll("\\s", "");
            durationConstraint = m.group(2).replaceAll("'", "\"").replaceAll("\\s", "");
            /* Duration.parse requires timestamps in ISO8601 Duration format */
            targetDuration = Duration.parse("PT" + m.group(3).replaceAll("\\s", ""));
        } else {
            triggerCondition = expression;
            durationConstraint = "";
            targetDuration = Duration.ZERO;
        }
        this.firstMetTime = new Date(0);
    }

    // Default Constructor for ObjectMapper Serialization
    public SmartTrigger() {
        this("", "", "");
    }

    public String getExpression() {
        return expression;
    }

    public TriggerState getState() {
        return state;
    }

    public void setState(TriggerState targetState) {
        this.state = targetState;
    }

    public String getRecordingTemplateName() {
        return recordingTemplateName;
    }

    public String getID() {
        return this.id;
    }

    public boolean isSimple() {
        return Duration.ZERO.equals(getTargetDuration());
    }

    public Duration getTargetDuration() {
        return targetDuration;
    }

    public void setTimeConditionFirstMet(Date date) {
        this.firstMetTime = new Date(date.getTime());
    }

    public Date getTimeConditionFirstMet() {
        return new Date(firstMetTime.getTime());
    }

    public String getTriggerCondition() {
        return triggerCondition;
    }

    public String getDurationConstraint() {
        return durationConstraint;
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                id,
                expression,
                durationConstraint,
                triggerCondition,
                recordingTemplateName,
                targetDuration);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        SmartTrigger other = (SmartTrigger) obj;
        return Objects.equals(id, other.id)
                && Objects.equals(expression, other.expression)
                && Objects.equals(durationConstraint, other.durationConstraint)
                && Objects.equals(triggerCondition, other.triggerCondition)
                && Objects.equals(recordingTemplateName, other.recordingTemplateName)
                && Objects.equals(targetDuration, other.targetDuration);
    }

    @Override
    public String toString() {
        return "SmartTrigger [id="
                + id
                + ", expression="
                + expression
                + ", durationConstraint="
                + durationConstraint
                + ", recordingTemplateName="
                + recordingTemplateName
                + ", targetDuration="
                + targetDuration
                + ", triggerCondition="
                + triggerCondition
                + "]";
    }
}
