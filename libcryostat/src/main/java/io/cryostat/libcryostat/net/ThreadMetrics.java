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

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.apache.commons.lang3.builder.ToStringBuilder;

public class ThreadMetrics {
    private long[] allThreadIds;
    private long currentThreadCpuTime;
    private long currentThreadUserTime;
    private int daemonThreadCount;
    private int peakThreadCount;
    private int threadCount;
    private long totalStartedThreadCount;
    private boolean currentThreadCpuTimeSupported;
    private boolean objectMonitorUsageSupported;
    private boolean synchronizerUsageSupported;
    private boolean threadContentionMonitoringEnabled;
    private boolean threadContentionMonitoringSupported;
    private boolean threadCpuTimeEnabled;
    private boolean threadCpuTimeSupported;

    public ThreadMetrics() {}

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
        return new ToStringBuilder(this)
                .append("allThreadIds", allThreadIds)
                .append("currentThreadCpuTime", currentThreadCpuTime)
                .append("currentThreadUserTime", currentThreadUserTime)
                .append("daemonThreadCount", daemonThreadCount)
                .append("peakThreadCount", peakThreadCount)
                .append("threadCount", threadCount)
                .append("totalStartedThreadCount", totalStartedThreadCount)
                .append("currentThreadCpuTimeSupported", currentThreadCpuTimeSupported)
                .append("objectMonitorUsageSupported", objectMonitorUsageSupported)
                .append("synchronizerUsageSupported", synchronizerUsageSupported)
                .append("threadContentionMonitoringEnabled", threadContentionMonitoringEnabled)
                .append("threadContentionMonitoringSupported", threadContentionMonitoringSupported)
                .append("threadCpuTimeEnabled", threadCpuTimeEnabled)
                .append("threadCpuTimeSupported", threadCpuTimeSupported)
                .build();
    }
}
