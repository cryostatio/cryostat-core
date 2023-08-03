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
package io.cryostat.core.log;

import org.slf4j.LoggerFactory;

public enum Logger {
    INSTANCE;

    private org.slf4j.Logger logger = LoggerFactory.getLogger(Logger.class);

    public void error(String message) {
        logger.error(message);
    }

    public void error(String message, Object a) {
        logger.error(message, a);
    }

    public void error(String message, Object a, Object b) {
        logger.error(message, a, b);
    }

    public void error(String message, Object... args) {
        logger.error(message, args);
    }

    public void error(Exception exception) {
        logger.error("Exception thrown", exception);
    }

    public void warn(String message) {
        logger.warn(message);
    }

    public void warn(String message, Object a) {
        logger.warn(message, a);
    }

    public void warn(String message, Object a, Object b) {
        logger.warn(message, a, b);
    }

    public void warn(String message, Object... args) {
        logger.warn(message, args);
    }

    public void warn(Exception exception) {
        logger.warn("Exception thrown", exception);
    }

    public void info(String message) {
        logger.info(message);
    }

    public void info(String message, Object a) {
        logger.info(message, a);
    }

    public void info(String message, Object a, Object b) {
        logger.info(message, a, b);
    }

    public void info(String message, Object... args) {
        logger.info(message, args);
    }

    public void info(Exception exception) {
        logger.info("Exception thrown", exception);
    }

    public void debug(String message) {
        logger.debug(message);
    }

    public void debug(String message, Object a) {
        logger.debug(message, a);
    }

    public void debug(String message, Object a, Object b) {
        logger.debug(message, a, b);
    }

    public void debug(String message, Object... args) {
        logger.debug(message, args);
    }

    public void debug(Exception exception) {
        logger.debug("Exception thrown", exception);
    }

    public void trace(String message) {
        logger.trace(message);
    }

    public void trace(String message, Object a) {
        logger.trace(message, a);
    }

    public void trace(String message, Object a, Object b) {
        logger.trace(message, a, b);
    }

    public void trace(String message, Object... args) {
        logger.trace(message, args);
    }

    public void trace(Exception exception) {
        logger.trace("Exception thrown", exception);
    }
}
