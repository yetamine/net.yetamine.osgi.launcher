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
 * The {@link Logger} implementation that never logs.
 */
enum LoggerOff implements Logger {

    INSTANCE;

    /**
     * @see java.lang.Enum#toString()
     */
    @Override
    public String toString() {
        return "Logger[level=off]";
    }

    /**
     * @see net.yetamine.osgi.launcher.logging.Logger#level()
     */
    @Override
    public Level level() {
        return Level.FORCE;
    }

    /**
     * @see net.yetamine.osgi.launcher.logging.Logger#log(net.yetamine.osgi.launcher.logging.Logger.Level,
     *      java.lang.String, java.lang.Throwable)
     */
    @Override
    public void log(Level level, String message, Throwable t) {
        // Do nothing
    }
}
