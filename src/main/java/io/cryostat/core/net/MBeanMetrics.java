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

public class MBeanMetrics {
    private final RuntimeMetrics runtime;
    private final MemoryMetrics memory;
    private final ThreadMetrics thread;
    private final OperatingSystemMetrics os;
    private final String jvmId;

    public MBeanMetrics(
            RuntimeMetrics runtimeDetails,
            MemoryMetrics memoryDetails,
            ThreadMetrics threadDetails,
            OperatingSystemMetrics operatingSystemDetails,
            String jvmId) {
        this.runtime = runtimeDetails;
        this.memory = memoryDetails;
        this.thread = threadDetails;
        this.os = operatingSystemDetails;
        this.jvmId = jvmId;
    }

    public RuntimeMetrics getRuntime() {
        return runtime;
    }

    public MemoryMetrics getMemory() {
        return memory;
    }

    public ThreadMetrics getThread() {
        return thread;
    }

    public OperatingSystemMetrics getOs() {
        return os;
    }

    public String getJvmId() {
        return jvmId;
    }
}
