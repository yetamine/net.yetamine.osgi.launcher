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

import java.util.Objects;
import java.util.function.Consumer;

/**
 * Defines a logging interface.
 *
 * <p>
 * The {@link #log(Level, String, Throwable)} method is the only abstract method
 * of this interface, all other methods delegate to it as they are serve as easy
 * shortcuts to invoke this implementation root.
 */
public interface Logger {

    /**
     * Defines the logging levels and thresholds.
     *
     * <p>
     * The levels are ordered, so that the greater level the more specific or
     * detailed message should it relate to. The <i>FORCE</i> level basically
     * serves for switching logging off, although it may be used to forced
     * output using {@link Logger#log(Level, String, Throwable)} directly.
     */
    public enum Level {
        FORCE, ERROR, WARN, INFO, DEBUG;
    }

    /**
     * Returns the logging level that serves as threshold for filtering messages
     * above this level.
     *
     * @return the logging level
     */
    Level level();

    /**
     * Performs the actual logging if the logger threshold allows it. This
     * method is a lazy variant of {@link #log(Level, String, Throwable)},
     * therefore the action executes only when the requested logging level
     * allows that with the respect to {@link #level()}.
     *
     * @param level
     *            the requested logging level. It must not be {@code null}.
     * @param action
     *            the action to execute. It must not be {@code null}.
     */
    default void log(Level level, Consumer<? super Logger> action) {
        Objects.requireNonNull(action);
        if (level.compareTo(level()) <= 0) {
            action.accept(this);
        }
    }

    /**
     * Performs the actual logging if the logger threshold allows it.
     *
     * <p>
     * This method is the workhorse that performs all the operations. It assumes
     * that at least one of the {@code message} or {@code t} arguments is not
     * {@code null}, otherwise it may fail or log an error that indicates the
     * logging problem instead of the original message.
     *
     * @param level
     *            the message logging level. It must not be {@code null}.
     * @param message
     *            the message to log
     * @param t
     *            the related exception
     */
    void log(Level level, String message, Throwable t);

    /**
     * Invokes the action on the error level.
     *
     * @param action
     *            the action to invoke. It must not be {@code null}.
     */
    default void error(Consumer<? super Logger> action) {
        log(Level.ERROR, action);
    }

    /**
     * Logs an error.
     *
     * @param message
     *            the message. It should not be {@code null}.
     * @param t
     *            the related exception. It should not be {@code null}.
     */
    default void error(String message, Throwable t) {
        log(Level.ERROR, message, t);
    }

    /**
     * Logs an error.
     *
     * @param message
     *            the message. It should not be {@code null}.
     */
    default void error(String message) {
        log(Level.ERROR, message, null);
    }

    /**
     * Logs an error.
     *
     * @param t
     *            the error. It should not be {@code null}.
     */
    default void error(Throwable t) {
        log(Level.ERROR, null, t);
    }

    /**
     * Invokes the action on the warning level.
     *
     * @param action
     *            the action to invoke. It must not be {@code null}.
     */
    default void warn(Consumer<? super Logger> action) {
        log(Level.WARN, action);
    }

    /**
     * Logs a warning.
     *
     * @param message
     *            the message. It should not be {@code null}.
     * @param t
     *            the related exception. It should not be {@code null}.
     */
    default void warn(String message, Throwable t) {
        log(Level.WARN, message, t);
    }

    /**
     * Logs a warning.
     *
     * @param message
     *            the message. It should not be {@code null}.
     */
    default void warn(String message) {
        log(Level.WARN, message, null);
    }

    /**
     * Invokes the action on the informative level.
     *
     * @param action
     *            the action to invoke. It must not be {@code null}.
     */
    default void info(Consumer<? super Logger> action) {
        log(Level.INFO, action);
    }

    /**
     * Logs an informative message.
     *
     * @param message
     *            the message. It should not be {@code null}.
     * @param t
     *            the related exception. It should not be {@code null}.
     */
    default void info(String message, Throwable t) {
        log(Level.INFO, message, t);
    }

    /**
     * Logs an informative message.
     *
     * @param message
     *            the message. It should not be {@code null}.
     */
    default void info(String message) {
        log(Level.INFO, message, null);
    }

    /**
     * Invokes the action on the debug level.
     *
     * @param action
     *            the action to invoke. It must not be {@code null}.
     */
    default void debug(Consumer<? super Logger> action) {
        log(Level.DEBUG, action);
    }

    /**
     * Logs a debugging message.
     *
     * @param message
     *            the message. It should not be {@code null}.
     * @param t
     *            the related exception. It should not be {@code null}.
     */
    default void debug(String message, Throwable t) {
        log(Level.DEBUG, message, t);
    }

    /**
     * Logs a debugging message.
     *
     * @param message
     *            the message. It should not be {@code null}.
     */
    default void debug(String message) {
        log(Level.DEBUG, message, null);
    }

    /**
     * @return a logger that logs nothing
     */
    static Logger off() {
        return LoggerOff.INSTANCE;
    }

    /**
     * Returns the given logger, or {@link #off()}.
     *
     * @param logger
     *            the logger to return
     *
     * @return the given logger, or {@link #off()} if {@code null}
     */
    static Logger fallback(Logger logger) {
        return (logger != null) ? logger : off();
    }
}
