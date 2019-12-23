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

import net.yetamine.osgi.launcher.logging.Logger;
import net.yetamine.osgi.launcher.logging.LoggerSupport;

/**
 * A skeletal implementation of {@link LoggerSupport} that can be used as a
 * convenient base class for classes which can inherit from {@link Object}.
 */
abstract class LoggerSupporter implements LoggerSupport {

    private volatile Logger logger = Logger.off();

    /**
     * Prepares a new instance.
     */
    protected LoggerSupporter() {
        // Default constructor
    }

    /**
     * @see net.yetamine.osgi.launcher.logging.LoggerSupport#logger(net.yetamine.osgi.launcher.logging.Logger)
     */
    @Override
    public final void logger(Logger value) {
        logger = Logger.fallback(value);
    }

    /**
     * @see net.yetamine.osgi.launcher.logging.LoggerSupport#logger()
     */
    @Override
    public final Logger logger() {
        return logger;
    }
}
