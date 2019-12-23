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

import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;

/**
 * A command extension that supports integration with system properties, but
 * rather delegates to a handler than updates the properties directly, which
 * does not work well with tests for instance.
 */
abstract class SystemAwareCommand extends Command {

    private final BiConsumer<? super String, ? super String> systemPropertiesHandler;

    /**
     * Creates a new instance.
     *
     * @param systemPropertiesUpdate
     *            the handler to take care of updating configured system
     *            properties. It must not be {@code null}.
     */
    public SystemAwareCommand(BiConsumer<? super String, ? super String> systemPropertiesUpdate) {
        systemPropertiesHandler = Objects.requireNonNull(systemPropertiesUpdate);
    }

    /**
     * Requests updating system properties.
     *
     * @param properties
     *            the properties to update. It must not be {@code null}.
     */
    protected final void updateSystemProperties(Map<String, String> properties) {
        properties.forEach(systemPropertiesHandler);
    }

    /**
     * Requests updating a system property.
     *
     * @param name
     *            the property name. It must not be {@code null}.
     * @param value
     *            the property value
     */
    protected final void updateSystemProperty(String name, String value) {
        systemPropertiesHandler.accept(name, name);
    }
}
