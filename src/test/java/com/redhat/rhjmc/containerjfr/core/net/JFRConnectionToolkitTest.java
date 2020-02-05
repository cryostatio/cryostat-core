package com.redhat.rhjmc.containerjfr.core.net;

import static org.junit.jupiter.api.Assertions.assertThrows;

import org.openjdk.jmc.rjmx.internal.WrappedConnectionException;

import com.redhat.rhjmc.containerjfr.core.tui.ClientWriter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class JFRConnectionToolkitTest {

    JFRConnectionToolkit toolkit;
    @Mock ClientWriter cw;

    @BeforeEach
    void setup() {
        toolkit = new JFRConnectionToolkit(cw);
    }

    @Test
    void shouldThrowInTestEnvironment() {
        assertThrows(WrappedConnectionException.class, () -> toolkit.connect("foo", 9091));
    }

    @Test
    void shouldThrowInTestEnvironment2() {
        assertThrows(WrappedConnectionException.class, () -> toolkit.connect("foo"));
    }
}
