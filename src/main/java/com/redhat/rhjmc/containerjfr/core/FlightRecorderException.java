package com.redhat.rhjmc.containerjfr.core;

public class FlightRecorderException extends Exception {
    private static final long serialVersionUID = 1L;

    FlightRecorderException(Throwable cause) {
        super(cause);
    }
}
