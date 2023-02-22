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
package io.cryostat.core.net;

import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;

public class OperatingSystemMetrics {
    private final String arch;
    private final int availableProcessors;
    private final String name;
    private final double systemLoadAverage;
    private final String version;
    private final long committedVirtualMemorySize;
    private final long freePhysicalMemorySize;
    private final long freeSwapSpaceSize;
    private final double processCpuLoad;
    private final long processCpuTime;
    private final double systemCpuLoad;
    private final long totalPhysicalMemorySize;
    private final long totalSwapSpaceSize;

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
