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

package net.yetamine.osgi.launcher;

import java.util.Objects;
import java.util.function.Consumer;

/**
 * Adapts {@link StringBuilder} to provide additional formatting means.
 */
final class StringPrinter {

    /** Native end of line for this platform. */
    public static final String EOL = String.format("%n");

    private final StringBuilder output;

    /**
     * Creates a new instance.
     *
     * @param builder
     *            the builder to use. It must not be {@code null}.
     */
    public StringPrinter(StringBuilder builder) {
        output = Objects.requireNonNull(builder);
    }

    /**
     * Creates a new instance with a blank builder.
     */
    public StringPrinter() {
        this(new StringBuilder());
    }

    /**
     * Prints the builder content.
     *
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return output.toString();
    }

    /**
     * Invokes the given action on the builder.
     *
     * @param action
     *            the action to invoke. It must not be {@code null}.
     *
     * @return this instance
     */
    public StringPrinter with(Consumer<? super StringBuilder> action) {
        action.accept(output);
        return this;
    }

    /**
     * Formats the string like {@link String#format(String, Object...)}.
     *
     * @param fmt
     *            the formatting string. It must not be {@code null}.
     * @param args
     *            the arguments for formatting. It must not be {@code null}.
     *
     * @return this instance
     */
    public StringPrinter format(String fmt, Object... args) {
        output.append(String.format(fmt, args));
        return this;
    }

    /**
     * Appends a string.
     *
     * @param s
     *            the string to append
     *
     * @return this instance
     */
    public StringPrinter print(String s) {
        output.append(s);
        return this;
    }

    /**
     * Appends a string and {@link #EOL}.
     *
     * @param s
     *            the string to append
     *
     * @return this instance
     */
    public StringPrinter println(String s) {
        return print(s).println();
    }

    /**
     * Appends the {@link #EOL}.
     *
     * @return this instance
     */
    public StringPrinter println() {
        return print(EOL);
    }

    /**
     * @return the builder to append to
     */
    public StringBuilder output() {
        return output;
    }
}
