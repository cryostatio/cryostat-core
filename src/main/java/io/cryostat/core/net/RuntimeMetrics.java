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
