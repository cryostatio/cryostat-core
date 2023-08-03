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

import java.util.Map;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;

public class RuntimeMetrics {
    private final String bootClassPath;
    private final String classPath;
    private final String[] inputArguments;
    private final String libraryPath;
    private final String managementSpecVersion;
    private final String name;
    private final String specName;
    private final String specVendor;
    private final String specVersion;
    private final Map<String, String> systemProperties;
    private final long startTime;
    private final long uptime;
    private final String vmName;
    private final String vmVendor;
    private final String vmVersion;
    private final boolean bootClassPathSupported;

    public RuntimeMetrics(Map<String, Object> attributes) {
        this.bootClassPath = (String) attributes.getOrDefault("BootClassPath", StringUtils.EMPTY);
        this.classPath = (String) attributes.getOrDefault("ClassPath", StringUtils.EMPTY);
        this.inputArguments = (String[]) attributes.getOrDefault("InputArguments", new String[0]);
        this.libraryPath = (String) attributes.getOrDefault("LibraryPath", StringUtils.EMPTY);
        this.managementSpecVersion =
                (String) attributes.getOrDefault("ManagementSpecVersion", StringUtils.EMPTY);
        this.name = (String) attributes.getOrDefault("Name", StringUtils.EMPTY);
        this.specName = (String) attributes.getOrDefault("SpecName", StringUtils.EMPTY);
        this.specVendor = (String) attributes.getOrDefault("SpecVendor", StringUtils.EMPTY);
        this.specVersion = (String) attributes.getOrDefault("SpecVersion", StringUtils.EMPTY);
        this.systemProperties =
                (Map<String, String>) attributes.getOrDefault("SystemProperties", Map.of());
        this.startTime = (long) attributes.getOrDefault("StartTime", Long.MIN_VALUE);
        this.uptime = (long) attributes.getOrDefault("Uptime", Long.MIN_VALUE);
        this.vmName = (String) attributes.getOrDefault("VmName", StringUtils.EMPTY);
        this.vmVendor = (String) attributes.getOrDefault("VmVendor", StringUtils.EMPTY);
        this.vmVersion = (String) attributes.getOrDefault("VmVersion", StringUtils.EMPTY);
        this.bootClassPathSupported =
                (boolean) attributes.getOrDefault("BootClassPathSupported", false);
    }

    public String getBootClassPath() {
        return bootClassPath;
    }

    public String getClassPath() {
        return classPath;
    }

    @SuppressFBWarnings("EI_EXPOSE_REP")
    public String[] getInputArguments() {
        return inputArguments;
    }

    public String getLibraryPath() {
        return libraryPath;
    }

    public String getManagementSpecVersion() {
        return managementSpecVersion;
    }

    public String getName() {
        return name;
    }

    public String getSpecName() {
        return specName;
    }

    public String getSpecVendor() {
        return specVendor;
    }

    public String getSpecVersion() {
        return specVersion;
    }

    public long getStartTime() {
        return startTime;
    }

    @SuppressFBWarnings("EI_EXPOSE_REP")
    public Map<String, String> getSystemProperties() {
        return systemProperties;
    }

    public long getUptime() {
        return uptime;
    }

    public String getVmName() {
        return vmName;
    }

    public String getVmVendor() {
        return vmVendor;
    }

    public String getVmVersion() {
        return vmVersion;
    }

    public boolean isBootClassPathSupported() {
        return bootClassPathSupported;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("bootClassPath", bootClassPath)
                .append("classPath", classPath)
                .append("inputArguments", inputArguments)
                .append("libraryPath", libraryPath)
                .append("managementSpecVersion", managementSpecVersion)
                .append("name", name)
                .append("specName", specName)
                .append("specVendor", specVendor)
                .append("specVersion", specVersion)
                .append("systemProperties", systemProperties)
                .append("startTime", startTime)
                .append("uptime", uptime)
                .append("vmName", vmName)
                .append("vmVendor", vmVendor)
                .append("vmVersion", vmVersion)
                .append("bootClassPathSupported", bootClassPathSupported)
                .build();
    }
}
