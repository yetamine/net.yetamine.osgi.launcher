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
 * Encapsulates an executable action.
 *
 * <p>
 * Actions may be stateful or use shared resources and not meant for concurrent
 * execution. Therefore, unless the implementation states otherwise, concurrent
 * use should be avoided. Implementations may limit repeated instance use too.
 *
 * @param <T>
 *            the type of the action's parameter
 * @param <X>
 *            the exception that the action may throw
 */
@FunctionalInterface
public interface Executable<T, X extends Exception> {

    /**
     * Executes the action.
     *
     * @param arg
     *            the action argument
     *
     * @throws X
     *             if the action failed
     */
    void execute(T arg) throws X;
}
