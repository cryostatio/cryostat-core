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
package io.cryostat.core.log;

public class LogConfig {

    private boolean includeTimestamp = true;
    private boolean includeThreadId = true;
    private boolean includeLogLevel = true;

    public LogConfig() {
        // Default constructor
    }

    // Copy constructor
    public LogConfig(LogConfig other) {
        this.includeTimestamp = other.includeTimestamp;
        this.includeThreadId = other.includeThreadId;
        this.includeLogLevel = other.includeLogLevel;
    }

    public boolean isIncludeTimestamp() {
        return includeTimestamp;
    }

    public void setIncludeTimestamp(boolean includeTimestamp) {
        this.includeTimestamp = includeTimestamp;
    }

    public boolean isIncludeThreadId() {
        return includeThreadId;
    }

    public void setIncludeThreadId(boolean includeThreadId) {
        this.includeThreadId = includeThreadId;
    }

    public boolean isIncludeLogLevel() {
        return includeLogLevel;
    }

    public void setIncludeLogLevel(boolean includeLogLevel) {
        this.includeLogLevel = includeLogLevel;
    }
}
