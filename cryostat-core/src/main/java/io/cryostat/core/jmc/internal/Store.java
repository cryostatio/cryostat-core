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
package io.cryostat.core.jmc.internal;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class Store {

    private final Map<String, Object> store = new HashMap<String, Object>();

    private static final String SEP = "_";

    public String insert(String key, boolean keyFamily, String value) {
        return insertInternal(key, keyFamily, value);
    }

    public String insert(String key, boolean keyFamily, String[] value) {
        return insertInternal(key, keyFamily, value);
    }

    public String insert(String key, boolean keyFamily, byte[] value) {
        return insertInternal(key, keyFamily, value);
    }

    private String generateKey(String family) {
        return (this.store.size() + 1) + SEP + "store" + (family == null ? "" : SEP + family);
    }

    private synchronized String insertInternal(String key, boolean keyFamily, Object value) {
        key = keyFamily || key == null ? generateKey(key) : key;
        this.store.put(key, value);
        return key;
    }

    public synchronized Object get(String key) {
        return this.store.get(key);
    }

    public synchronized void clearFamily(String family, Set<String> keepKeys) {
        Iterator<String> it = this.store.keySet().iterator();
        while (it.hasNext()) {
            String key = it.next();
            String[] keyParts = key.split(SEP);
            if (keyParts.length == 3 && keyParts[2].equals(family) && !keepKeys.contains(key)) {
                it.remove();
            }
        }
    }

    public synchronized boolean hasKey(String key) {
        return this.store.containsKey(key);
    }

    public synchronized Object remove(String key) {
        return this.store.remove(key);
    }
}
