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
package com.redhat.rhjmc.containerjfr.core.jmc;

import java.util.Collections;
import java.util.Set;

import org.apache.commons.lang3.NotImplementedException;

import org.openjdk.jmc.ui.common.security.ActionNotGrantedException;
import org.openjdk.jmc.ui.common.security.FailedToSaveException;
import org.openjdk.jmc.ui.common.security.ISecurityManager;
import org.openjdk.jmc.ui.common.security.SecurityException;

import com.redhat.rhjmc.containerjfr.core.jmc.internal.Store;

public class SecurityManager implements ISecurityManager {

    private final Store store;

    public SecurityManager() {
        this.store = new Store();
    }

    @Override
    public boolean hasKey(String key) {
        return this.store.hasKey(key);
    }

    @Override
    public Object withdraw(String key) throws SecurityException {
        return this.store.remove(key);
    }

    @Override
    public void clearFamily(String family, Set<String> keys) throws FailedToSaveException {
        this.store.clearFamily(family, keys);
    }

    @Override
    public Object get(String key) throws SecurityException {
        return hasKey(key) ? this.store.get(key) : null;
    }

    @Override
    public String store(byte... value) throws SecurityException {
        return this.store.insert(null, true, value);
    }

    @Override
    public String store(String... value) throws SecurityException {
        return this.store.insert(null, true, value);
    }

    @Override
    public String storeInFamily(String family, byte... value) throws SecurityException {
        return this.store.insert(family, true, value);
    }

    @Override
    public String storeInFamily(String family, String... value) throws SecurityException {
        return this.store.insert(family, true, value);
    }

    @Override
    public void storeWithKey(String key, byte... value) throws SecurityException {
        this.store.insert(key, false, value);
    }

    @Override
    public void storeWithKey(String key, String... value) throws SecurityException {
        this.store.insert(key, false, value);
    }

    @Override
    public Set<String> getEncryptionCiphers() {
        return Collections.emptySet();
    }

    @Override
    public String getEncryptionCipher() {
        return null;
    }

    @Override
    public void setEncryptionCipher(String encryptionCipher) throws SecurityException {
        throw new NotImplementedException("Encryption not supported");
    }

    @Override
    public void changeMasterPassword() throws SecurityException {
        throw new NotImplementedException("Master Password change not implemented");
    }

    @Override
    public boolean isLocked() {
        return false;
    }

    @Override
    public void unlock() throws ActionNotGrantedException {
        // no-op
    }
}
