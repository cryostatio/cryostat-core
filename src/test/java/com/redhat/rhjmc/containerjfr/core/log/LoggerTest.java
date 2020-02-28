package com.redhat.rhjmc.containerjfr.core.log;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;

import java.io.PrintStream;
import java.io.PrintWriter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;

import com.redhat.rhjmc.containerjfr.core.log.Logger.Level;

@ExtendWith(MockitoExtension.class)
public class LoggerTest {

    static final String TAGGED_MESSAGE_FORMAT = "[%s] %s%n";

    @Mock PrintStream stream;
    Logger logger;

    @BeforeEach
    void setup() {
        this.logger = Logger.INSTANCE;
        this.logger.setPrintStream(stream);
    }

    @Test
    void testDisallowsNullLevel() {
        assertThrows(IllegalArgumentException.class, () -> logger.setLevel(null));
    }

    @Test
    void testDisallowsNullStream() {
        assertThrows(IllegalArgumentException.class, () -> logger.setPrintStream(null));
    }

    @Nested
    class WithInfoLevel {

        @BeforeEach
        void setup() {
            logger.setLevel(Level.INFO);
        }

        @Test
        void testPrintsError() {
            String message = "some message";
            logger.error(message);
            verify(stream).format(TAGGED_MESSAGE_FORMAT, Level.ERROR.name(), message);
        }

        @Test
        void testPrintsWarn() {
            String message = "some message";
            logger.warn(message);
            verify(stream).format(TAGGED_MESSAGE_FORMAT, Level.WARN.name(), message);
        }

        @Test
        void testPrintsInfo() {
            String message = "some message";
            logger.info(message);
            verify(stream).format(TAGGED_MESSAGE_FORMAT, Level.INFO.name(), message);
        }

        @Test
        void testDoesNotPrintDebug() {
            String message = "some message";
            logger.debug(message);
            verifyZeroInteractions(stream);
        }

        @Test
        void testDoesNotPrintTrace() {
            String message = "some message";
            logger.trace(message);
            verifyZeroInteractions(stream);
        }
    }

    @Nested
    class WithAllLevel {

        @Mock Exception exception;

        @BeforeEach
        void setLevel() {
            logger.setLevel(Level.ALL);
        }

        @Test
        void testPrintsErrorString() {
            String message = "some message";
            logger.error(message);
            verify(stream).format(TAGGED_MESSAGE_FORMAT, Level.ERROR.name(), message);
        }

        @Test
        void testPrintsErrorException() {
            String message = "this is a test message";
            Mockito.doAnswer(
                            new Answer<Void>() {
                                @Override
                                public Void answer(InvocationOnMock invocation) throws Throwable {
                                    ((PrintWriter) invocation.getArgument(0)).print(message);
                                    return null;
                                }
                            })
                    .when(exception)
                    .printStackTrace(Mockito.any(PrintWriter.class));
            logger.error(exception);
            verify(stream).format(TAGGED_MESSAGE_FORMAT, Level.ERROR.name(), message);
        }

        @Test
        void testPrintsWarnString() {
            String message = "some message";
            logger.warn(message);
            verify(stream).format(TAGGED_MESSAGE_FORMAT, Level.WARN.name(), message);
        }

        @Test
        void testPrintsWarnException() {
            String message = "this is a test message";
            Mockito.doAnswer(
                            new Answer<Void>() {
                                @Override
                                public Void answer(InvocationOnMock invocation) throws Throwable {
                                    ((PrintWriter) invocation.getArgument(0)).print(message);
                                    return null;
                                }
                            })
                    .when(exception)
                    .printStackTrace(Mockito.any(PrintWriter.class));
            logger.warn(exception);
            verify(stream).format(TAGGED_MESSAGE_FORMAT, Level.WARN.name(), message);
        }

        @Test
        void testPrintsInfoString() {
            String message = "some message";
            logger.info(message);
            verify(stream).format(TAGGED_MESSAGE_FORMAT, Level.INFO.name(), message);
        }

        @Test
        void testPrintsInfoException() {
            String message = "this is a test message";
            Mockito.doAnswer(
                            new Answer<Void>() {
                                @Override
                                public Void answer(InvocationOnMock invocation) throws Throwable {
                                    ((PrintWriter) invocation.getArgument(0)).print(message);
                                    return null;
                                }
                            })
                    .when(exception)
                    .printStackTrace(Mockito.any(PrintWriter.class));
            logger.info(exception);
            verify(stream).format(TAGGED_MESSAGE_FORMAT, Level.INFO.name(), message);
        }

        @Test
        void testPrintsDebugString() {
            String message = "some message";
            logger.debug(message);
            verify(stream).format(TAGGED_MESSAGE_FORMAT, Level.DEBUG.name(), message);
        }

        @Test
        void testPrintsDebugException() {
            String message = "this is a test message";
            Mockito.doAnswer(
                            new Answer<Void>() {
                                @Override
                                public Void answer(InvocationOnMock invocation) throws Throwable {
                                    ((PrintWriter) invocation.getArgument(0)).print(message);
                                    return null;
                                }
                            })
                    .when(exception)
                    .printStackTrace(Mockito.any(PrintWriter.class));
            logger.debug(exception);
            verify(stream).format(TAGGED_MESSAGE_FORMAT, Level.DEBUG.name(), message);
        }

        @Test
        void testPrintsTraceString() {
            String message = "some message";
            logger.trace(message);
            verify(stream).format(TAGGED_MESSAGE_FORMAT, Level.TRACE.name(), message);
        }

        @Test
        void testPrintsTraceException() {
            String message = "this is a test message";
            Mockito.doAnswer(
                            new Answer<Void>() {
                                @Override
                                public Void answer(InvocationOnMock invocation) throws Throwable {
                                    ((PrintWriter) invocation.getArgument(0)).print(message);
                                    return null;
                                }
                            })
                    .when(exception)
                    .printStackTrace(Mockito.any(PrintWriter.class));
            logger.trace(exception);
            verify(stream).format(TAGGED_MESSAGE_FORMAT, Level.TRACE.name(), message);
        }
    }

    @Nested
    class WithOffLevel {

        @BeforeEach
        void setup() {
            logger.setLevel(Level.OFF);
        }

        @Test
        void testDoesNotPrintError() {
            String message = "some message";
            logger.error(message);
            verifyZeroInteractions(stream);
        }

        @Test
        void testDoesNotPrintWarn() {
            String message = "some message";
            logger.warn(message);
            verifyZeroInteractions(stream);
        }

        @Test
        void testDoesNotPrintInfo() {
            String message = "some message";
            logger.info(message);
            verifyZeroInteractions(stream);
        }

        @Test
        void testDoesNotPrintDebug() {
            String message = "some message";
            logger.debug(message);
            verifyZeroInteractions(stream);
        }

        @Test
        void testDoesNotPrintTrace() {
            String message = "some message";
            logger.trace(message);
            verifyZeroInteractions(stream);
        }
    }
}
