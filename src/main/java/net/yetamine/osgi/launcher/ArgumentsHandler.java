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
 * Encapsulates the strategy for handling arguments.
 *
 * @param <T>
 *            the type of the context
 */
@FunctionalInterface
interface ArgumentsHandler<T> {

    /**
     * Handles the arguments.
     *
     * <p>
     * If the handler throws an exception, the state of the arguments and the
     * context is undefined and their further should be avoided, unless the
     * implementation asserts otherwise.
     *
     * @param args
     *            the arguments to handle. It must not be {@code null}.
     * @param context
     *            the context for handling, usually the object to receive the
     *            results of the handler. It must not be {@code null}.
     *
     * @throws SyntaxException
     *             if the arguments do not have the expected syntax
     */
    void handle(Arguments args, T context);
}
