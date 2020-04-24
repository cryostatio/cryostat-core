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
