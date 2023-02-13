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

import java.lang.management.MemoryUsage;
import java.util.Collections;
import java.util.Map;

public class MemoryMetrics {
    private final Map<String, Object> attributes;

    public MemoryMetrics(Map<String, Object> attributes) {
        this.attributes = Collections.unmodifiableMap(attributes);
    }

    public CustomMemoryUsage getHeapMemoryUsage() {
        return (CustomMemoryUsage)
                attributes.getOrDefault(
                        "HeapMemoryUsage",
                        new CustomMemoryUsage(
                                Long.MIN_VALUE, Long.MIN_VALUE, Long.MIN_VALUE, Long.MIN_VALUE));
    }

    public CustomMemoryUsage getNonHeapMemoryUsage() {
        return (CustomMemoryUsage)
                attributes.getOrDefault(
                        "NonHeapMemoryUsage",
                        new CustomMemoryUsage(
                                Long.MIN_VALUE, Long.MIN_VALUE, Long.MIN_VALUE, Long.MIN_VALUE));
    }

    public long getObjectPendingFinalizationCount() {
        return (long) attributes.getOrDefault("ObjectPendingFinalizationCount", Long.MIN_VALUE);
    }

    public long getFreeHeapMemory() {
        return (long) attributes.getOrDefault("FreeHeapMemory", Long.MIN_VALUE);
    }

    public long getFreeNonHeapMemory() {
        return (long) attributes.getOrDefault("FreeNonHeapMemory", Long.MIN_VALUE);
    }

    public double getHeapMemoryUsagePercent() {
        return (double) attributes.getOrDefault("HeapMemoryUsagePercent", Double.MIN_VALUE);
    }

    public boolean isVerbose() {
        return (boolean) attributes.get("Verbose");
    }

    // Gson cannot read private fields of java.lang.management.MemoryUsage
    // Using our own class, we can also define custom default values for each field
    static class CustomMemoryUsage {
        private final long init;
        private final long used;
        private final long committed;
        private final long max;

        public CustomMemoryUsage(long init, long used, long committed, long max) {
            this.init = init;
            this.used = used;
            this.committed = committed;
            this.max = max;
        }

        public long getInit() {
            return init;
        }

        public long getUsed() {
            return used;
        }

        public long getCommitted() {
            return committed;
        }

        public long getMax() {
            return max;
        }

        public static CustomMemoryUsage fromMemoryUsage(MemoryUsage mu) {
            return new CustomMemoryUsage(
                    mu.getInit(), mu.getUsed(), mu.getCommitted(), mu.getMax());
        }
    }
}
