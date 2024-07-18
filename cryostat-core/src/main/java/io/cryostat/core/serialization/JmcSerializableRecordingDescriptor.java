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
import org.openjdk.jmc.flightrecorder.configuration.IRecordingDescriptor;
import org.openjdk.jmc.flightrecorder.configuration.IRecordingDescriptor.RecordingState;

import io.cryostat.libcryostat.serialization.SerializableRecordingDescriptor;

public class JmcSerializableRecordingDescriptor extends SerializableRecordingDescriptor {

    public JmcSerializableRecordingDescriptor() {}

    public JmcSerializableRecordingDescriptor(IRecordingDescriptor orig)
            throws QuantityConversionException {
        this.id = orig.getId();
        this.name = orig.getName();
        this.state = reverseMapRecordingState(orig.getState());
        this.startTime = orig.getStartTime().longValueIn(UnitLookup.EPOCH_MS);
        this.duration = orig.getDuration().longValueIn(UnitLookup.MILLISECOND);
        this.continuous = orig.isContinuous();
        this.toDisk = orig.getToDisk();
        this.maxSize = orig.getMaxSize().longValueIn(UnitLookup.BYTE);
        this.maxAge = orig.getMaxAge().longValueIn(UnitLookup.MILLISECOND);
    }

    public JmcSerializableRecordingDescriptor(JmcSerializableRecordingDescriptor o) {
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

    /**
     * @see {@link org.openjdk.jmc.rjmx.services.jfr.internal.RecordingDescriptorV2#decideState}
     */
    private static RecordingState mapRecordingState(jdk.jfr.RecordingState s) {
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

    private static jdk.jfr.RecordingState reverseMapRecordingState(RecordingState s) {
        switch (s) {
            case CREATED:
                return jdk.jfr.RecordingState.NEW;
            case RUNNING:
                return jdk.jfr.RecordingState.RUNNING;
            case STOPPED:
                return jdk.jfr.RecordingState.STOPPED;
            case STOPPING:
                return jdk.jfr.RecordingState.RUNNING;
            default:
                // better not to return null here for NPE safety, but this may not always be
                // accurate
                return jdk.jfr.RecordingState.STOPPED;
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
                return mapRecordingState(JmcSerializableRecordingDescriptor.this.state);
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

    public RecordingState getJmcRecordingState() {
        return mapRecordingState(this.state);
    }
}
