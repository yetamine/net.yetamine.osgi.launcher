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
 * Represents a function that may throw a checked exception.
 *
 * @param <T>
 *            the type of the function's parameter
 * @param <R>
 *            the return type of the function
 * @param <X>
 *            the exception that the action may throw
 */
@FunctionalInterface
public interface ThrowingFunction<T, R, X extends Exception> {

    /**
     * Invokes the function.
     *
     * @param arg
     *            the function's argument
     *
     * @return the result of the function
     *
     * @throws X
     *             if the action failed
     */
    R apply(T arg) throws X;
}
