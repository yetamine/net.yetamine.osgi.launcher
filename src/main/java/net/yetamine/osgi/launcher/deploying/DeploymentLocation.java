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

package net.yetamine.osgi.launcher.deploying;

import java.net.URI;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Objects;

/**
 * Encapsulates settings for a location and its children.
 */
public final class DeploymentLocation extends DeploymentSettings {

    private final String root;

    /**
     * Creates a new instance.
     *
     * @param locationRoot
     *            the location root. It must not be {@code null}.
     */
    public DeploymentLocation(String locationRoot) {
        root = Objects.requireNonNull(locationRoot);
    }

    /**
     * Creates a new instance.
     *
     * @param locationRoot
     *            the location root. It must not be {@code null}.
     * @param settings
     *            the settings to inherit. It must not be {@code null}.
     */
    public DeploymentLocation(String locationRoot, DeploymentSettings settings) {
        super(settings);
        root = Objects.requireNonNull(locationRoot);
    }

    /**
     * Creates a new instance.
     *
     * @param locationRoot
     *            the location root. It must not be {@code null}.
     * @param settings
     *            the settings to inherit. It must not be {@code null}.
     */
    public DeploymentLocation(Path locationRoot, DeploymentSettings settings) {
        this(locationUri(locationRoot), settings);
    }

    /**
     * Returns a location URI for the given {@link Path} that should point to a
     * directory.
     *
     * @param location
     *            the location to normalize. It must not be {@code null}.
     *
     * @return the normalized location URI
     */
    public static String locationUri(Path location) {
        return locationUri(location.toAbsolutePath().normalize().toUri());
    }

    /**
     * Converts a location URI into the normalized form which ends with a slash
     * character.
     *
     * @param location
     *            the location to normalize. It must not be {@code null}.
     *
     * @return the normalized location URI
     */
    public static String locationUri(URI location) {
        final String result = location.toString();
        return result.endsWith("/") ? result : result + '/';
    }

    /**
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        // @formatter:off
        return "DeploymentLocation[root=" + root
                + ", startLevel=" + startLevel()
                + ", autostart=" + autostart().orElse(null)
                + ", actions=" + actions()
                + ']';
        // @formatter:on
    }

    /**
     * @see net.yetamine.osgi.launcher.deploying.DeploymentSettings#actions(java.util.Collection)
     */
    @Override
    public DeploymentLocation actions(Collection<DeploymentAction> values) {
        super.actions(values);
        return this;
    }

    /**
     * @see net.yetamine.osgi.launcher.deploying.DeploymentSettings#autostart(net.yetamine.osgi.launcher.deploying.BundleAutostart)
     */
    @Override
    public DeploymentLocation autostart(BundleAutostart value) {
        super.autostart(value);
        return this;
    }

    /**
     * @see net.yetamine.osgi.launcher.deploying.DeploymentSettings#startLevel(int)
     */
    @Override
    public DeploymentLocation startLevel(int value) {
        super.startLevel(value);
        return this;
    }

    /**
     * @return the root of this location
     */
    public String root() {
        return root;
    }
}
