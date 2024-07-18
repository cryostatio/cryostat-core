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
package io.cryostat.libcryostat.net;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

public class Credentials {

    private final String username;
    private final String password;

    public Credentials(String username, String password) {
        this.username = username;
        this.password = password;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    @Override
    public final String toString() {
        // Don't override or modify this to include actual contents. This should be kept in-memory
        // for as short a time as possible and never logged
        return super.toString();
    }

    @Override
    public boolean equals(Object other) {
        if (other == null) {
            return false;
        }
        if (other == this) {
            return true;
        }
        if (!(other instanceof Credentials)) {
            return false;
        }
        Credentials c = (Credentials) other;
        return new EqualsBuilder()
                .append(username, c.username)
                .append(password, c.password)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(username).append(password).hashCode();
    }
}
