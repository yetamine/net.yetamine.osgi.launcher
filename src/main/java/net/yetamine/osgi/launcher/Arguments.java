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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Supports argument list parsing.
 */
final class Arguments {

    private final List<String> arguments;
    private int position;

    /**
     * Creates a new instance.
     *
     * @param givenArguments
     *            the arguments to parse. It must not be {@code null}.
     */
    public Arguments(List<String> givenArguments) {
        arguments = Collections.unmodifiableList(new ArrayList<>(givenArguments));
    }

    /**
     * Tests if an argument looks like an option.
     *
     * @param argument
     *            the argument to test. It must not be {@code null}.
     *
     * @return {@code true} if the argument starts with <i>-</i> (a dash)
     */
    public static boolean isOption(String argument) {
        return argument.startsWith("-");
    }

    /**
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "Arguments[position=" + position + ", arguments=" + arguments + ']';
    }

    /**
     * @return the arguments
     */
    public List<String> arguments() {
        return arguments;
    }

    /**
     * @return the current position
     */
    public int position() {
        return position;
    }

    /**
     * Sets the position explicitly.
     *
     * @param value
     *            the position to set
     *
     * @return this instance
     *
     * @throws IndexOutOfBoundsException
     *             if the position does not fit into valid bounds
     */
    public Arguments position(int value) {
        if ((position < 0) || (arguments.size() < position)) {
            throw new IndexOutOfBoundsException();
        }

        position = value;
        return this;
    }

    /**
     * Moves to the next argument if possible.
     *
     * @return this instance
     */
    public Arguments next() {
        if (position < arguments.size()) {
            ++position;
        }

        return this;
    }

    /**
     * Moves to the previous argument.
     *
     * @return this instance
     *
     * @throws IllegalStateException
     *             if there is no previous argument
     */
    public Arguments back() {
        if (position == 0) {
            throw new IllegalStateException();
        }

        --position;
        return this;
    }

    /**
     * @return the current argument if any
     */
    public Optional<String> string() {
        return (position < arguments.size()) ? Optional.of(arguments.get(position)) : Optional.empty();
    }

    /**
     * @return the current argument if looks like an option
     */
    public Optional<String> option() {
        return string().filter(Arguments::isOption);
    }

    /**
     * @return the current argument as a Boolean if the value is <i>true</i> or
     *         <i>false</i> case-insensitive
     */
    public Optional<Boolean> optionSwitch() {
        return string().flatMap(Arguments::toBoolean);
    }

    /**
     * @return the list of remaining arguments
     */
    public List<String> remaining() {
        return arguments.subList(position, arguments.size());
    }

    /**
     * @return the argument as a string
     *
     * @throws SyntaxException
     *             if there are no more arguments
     */
    public String requireString() {
        return string().orElseThrow(() -> new SyntaxException("Missing a required argument."));
    }

    /**
     * Returns the argument as a string.
     *
     * @param name
     *            the name of the argument that is used in the case of an error.
     *            It must not be {@code null}.
     *
     * @return the argument as a string
     */
    public String requireString(String name) {
        return string().orElseThrow(() -> new SyntaxException("Missing required argument " + name));
    }

    private static Optional<Boolean> toBoolean(String argument) {
        switch (argument.toLowerCase(Locale.ROOT)) {
            case "false":
                return Optional.of(Boolean.FALSE);

            case "true":
                return Optional.of(Boolean.TRUE);

            default:
                return Optional.empty();
        }
    }
}
