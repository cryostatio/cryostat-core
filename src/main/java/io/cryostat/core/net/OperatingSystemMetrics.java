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

import org.apache.commons.lang3.StringUtils;

public class OperatingSystemMetrics {
    private final Map<String, Object> attributes;

    public OperatingSystemMetrics(Map<String, Object> attributes) {
        this.attributes = Collections.unmodifiableMap(attributes);
    }

    public String getArch() {
        return (String) attributes.getOrDefault("Arch", StringUtils.EMPTY);
    }

    public int getAvailableProcessors() {
        return (int) attributes.getOrDefault("AvailableProcessors", Integer.MIN_VALUE);
    }

    public String getName() {
        return (String) attributes.getOrDefault("Name", StringUtils.EMPTY);
    }

    public double getSystemLoadAverage() {
        return (double) attributes.getOrDefault("SystemLoadAverage", Double.MIN_VALUE);
    }

    public String getVersion() {
        return (String) attributes.getOrDefault("Version", StringUtils.EMPTY);
    }

    public long getCommittedVirtualMemorySize() {
        return (long) attributes.getOrDefault("CommittedVirtualMemorySize", Long.MIN_VALUE);
    }

    public long getFreePhysicalMemorySize() {
        return (long) attributes.getOrDefault("FreePhysicalMemorySize", Long.MIN_VALUE);
    }

    public long getFreeSwapSpaceSize() {
        return (long) attributes.getOrDefault("FreeSwapSpaceSize", Long.MIN_VALUE);
    }

    public double getProcessCpuLoad() {
        return (double) attributes.getOrDefault("ProcessCpuLoad", Double.MIN_VALUE);
    }

    public long getProcessCpuTime() {
        return (long) attributes.getOrDefault("ProcessCpuTime", Long.MIN_VALUE);
    }

    public double getSystemCpuLoad() {
        return (double) attributes.getOrDefault("SystemCpuLoad", Double.MIN_VALUE);
    }

    public long getTotalPhysicalMemorySize() {
        return (long) attributes.getOrDefault("TotalPhysicalMemorySize", Long.MIN_VALUE);
    }

    public double getTotalSwapSpaceSize() {
        return (double) attributes.getOrDefault("TotalSwapSpaceSize", Double.MIN_VALUE);
    }
}
