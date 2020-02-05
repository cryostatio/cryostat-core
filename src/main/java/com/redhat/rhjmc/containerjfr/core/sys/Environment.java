package com.redhat.rhjmc.containerjfr.core.sys;

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
