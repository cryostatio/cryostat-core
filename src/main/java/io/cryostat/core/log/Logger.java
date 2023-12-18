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

public class Logger {

    private org.slf4j.Logger logger;
    private LogConfig logConfig;

    public static final Logger INSTANCE = new Logger();

    private Logger() {
        this.logger = LoggerFactory.getLogger(Logger.class);
        this.logConfig = new LogConfig();
    }

    public void setLogConfig(LogConfig logConfig) {
        this.logConfig = new LogConfig(logConfig);
    }

    public void error(String message, Object... args) {
        log(LogLevel.ERROR, message, args);
    }

    public void error(Exception exception) {
        log(LogLevel.ERROR, "Exception thrown", exception);
    }

    public void warn(String message, Object... args) {
        log(LogLevel.WARN, message, args);
    }

    public void warn(Exception exception) {
        log(LogLevel.WARN, "Exception thrown", exception);
    }

    public void info(String message, Object... args) {
        log(LogLevel.INFO, message, args);
    }

    public void info(Exception exception) {
        log(LogLevel.INFO, "Exception thrown", exception);
    }

    public void debug(String message, Object... args) {
        log(LogLevel.DEBUG, message, args);
    }

    public void debug(Exception exception) {
        log(LogLevel.DEBUG, "Exception thrown", exception);
    }

    public void trace(String message, Object... args) {
        log(LogLevel.TRACE, message, args);
    }

    public void trace(Exception exception) {
        log(LogLevel.TRACE, "Exception thrown", exception);
    }

    private void log(LogLevel level, String message, Object... args) {
        String formattedMessage = formatMessage(level, message, args);
        switch (level) {
            case ERROR:
                logger.error(formattedMessage);
                break;
            case WARN:
                logger.warn(formattedMessage);
                break;
            case INFO:
                logger.info(formattedMessage);
                break;
            case DEBUG:
                logger.debug(formattedMessage);
                break;
            case TRACE:
                logger.trace(formattedMessage);
                break;
        }
    }

    private String formatMessage(LogLevel level, String message, Object... args) {
        StringBuilder formattedMessage = new StringBuilder();
        formattedMessage.append("{");
        formattedMessage
                .append("\"timestamp\":\"")
                .append(System.currentTimeMillis())
                .append("\",");
        formattedMessage.append("\"level\":\"").append(level.name()).append("\",");
        formattedMessage.append("\"message\":\"").append(message).append("\"");
        if (args.length > 0) {
            formattedMessage.append(" [");
            for (int i = 0; i < args.length; i++) {
                formattedMessage.append(args[i]);
                if (i < args.length - 1) {
                    formattedMessage.append(", ");
                }
            }
            formattedMessage.append("]");
        }
        return formattedMessage.toString();
    }
}
