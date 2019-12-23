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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * Encapsulates a deployment.
 *
 * <p>
 * The deployment can be constructed by incremental refining deployment settings
 * and adding or updating bundle bindings. At the end the deployment captures an
 * ordered bundle deployment list that can be applied to a framework instance.
 *
 * <p>
 * Note that the effects always reflect the previous state of the instance,
 * e.g., when a bundle binding is created, the current defaults apply. Then
 * settings for the bindings then remain, unless changed explicitly, even if the
 * applied defaults may change again.
 */
public final class DeploymentUmbrella {

    private final DeploymentSettings defaultSettings = new DeploymentSettings();
    private final SortedMap<String, DeploymentLocation> locations = new TreeMap<>();
    private final Map<String, BundleDeployment> bundles = new HashMap<>();

    /**
     * Creates a new instance.
     */
    public DeploymentUmbrella() {
        // Default constructor
    }

    /**
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "DeploymentDescriptor[locations=" + locations.keySet() + ']';
    }

    /**
     * @return the default settings used for creating new locations and for
     *         bundles that could not be bound to a location
     */
    public DeploymentSettings defaultSettings() {
        return defaultSettings;
    }

    /**
     * Returns a location settings.
     *
     * <p>
     * If the settings representation for the given does not exist yet, it is
     * created and initialized with the current {@link #defaultSettings()}.
     *
     * @param location
     *            the location whose settings shall be returned. It must not be
     *            {@code null}.
     *
     * @return the location settings
     */
    public DeploymentLocation location(String location) {
        return locations.computeIfAbsent(Objects.requireNonNull(location), this::createLocation);
    }

    /**
     * Returns a location settings.
     *
     * <p>
     * If the settings representation for the given does not exist yet, it is
     * created and initialized with the current {@link #defaultSettings()}.
     *
     * @param location
     *            the location whose settings shall be returned. It must not be
     *            {@code null}.
     *
     * @return the location settings
     */
    public DeploymentLocation location(Path location) {
        return location(DeploymentLocation.locationUri(location));
    }

    /**
     * Returns a location settings.
     *
     * <p>
     * If the settings representation for the given does not exist yet, it is
     * created and initialized with the current {@link #defaultSettings()}.
     *
     * @param location
     *            the location whose settings shall be returned. It must not be
     *            {@code null}.
     *
     * @return the location settings
     */
    public DeploymentLocation location(URI location) {
        return location(DeploymentLocation.locationUri(location));
    }

    /**
     * Returns a bundle deployment settings.
     *
     * @param location
     *            the location of the bundle. It must not be {@code null}.
     *
     * @return the bundle deployment settings
     */
    public BundleDeployment bundle(String location) {
        return bundles.computeIfAbsent(Objects.requireNonNull(location), this::createBundle);
    }

    /**
     * @return an ordered list of the bundle deployments
     */
    public List<BundleDeployment> bundles() {
        final List<BundleDeployment> result = new ArrayList<>(bundles.values());
        result.sort(DeploymentOrdering.comparator());
        return result;
    }

    private BundleDeployment createBundle(String location) {
        return new BundleDeployment(location, settings(location));
    }

    private DeploymentLocation createLocation(String root) {
        return new DeploymentLocation(root, defaultSettings);
    }

    private DeploymentSettings settings(String location) {
        final SortedMap<String, DeploymentLocation> head = locations.headMap(location);

        if (head.isEmpty()) {
            return defaultSettings;
        }

        final String last = head.lastKey();
        if (location.startsWith(last)) {
            return head.get(last);
        }

        return defaultSettings;
    }
}
