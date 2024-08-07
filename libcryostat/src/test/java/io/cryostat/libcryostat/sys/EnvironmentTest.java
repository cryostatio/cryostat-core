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

import java.util.UUID;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EnvironmentTest {

    Environment env;

    @BeforeEach
    void setup() {
        env = new Environment();
    }

    @Test
    void getEnvShouldNotContainRandomUUIDEnv() {
        String uuid = UUID.randomUUID().toString();
        MatcherAssert.assertThat(env.hasEnv(uuid), Matchers.is(false));
        MatcherAssert.assertThat(env.getEnv(uuid), Matchers.nullValue());
    }

    @Test
    void getEnvShouldReturnSpecifiedDefaultWhenEnvVarUndefined() {
        MatcherAssert.assertThat(
                env.getEnv(UUID.randomUUID().toString(), "default"), Matchers.equalTo("default"));
    }

    @Test
    void getPropertyShouldNotContainRandomUUIDProperty() {
        String uuid = UUID.randomUUID().toString();
        MatcherAssert.assertThat(env.hasProperty(uuid), Matchers.is(false));
        MatcherAssert.assertThat(env.getProperty(uuid), Matchers.nullValue());
    }

    @Test
    void getPropertyShouldReturnSpecifiedDefaultWhenPropertyUnset() {
        MatcherAssert.assertThat(
                env.getProperty(UUID.randomUUID().toString(), "default"),
                Matchers.equalTo("default"));
    }
}
