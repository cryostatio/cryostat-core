package com.redhat.rhjmc.containerjfr.core.log;

import java.io.PrintStream;

import org.apache.commons.lang3.exception.ExceptionUtils;

/** Built-in lightweight and simple logging facility. TODO evaluate log4j */
public enum Logger {
    INSTANCE;

    private Level level = Level.INFO;
    private PrintStream stream = System.err;

    public enum Level {
        OFF,
        ERROR,
        WARN,
        INFO,
        DEBUG,
        TRACE,
        ALL,
    }

    /**
     * Set the log level to print. Messages with log level at or above the specified level will be
     * printed to the Logger's PrintStream. Log output can be silenced by setting the level to OFF.
     * All output can be written by setting the level to ALL. Default is INFO.
     */
    public void setLevel(Level level) {
        if (level == null) {
            throw new IllegalArgumentException("Level cannot be null");
        }
        this.level = level;
    }

    public Level getLevel() {
        return level;
    }

    /**
     * Set the PrintStream which this Logger writes to. Log output can be silenced by passing a
     * PrintStream which drops messages. Default is System.err.
     */
    public synchronized void setPrintStream(PrintStream stream) {
        if (stream == null) {
            throw new IllegalArgumentException("PrintStream cannot be null");
        }
        this.stream = stream;
    }

    public synchronized void log(Level level, String message) {
        if (level.ordinal() > this.level.ordinal()) {
            return;
        }
        stream.format("[%s] %s%n", level.name(), message);
    }

    public synchronized void log(Level level, Exception exception) {
        log(level, ExceptionUtils.getStackTrace(exception));
    }

    public void error(String message) {
        log(Level.ERROR, message);
    }

    public void error(Exception exception) {
        error(ExceptionUtils.getStackTrace(exception));
    }

    public void warn(String message) {
        log(Level.WARN, message);
    }

    public void warn(Exception exception) {
        warn(ExceptionUtils.getStackTrace(exception));
    }

    public void info(String message) {
        log(Level.INFO, message);
    }

    public void info(Exception exception) {
        info(ExceptionUtils.getStackTrace(exception));
    }

    public void debug(String message) {
        log(Level.DEBUG, message);
    }

    public void debug(Exception exception) {
        debug(ExceptionUtils.getStackTrace(exception));
    }

    public void trace(String message) {
        log(Level.TRACE, message);
    }

    public void trace(Exception exception) {
        trace(ExceptionUtils.getStackTrace(exception));
    }
}
