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

/**
 * Indicates a problem with an argument syntax.
 */
public class SyntaxException extends IllegalArgumentException {

    private static final long serialVersionUID = 1L;

    /**
     * Creates a new instance with no details.
     */
    public SyntaxException() {
        // Default constructor
    }

    /**
     * Creates a new instance with the specified detail message.
     *
     * @param message
     *            the detail message
     */
    public SyntaxException(String message) {
        super(message);
    }

    /**
     * Creates a new instance with the specified cause and a detail message
     * constructed from the cause (if not {@code null}).
     *
     * @param cause
     *            the cause
     */
    public SyntaxException(Throwable cause) {
        super(cause);
    }

    /**
     * Creates a new instance with the specified detail message and cause.
     *
     * @param message
     *            the detail message
     * @param cause
     *            the cause
     */
    public SyntaxException(String message, Throwable cause) {
        super(message, cause);
    }
}
