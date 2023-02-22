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

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

public class ThreadMetrics {
    private final long[] allThreadIds;
    private final long currentThreadCpuTime;
    private final long currentThreadUserTime;
    private final int daemonThreadCount;
    private final int peakThreadCount;
    private final int threadCount;
    private final long totalStartedThreadCount;
    private final boolean currentThreadCpuTimeSupported;
    private final boolean objectMonitorUsageSupported;
    private final boolean synchronizerUsageSupported;
    private final boolean threadContentionMonitoringEnabled;
    private final boolean threadContentionMonitoringSupported;
    private final boolean threadCpuTimeEnabled;
    private final boolean threadCpuTimeSupported;

    public ThreadMetrics(Map<String, Object> attributes) {
        this.allThreadIds = (long[]) attributes.getOrDefault("AllThreadIds", new long[0]);
        this.currentThreadCpuTime =
                (long) attributes.getOrDefault("CurrentThreadCpuTime", Long.MIN_VALUE);
        this.currentThreadUserTime =
                (long) attributes.getOrDefault("CurrentThreadUserTime", Long.MIN_VALUE);
        this.daemonThreadCount =
                (int) attributes.getOrDefault("DaemonThreadCount", Integer.MIN_VALUE);
        this.peakThreadCount = (int) attributes.getOrDefault("PeakThreadCount", Integer.MIN_VALUE);
        this.threadCount = (int) attributes.getOrDefault("ThreadCount", Integer.MIN_VALUE);
        this.totalStartedThreadCount =
                (long) attributes.getOrDefault("TotalStartedThreadCount", Long.MIN_VALUE);
        this.currentThreadCpuTimeSupported =
                (boolean) attributes.getOrDefault("CurrentThreadCpuTimeSupported", false);
        this.objectMonitorUsageSupported =
                (boolean) attributes.getOrDefault("ObjectMonitorUsageSupported", false);
        this.synchronizerUsageSupported =
                (boolean) attributes.getOrDefault("SynchronizerUsageSupported", false);
        this.threadContentionMonitoringEnabled =
                (boolean) attributes.getOrDefault("ThreadContentionMonitoringEnabled", false);
        this.threadContentionMonitoringSupported =
                (boolean) attributes.getOrDefault("ThreadContentionMonitoringSupported", false);
        this.threadCpuTimeEnabled =
                (boolean) attributes.getOrDefault("ThreadCpuTimeEnabled", false);
        this.threadCpuTimeSupported =
                (boolean) attributes.getOrDefault("ThreadCpuTimeSupported", false);
    }

    @SuppressFBWarnings("EI_EXPOSE_REP")
    public long[] getAllThreadIds() {
        return allThreadIds;
    }

    public long getCurrentThreadCpuTime() {
        return currentThreadCpuTime;
    }

    public long getCurrentThreadUserTime() {
        return currentThreadUserTime;
    }

    public int getDaemonThreadCount() {
        return daemonThreadCount;
    }

    public int getPeakThreadCount() {
        return peakThreadCount;
    }

    public int getThreadCount() {
        return threadCount;
    }

    public long getTotalStartedThreadCount() {
        return totalStartedThreadCount;
    }

    public boolean isCurrentThreadCpuTimeSupported() {
        return currentThreadCpuTimeSupported;
    }

    public boolean isObjectMonitorUsageSupported() {
        return objectMonitorUsageSupported;
    }

    public boolean isSynchronizerUsageSupported() {
        return synchronizerUsageSupported;
    }

    public boolean isThreadContentionMonitoringEnabled() {
        return threadContentionMonitoringEnabled;
    }

    public boolean isThreadContentionMonitoringSupported() {
        return threadContentionMonitoringSupported;
    }

    public boolean isThreadCpuTimeEnabled() {
        return threadCpuTimeEnabled;
    }

    public boolean isThreadCpuTimeSupported() {
        return threadCpuTimeSupported;
    }

    @Override
    public String toString() {
        return "ThreadMetrics{"
                + "allThreadIds="
                + allThreadIds
                + ", currentThreadCpuTime="
                + currentThreadCpuTime
                + ", currentThreadUserTime="
                + currentThreadUserTime
                + ", daemonThreadCount="
                + daemonThreadCount
                + ", peakThreadCount="
                + peakThreadCount
                + ", threadCount="
                + threadCount
                + ", totalStartedThreadCount="
                + totalStartedThreadCount
                + ", currentThreadCpuTimeSupported="
                + currentThreadCpuTimeSupported
                + ", objectMonitorUsageSupported="
                + objectMonitorUsageSupported
                + ", synchronizerUsageSupported="
                + synchronizerUsageSupported
                + ", threadContentionMonitoringEnabled="
                + threadContentionMonitoringEnabled
                + ", threadContentionMonitoringSupported="
                + threadContentionMonitoringSupported
                + ", threadCpuTimeEnabled="
                + threadCpuTimeEnabled
                + ", threadCpuTimeSupported="
                + threadCpuTimeSupported
                + '}';
    }
}
