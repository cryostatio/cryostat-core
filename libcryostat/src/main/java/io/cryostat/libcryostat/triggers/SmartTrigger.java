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

public class SmartTrigger {

    public enum TriggerState {
        /* Newly Created or Condition not met. */
        NEW,
        /* Condition has been met but target Duration has not */
        WAITING_HIGH,
        /* Condition has not been met and target Duration has not been met */
        WAITING_LOW,
        /* Conditions have been met and recording has started */
        RECORDING_ACTIVE,
        /* Conditions no longer met, waiting for threshold */
        RECORDING_STOPPING,
        /* All conditions met, can be removed */
        COMPLETE
    };

    // Unique UUID to identify the smart trigger
    private final String id;
    private final String triggerCondition;
    private final String recordingTemplateName;
    private final Duration targetDuration;
    private final String stopCondition;
    /* Keep track of the time the condition was first met for
     * sustained durations
     */
    private volatile Date firstMetTime;
    private volatile TriggerState state;
    private volatile long stopDuration;
    private volatile Date stopMetTime;
    private volatile long executionTarget;

    public SmartTrigger(
            String id,
            String expression,
            String stopCondition,
            long duration,
            long stopDuration,
            long executionTarget,
            String templateName) {
        this.recordingTemplateName = templateName;
        this.id = id;
        this.state = TriggerState.NEW;
        triggerCondition = expression;
        targetDuration = Duration.ofMillis(duration);
        this.firstMetTime = new Date(0);
        this.stopMetTime = new Date(0);
        this.executionTarget = executionTarget;
        this.stopCondition = stopCondition;
        this.stopDuration = stopDuration;
    }

    // Default Constructor for ObjectMapper Serialization
    public SmartTrigger() {
        this("", "", "", 0, 0, -1, "");
    }

    public long getStopDuration() {
        return stopDuration;
    }

    public void setStopDuration(long duration) {
        this.stopDuration = duration;
    }

    public String getStopCondition() {
        return stopCondition;
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

    public void setTimeStopConditionFirstMet(Date date) {
        this.stopMetTime = new Date(date.getTime());
    }

    public Date getTimeStopConditionFirstMet() {
        return new Date(stopMetTime.getTime());
    }

    public void setExecutionTarget(long target) {
        this.executionTarget = target;
    }

    public long getExecutionTarget() {
        return executionTarget;
    }

    public String getTriggerCondition() {
        return triggerCondition;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, triggerCondition, recordingTemplateName, targetDuration);
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
                && Objects.equals(triggerCondition, other.triggerCondition)
                && Objects.equals(recordingTemplateName, other.recordingTemplateName)
                && Objects.equals(targetDuration, other.targetDuration);
    }

    @Override
    public String toString() {
        return "SmartTrigger [id="
                + id
                + ", recordingTemplateName="
                + recordingTemplateName
                + ", targetDuration="
                + targetDuration
                + ", triggerCondition="
                + triggerCondition
                + "]";
    }
}
