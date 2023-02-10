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

import java.util.Collections;
import java.util.Map;

public class ThreadDetails {
    private final Map<String, Object> attributes;

    public ThreadDetails(Map<String, Object> attributes) {
        this.attributes = Collections.unmodifiableMap(attributes);
    }

    public long[] getAllThreadIds() {
        return (long[]) attributes.get("AllThreadIds");
    }

    public long getCurrentThreadCpuTime() {
        return (long) attributes.get("CurrentThreadCpuTime");
    }

    public long getCurrentThreadUserTime() {
        return (long) attributes.get("CurrentThreadUserTime");
    }

    public int getDaemonThreadCount() {
        return (int) attributes.get("DaemonThreadCount");
    }

    public int getPeakThreadCount() {
        return (int) attributes.get("PeakThreadCount");
    }

    public int getThreadCount() {
        return (int) attributes.get("ThreadCount");
    }

    public long getTotalStartedThreadCount() {
        return (long) attributes.get("TotalStartedThreadCount");
    }

    public boolean isCurrentThreadCpuTimeSupported() {
        return (boolean) attributes.get("CurrentThreadCpuTimeSupported");
    }

    public boolean isObjectMonitorUsageSupported() {
        return (boolean) attributes.get("ObjectMonitorUsageSupported");
    }

    public boolean isSynchronizerUsageSupported() {
        return (boolean) attributes.get("SynchronizerUsageSupported");
    }

    public boolean isThreadContentionMonitoringEnabled() {
        return (boolean) attributes.get("ThreadContentionMonitoringEnabled");
    }

    public boolean isThreadContentionMonitoringSupported() {
        return (boolean) attributes.get("ThreadContentionMonitoringSupported");
    }

    public boolean isThreadCpuTimeEnabled() {
        return (boolean) attributes.get("ThreadCpuTimeEnabled");
    }

    public boolean isThreadCpuTimeSupported() {
        return (boolean) attributes.get("ThreadCpuTimeSupported");
    }

    @Override
    public String toString() {
        return "ThreadDetails{"
                + "AllThreadIds="
                + getAllThreadIds()
                + ", CurrentThreadCpuTime="
                + getCurrentThreadCpuTime()
                + ", CurrentThreadUserTime="
                + getCurrentThreadUserTime()
                + ", DaemonThreadCount="
                + getDaemonThreadCount()
                + ", PeakThreadCount="
                + getPeakThreadCount()
                + ", ThreadCount="
                + getThreadCount()
                + ", TotalStartedThreadCount="
                + getTotalStartedThreadCount()
                + ", CurrentThreadCpuTimeSupported="
                + isCurrentThreadCpuTimeSupported()
                + ", ObjectMonitorUsageSupported="
                + isObjectMonitorUsageSupported()
                + ", SynchronizerUsageSupported="
                + isSynchronizerUsageSupported()
                + ", ThreadContentionMonitoringEnabled="
                + isThreadContentionMonitoringEnabled()
                + ", ThreadContentionMonitoringSupported="
                + isThreadContentionMonitoringSupported()
                + ", ThreadCpuTimeEnabled="
                + isThreadCpuTimeEnabled()
                + ", ThreadCpuTimeSupported="
                + isThreadCpuTimeSupported()
                + '}';
    }
}
