package com.redhat.rhjmc.containerjfr.core.sys;

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
