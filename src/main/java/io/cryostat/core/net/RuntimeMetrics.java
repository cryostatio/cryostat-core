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

public class RuntimeMetrics {
    private final Map<String, Object> attributes;

    public RuntimeMetrics(Map<String, Object> attributes) {
        this.attributes = Collections.unmodifiableMap(attributes);
    }

    public String getBootClassPath() {
        return (String) attributes.getOrDefault("BootClassPath", StringUtils.EMPTY);
    }

    public String getClassPath() {
        return (String) attributes.getOrDefault("ClassPath", StringUtils.EMPTY);
    }

    public String[] getInputArguments() {
        return (String[]) attributes.getOrDefault("InputArguments", new String[0]);
    }

    public String getLibraryPath() {
        return (String) attributes.getOrDefault("LibraryPath", StringUtils.EMPTY);
    }

    public String getManagementSpecVersion() {
        return (String) attributes.getOrDefault("ManagementSpecVersion", StringUtils.EMPTY);
    }

    public String getName() {
        return (String) attributes.getOrDefault("Name", StringUtils.EMPTY);
    }

    public String getSpecName() {
        return (String) attributes.getOrDefault("SpecName", StringUtils.EMPTY);
    }

    public String getSpecVendor() {
        return (String) attributes.getOrDefault("SpecVendor", StringUtils.EMPTY);
    }

    public String getSpecVersion() {
        return (String) attributes.getOrDefault("SpecVersion", StringUtils.EMPTY);
    }

    public long getStartTime() {
        return (long) attributes.getOrDefault("StartTime", Long.MIN_VALUE);
    }

    public Map<String, String> getSystemProperties() {
        return (Map<String, String>)
                attributes.getOrDefault("SystemProperties", Collections.emptyMap());
    }

    public long getUptime() {
        return (long) attributes.getOrDefault("Uptime", Long.MIN_VALUE);
    }

    public String getVmName() {
        return (String) attributes.getOrDefault("VmName", StringUtils.EMPTY);
    }

    public String getVmVendor() {
        return (String) attributes.getOrDefault("VmVendor", StringUtils.EMPTY);
    }

    public String getVmVersion() {
        return (String) attributes.getOrDefault("VmVersion", StringUtils.EMPTY);
    }

    public boolean isBootClassPathSupported() {
        return (boolean) attributes.get("BootClassPathSupported");
    }
}
