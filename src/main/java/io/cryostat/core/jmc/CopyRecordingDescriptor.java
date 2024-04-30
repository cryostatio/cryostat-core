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
package io.cryostat.core.jmc;

import java.util.Map;

import javax.management.ObjectName;

import org.openjdk.jmc.common.unit.IQuantity;
import org.openjdk.jmc.flightrecorder.configuration.IRecordingDescriptor;

public class CopyRecordingDescriptor implements IRecordingDescriptor {
    private final IRecordingDescriptor original;

    public CopyRecordingDescriptor(IRecordingDescriptor original) {
        this.original = original;
    }

    @Override
    public String getName() {
        return original.getName();
    }

    @Override
    public Long getId() {
        return original.getId();
    }

    @Override
    public RecordingState getState() {
        return original.getState();
    }

    @Override
    public Map<String, ?> getOptions() {
        return original.getOptions();
    }

    @Override
    public ObjectName getObjectName() {
        return original.getObjectName();
    }

    @Override
    public IQuantity getDataStartTime() {
        return original.getDataStartTime();
    }

    @Override
    public IQuantity getDataEndTime() {
        return original.getDataEndTime();
    }

    @Override
    public IQuantity getStartTime() {
        return original.getStartTime();
    }

    @Override
    public IQuantity getDuration() {
        return original.getDuration();
    }

    @Override
    public boolean isContinuous() {
        return original.isContinuous();
    }

    @Override
    public boolean getToDisk() {
        return original.getToDisk();
    }

    @Override
    public IQuantity getMaxSize() {
        return original.getMaxSize();
    }

    @Override
    public IQuantity getMaxAge() {
        return original.getMaxAge();
    }
}
