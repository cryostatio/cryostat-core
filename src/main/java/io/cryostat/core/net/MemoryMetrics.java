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
package io.cryostat.core.net;

import java.lang.management.MemoryUsage;
import java.util.Map;

import org.apache.commons.lang3.builder.ToStringBuilder;

public class MemoryMetrics {
    private MemoryUtilization heapMemoryUsage;
    private MemoryUtilization nonHeapMemoryUsage;
    private long objectPendingFinalizationCount;
    private long freeHeapMemory;
    private long freeNonHeapMemory;
    private double heapMemoryUsagePercent;
    private boolean verbose;

    public MemoryMetrics() {}

    public MemoryMetrics(Map<String, Object> attributes) {
        this.heapMemoryUsage =
                MemoryUtilization.from(
                        (MemoryUsage)
                                attributes.getOrDefault(
                                        "HeapMemoryUsage", new MemoryUsage(-1, 0, 0, -1)));
        this.nonHeapMemoryUsage =
                MemoryUtilization.from(
                        (MemoryUsage)
                                attributes.getOrDefault(
                                        "NonHeapMemoryUsage", new MemoryUsage(-1, 0, 0, -1)));
        this.objectPendingFinalizationCount =
                (int) attributes.getOrDefault("ObjectPendingFinalizationCount", Integer.MIN_VALUE);
        this.freeHeapMemory = (long) attributes.getOrDefault("FreeHeapMemory", Long.MIN_VALUE);
        this.freeNonHeapMemory =
                (long) attributes.getOrDefault("FreeNonHeapMemory", Long.MIN_VALUE);
        this.heapMemoryUsagePercent =
                (double) attributes.getOrDefault("HeapMemoryUsagePercent", Double.MIN_VALUE);
        this.verbose = (boolean) attributes.getOrDefault("Verbose", false);
    }

    public MemoryUtilization getHeapMemoryUsage() {
        return heapMemoryUsage;
    }

    public MemoryUtilization getNonHeapMemoryUsage() {
        return nonHeapMemoryUsage;
    }

    public long getObjectPendingFinalizationCount() {
        return objectPendingFinalizationCount;
    }

    public long getFreeHeapMemory() {
        return freeHeapMemory;
    }

    public long getFreeNonHeapMemory() {
        return freeNonHeapMemory;
    }

    public double getHeapMemoryUsagePercent() {
        return heapMemoryUsagePercent;
    }

    public boolean isVerbose() {
        return verbose;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("heapMemoryUsage", heapMemoryUsage)
                .append("nonHeapMemoryUsage", nonHeapMemoryUsage)
                .append("objectPendingFinalizationCount", objectPendingFinalizationCount)
                .append("freeHeapMemory", freeHeapMemory)
                .append("freeNonHeapMemory", freeNonHeapMemory)
                .append("heapMemoryUsagePercent", heapMemoryUsagePercent)
                .append("verbose", verbose)
                .build();
    }
}
