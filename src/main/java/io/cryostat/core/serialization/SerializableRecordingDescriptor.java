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
package io.cryostat.core.serialization;

import java.util.Map;

import javax.management.ObjectName;

import org.openjdk.jmc.common.unit.IQuantity;
import org.openjdk.jmc.common.unit.QuantityConversionException;
import org.openjdk.jmc.common.unit.UnitLookup;
import org.openjdk.jmc.rjmx.services.jfr.IRecordingDescriptor;
import org.openjdk.jmc.rjmx.services.jfr.IRecordingDescriptor.RecordingState;

import jdk.jfr.Recording;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

public class SerializableRecordingDescriptor {

    protected long id;
    protected String name;
    protected RecordingState state;
    protected long startTime;
    protected long duration;
    protected boolean continuous;
    protected boolean toDisk;
    protected long maxSize;
    protected long maxAge;

    public SerializableRecordingDescriptor(
            long id,
            String name,
            RecordingState state,
            long startTime,
            long duration,
            boolean continuous,
            boolean toDisk,
            long maxSize,
            long maxAge) {
        this.id = id;
        this.name = name;
        this.state = state;
        this.startTime = startTime;
        this.duration = duration;
        this.continuous = continuous;
        this.toDisk = toDisk;
        this.maxSize = maxSize;
        this.maxAge = maxAge;
    }

    public SerializableRecordingDescriptor(Recording recording) {
        this(
                recording.getId(),
                recording.getName(),
                mapRecordingStateState(recording.getState()),
                recording.getStartTime() == null ? 0 : recording.getStartTime().toEpochMilli(),
                recording.getDuration() == null ? 0 : recording.getDuration().toMillis(),
                recording.getDuration() == null,
                recording.isToDisk(),
                recording.getMaxSize(),
                recording.getMaxAge() == null ? 0 : recording.getMaxAge().toMillis());
    }

    public SerializableRecordingDescriptor(IRecordingDescriptor orig)
            throws QuantityConversionException {
        this.id = orig.getId();
        this.name = orig.getName();
        this.state = orig.getState();
        this.startTime = orig.getStartTime().longValueIn(UnitLookup.EPOCH_MS);
        this.duration = orig.getDuration().longValueIn(UnitLookup.MILLISECOND);
        this.continuous = orig.isContinuous();
        this.toDisk = orig.getToDisk();
        this.maxSize = orig.getMaxSize().longValueIn(UnitLookup.BYTE);
        this.maxAge = orig.getMaxAge().longValueIn(UnitLookup.MILLISECOND);
    }

    public SerializableRecordingDescriptor(SerializableRecordingDescriptor o) {
        this.id = o.getId();
        this.name = o.getName();
        this.state = o.getState();
        this.startTime = o.getStartTime();
        this.duration = o.getDuration();
        this.continuous = o.isContinuous();
        this.toDisk = o.getToDisk();
        this.maxSize = o.getMaxSize();
        this.maxAge = o.getMaxAge();
    }

    protected final void finalize() {}

    /**
     * @see {@link org.openjdk.jmc.rjmx.services.jfr.internal.RecordingDescriptorV2#decideState}
     */
    private static RecordingState mapRecordingStateState(jdk.jfr.RecordingState s) {
        switch (s) {
            case NEW:
                return RecordingState.CREATED;
            case DELAYED:
                return RecordingState.RUNNING;
            case RUNNING:
                return RecordingState.RUNNING;
            case STOPPED:
                return RecordingState.STOPPED;
            default:
                // better not to return null here for NPE safety, but this may not always be
                // accurate
                return RecordingState.STOPPED;
        }
    }

    public IRecordingDescriptor toJmcForm() {
        return new IRecordingDescriptor() {

            @Override
            public Long getId() {
                return id;
            }

            @Override
            public String getName() {
                return name;
            }

            @Override
            public RecordingState getState() {
                return state;
            }

            @Override
            public Map<String, ?> getOptions() {
                return Map.of();
            }

            @Override
            public ObjectName getObjectName() {
                return null;
            }

            @Override
            public IQuantity getDataStartTime() {
                return getStartTime();
            }

            @Override
            public IQuantity getDataEndTime() {
                return getStartTime().add(getDuration());
            }

            @Override
            public IQuantity getStartTime() {
                return UnitLookup.EPOCH_MS.quantity(startTime);
            }

            @Override
            public IQuantity getDuration() {
                return UnitLookup.MILLISECOND.quantity(duration);
            }

            @Override
            public boolean isContinuous() {
                return continuous;
            }

            @Override
            public boolean getToDisk() {
                return toDisk;
            }

            @Override
            public IQuantity getMaxSize() {
                return UnitLookup.BYTE.quantity(maxSize);
            }

            @Override
            public IQuantity getMaxAge() {
                return UnitLookup.MILLISECOND.quantity(maxAge);
            }
        };
    }

    public long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public RecordingState getState() {
        return state;
    }

    public long getStartTime() {
        return startTime;
    }

    public long getDuration() {
        return duration;
    }

    public boolean isContinuous() {
        return continuous;
    }

    public boolean getToDisk() {
        return toDisk;
    }

    public long getMaxSize() {
        return maxSize;
    }

    public long getMaxAge() {
        return maxAge;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }

    @Override
    public int hashCode() {
        return HashCodeBuilder.reflectionHashCode(this);
    }

    @Override
    public boolean equals(Object o) {
        return EqualsBuilder.reflectionEquals(this, o);
    }
}
