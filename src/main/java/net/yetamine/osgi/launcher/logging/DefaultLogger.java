/*
 * Copyright 2019 Yetamine
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.yetamine.osgi.launcher.logging;

import java.io.PrintStream;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

/**
 * Implements {@link Logger} that prints to a {@link PrintStream} with little
 * additional formatting.
 *
 * <p>
 * This implementation can be useful as a mild improvement of printing messages
 * directly to the standard or error output for various messages which might be
 * used when no better logging framework is available.
 */
public final class DefaultLogger implements Logger {

    /**
     * Name of the system property with the default logger level.
     */
    public static final String SYSTEM_LEVEL_PROPERTY = "net.yetamine.osgi.launcher.logging.level";

    /**
     * Name of the system property with the desired output.
     */
    public static final String SYSTEM_OUTPUT_PROPERTY = "net.yetamine.osgi.launcher.logging.file";

    private volatile Level level = Level.ERROR;
    private volatile PrintStream output;
    private volatile boolean stackTrace;

    /**
     * Creates a new instance.
     *
     * @param givenOutput
     *            the output to write to. It must not be {@code null}.
     */
    public DefaultLogger(PrintStream givenOutput) {
        output = Objects.requireNonNull(givenOutput);
    }

    /**
     * @return the default level provided by the system property
     */
    public static Level systemLevel() {
        final String property = System.getProperty(SYSTEM_LEVEL_PROPERTY);
        if ((property == null) || property.isEmpty()) {
            return Level.ERROR;
        }

        try {
            return Level.valueOf(property.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return Level.DEBUG;
        }
    }

    /**
     * @return the default output requested by the system property, or an empty
     *         string if not specified
     */
    public static String systemOutput() {
        return System.getProperty(SYSTEM_OUTPUT_PROPERTY, "");
    }

    /**
     * Sets the logger level.
     *
     * <p>
     * Setting the logger level automatically adjusts {@link #stackTrace()}, so
     * that the stack traces are printed automatically when the level is set to
     * {@link net.yetamine.osgi.launcher.logging.Logger.Level#DEBUG}. It can be
     * be overridden with {@link #stackTrace(boolean)}.
     *
     * @param value
     *            the value to set. It must not be {@code null}.
     *
     * @return this instance
     */
    public DefaultLogger level(Level value) {
        level = Objects.requireNonNull(value);
        if (Level.DEBUG.compareTo(value) <= 0) {
            stackTrace(true);
        }

        return this;
    }

    /**
     * @see net.yetamine.osgi.launcher.logging.Logger#level()
     */
    @Override
    public Level level() {
        return level;
    }

    /**
     * Sets the stack trace printing.
     *
     * @param value
     *            the value to set. It must not be {@code null}.
     *
     * @return this instance
     */
    public DefaultLogger stackTrace(boolean value) {
        stackTrace = value;
        return this;
    }

    /**
     * @return {@code true} if stack traces should be printed
     */
    public boolean stackTrace() {
        return stackTrace;
    }

    /**
     * @see net.yetamine.osgi.launcher.logging.Logger#log(net.yetamine.osgi.launcher.logging.Logger.Level,
     *      java.lang.String, java.lang.Throwable)
     */
    @Override
    public void log(Level requestedLevel, String message, Throwable t) {
        if (missingMessage(message, t)) {
            missingMessageError();
            return;
        }

        if (isAboveLevel(requestedLevel)) {
            return;
        }

        print(requestedLevel, composeMessage(message, t), filter(t));
    }

    /**
     * Sets the output to write to.
     *
     * @param stream
     *            the output to write to. It must not be {@code null}.
     *
     * @return this instance
     */
    public DefaultLogger output(PrintStream stream) {
        output = Objects.requireNonNull(stream);
        return this;
    }

    /**
     * @return the current output
     */
    public PrintStream output() {
        return output;
    }

    private void print(Object header, String message, Throwable t) {
        // Ensure that the same printer is used for both parts
        final PrintStream printer = output();
        // Do not allow the parts to be split
        synchronized (printer) {
            printer.format("[%-5s] %s%n", header, message);
            if (t != null) {
                t.printStackTrace(printer);
            }
        }
    }

    private Throwable filter(Throwable t) {
        return stackTrace() ? t : null;
    }

    private static String composeMessage(String message, Throwable t) {
        if (message != null) {
            return message;
        }

        return Optional.ofNullable(t)           ///
                .map(Throwable::getMessage)     ///
                .orElse("Exception occurred. See the stack trace for details.");
    }

    private void missingMessageError() {
        try {
            throw new IllegalArgumentException();
        } catch (IllegalArgumentException e) {
            print("BUG!?", "Invalid arguments for logging: no message source provided.", e);
        }
    }

    private boolean missingMessage(String message, Throwable t) {
        return ((message == null) && (t == null));
    }

    private boolean isAboveLevel(Level requestedLevel) {
        return (level().compareTo(requestedLevel) < 0);
    }
}
