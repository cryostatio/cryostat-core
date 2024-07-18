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
package io.cryostat.libcryostat.net;

import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;

public class OperatingSystemMetrics {
    private String arch;
    private int availableProcessors;
    private String name;
    private double systemLoadAverage;
    private String version;
    private long committedVirtualMemorySize;
    private long freePhysicalMemorySize;
    private long freeSwapSpaceSize;
    private double processCpuLoad;
    private long processCpuTime;
    private double systemCpuLoad;
    private long totalPhysicalMemorySize;
    private long totalSwapSpaceSize;

    public OperatingSystemMetrics() {}

    public OperatingSystemMetrics(Map<String, Object> attributes) {
        this.arch = (String) attributes.getOrDefault("Arch", StringUtils.EMPTY);
        this.availableProcessors =
                (int) attributes.getOrDefault("AvailableProcessors", Integer.MIN_VALUE);
        this.name = (String) attributes.getOrDefault("Name", StringUtils.EMPTY);
        this.systemLoadAverage =
                (double) attributes.getOrDefault("SystemLoadAverage", Double.MIN_VALUE);
        this.version = (String) attributes.getOrDefault("Version", StringUtils.EMPTY);
        this.committedVirtualMemorySize =
                (long) attributes.getOrDefault("CommittedVirtualMemorySize", Long.MIN_VALUE);
        this.freePhysicalMemorySize =
                (long) attributes.getOrDefault("FreePhysicalMemorySize", Long.MIN_VALUE);
        this.freeSwapSpaceSize =
                (long) attributes.getOrDefault("FreeSwapSpaceSize", Long.MIN_VALUE);
        this.processCpuLoad = (double) attributes.getOrDefault("ProcessCpuLoad", Double.MIN_VALUE);
        this.processCpuTime = (long) attributes.getOrDefault("ProcessCpuTime", Long.MIN_VALUE);
        this.systemCpuLoad = (double) attributes.getOrDefault("SystemCpuLoad", Double.MIN_VALUE);
        this.totalPhysicalMemorySize =
                (long) attributes.getOrDefault("TotalPhysicalMemorySize", Long.MIN_VALUE);
        this.totalSwapSpaceSize =
                (long) attributes.getOrDefault("TotalSwapSpaceSize", Long.MIN_VALUE);
    }

    public String getArch() {
        return arch;
    }

    public int getAvailableProcessors() {
        return availableProcessors;
    }

    public String getName() {
        return name;
    }

    public double getSystemLoadAverage() {
        return systemLoadAverage;
    }

    public String getVersion() {
        return version;
    }

    public long getCommittedVirtualMemorySize() {
        return committedVirtualMemorySize;
    }

    public long getFreePhysicalMemorySize() {
        return freePhysicalMemorySize;
    }

    public long getFreeSwapSpaceSize() {
        return freeSwapSpaceSize;
    }

    public double getProcessCpuLoad() {
        return processCpuLoad;
    }

    public long getProcessCpuTime() {
        return processCpuTime;
    }

    public double getSystemCpuLoad() {
        return systemCpuLoad;
    }

    public long getTotalPhysicalMemorySize() {
        return totalPhysicalMemorySize;
    }

    public long getTotalSwapSpaceSize() {
        return totalSwapSpaceSize;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("arch", arch)
                .append("availableProcessors", availableProcessors)
                .append("name", name)
                .append("systemLoadAverage", systemLoadAverage)
                .append("version", version)
                .append("committedVirtualMemorySize", committedVirtualMemorySize)
                .append("freePhysicalMemorySize", freePhysicalMemorySize)
                .append("freeSwapSpaceSize", freeSwapSpaceSize)
                .append("processCpuTime", processCpuTime)
                .append("totalPhysicalMemorySize", totalPhysicalMemorySize)
                .append("totalSwapSpaceSize", totalSwapSpaceSize)
                .append("processCpuLoad", processCpuLoad)
                .append("systemCpuLoad", systemCpuLoad)
                .build();
    }
}
