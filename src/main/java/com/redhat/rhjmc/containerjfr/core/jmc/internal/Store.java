/*-
 * #%L
 * Container JFR Core
 * %%
 * Copyright (C) 2020 Red Hat, Inc.
 * %%
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
 * #L%
 */
package com.redhat.rhjmc.containerjfr.core.jmc.internal;

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
