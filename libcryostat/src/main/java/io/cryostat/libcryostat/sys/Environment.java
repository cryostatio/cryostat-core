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
package io.cryostat.libcryostat.sys;

import java.util.Map;

public class Environment {

    public boolean hasEnv(String key) {
        return getEnv(key) != null && !getEnv(key).isBlank();
    }

    public String getEnv(String key) {
        return System.getenv(key);
    }

    public String getEnv(String key, String def) {
        if (!hasEnv(key)) {
            return def;
        }
        return getEnv(key);
    }

    public Map<String, String> getEnv() {
        return System.getenv();
    }

    public boolean hasProperty(String key) {
        return getProperty(key) != null && !getProperty(key).isBlank();
    }

    public String getProperty(String key) {
        return System.getProperty(key);
    }

    public String getProperty(String key, String def) {
        return System.getProperty(key, def);
    }
}
