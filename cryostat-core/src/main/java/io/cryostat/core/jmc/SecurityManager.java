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
package io.cryostat.core.jmc;

import java.util.Collections;
import java.util.Set;

import org.openjdk.jmc.common.security.ActionNotGrantedException;
import org.openjdk.jmc.common.security.FailedToSaveException;
import org.openjdk.jmc.common.security.ISecurityManager;
import org.openjdk.jmc.common.security.SecurityException;

import io.cryostat.core.jmc.internal.Store;

import org.apache.commons.lang3.NotImplementedException;

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
