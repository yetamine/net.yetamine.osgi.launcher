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

/**
 * Provides means for configuring the logger.
 *
 * <p>
 * Implementations of this interface must be thread-safe.
 */
public interface LoggerSupport {

    /**
     * @return the current logger
     */
    Logger logger();

    /**
     * Sets the logger.
     *
     * @param value
     *            the logger to set. It may be {@code null} to mute logging.
     */
    void logger(Logger value);
}
