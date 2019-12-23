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

import java.io.IOException;
import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;

/**
 * Encapsulates deployment of a bundle.
 */
public final class BundleDeployment extends DeploymentSettings {

    private final String location;
    private InputStreamSource source;

    /**
     * Creates a new instance.
     *
     * @param bundleLocation
     *            the location for the bundle. It must not be {@code null} and
     *            it should be a unique identifier in the normalized form, e.g.,
     *            a URI.
     */
    public BundleDeployment(String bundleLocation) {
        location = Objects.requireNonNull(bundleLocation);
    }

    /**
     * Creates a copy of an existing instance.
     *
     * @param origin
     *            the source instance. It must not be {@code null}.
     */
    public BundleDeployment(BundleDeployment origin) {
        super(origin);
        source = origin.source;
        location = origin.location;
    }

    /**
     * Creates a new instance.
     *
     * @param bundleLocation
     *            the location for the bundle. It must not be {@code null} and
     *            it should be a unique identifier in the normalized form, e.g.,
     *            a URI.
     * @param settings
     *            the settings to inherit. It must not be {@code null}.
     */
    public BundleDeployment(String bundleLocation, DeploymentSettings settings) {
        super(settings);
        location = Objects.requireNonNull(bundleLocation);
    }

    /**
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        // @formatter:off
        return "BundleDeployment[location=" + location
                + ", startLevel=" + startLevel()
                + ", autostart=" + autostart().orElse(null)
                + ", actions=" + actions()
                + ", source=" + source().orElse(null)
                + ']';
        // @formatter:on
    }

    /**
     * Executes the required deployment actions.
     *
     * @param context
     *            the context to use for the actions. It should rather be the
     *            system bundle context.
     *
     * @return the bundle that was processed if the object could be retrieved
     *
     * @throws BundleException
     *             if the operation failed
     * @throws IOException
     *             if the bundle source failed to deliver the data
     */
    public Optional<Bundle> execute(BundleContext context) throws BundleException, IOException {
        Objects.requireNonNull(context);

        // Note that Concierge does not implement getBundle by location correctly
        final Bundle result = context.getBundle(location);
        final Set<DeploymentAction> actions = actions();

        if (result == null) {
            // All other possible scenarios depend on the bundle being present
            if (actions.contains(DeploymentAction.INSTALL) && (source != null)) {
                final Bundle installed = BundleProvisioning.installBundle(context, location, source);
                BundleProvisioning.updateStartLevel(installed, startLevel());
                BundleProvisioning.updateAutostart(installed, autostart().orElse(null));
                return Optional.of(installed);
            }

            return Optional.empty();
        }

        if (actions.contains(DeploymentAction.UNINSTALL) && (source == null)) {
            BundleProvisioning.uninstallBundle(result);
        } else if (actions.contains(DeploymentAction.UPDATE) && (source != null)) {
            BundleProvisioning.updateBundle(result, source);
            BundleProvisioning.updateStartLevel(result, startLevel());
            BundleProvisioning.updateAutostart(result, autostart().orElse(null));
        }

        return Optional.of(result);
    }

    /**
     * @see net.yetamine.osgi.launcher.deploying.DeploymentSettings#actions(java.util.Collection)
     */
    @Override
    public BundleDeployment actions(Collection<DeploymentAction> values) {
        super.actions(values);
        return this;
    }

    /**
     * @see net.yetamine.osgi.launcher.deploying.DeploymentSettings#autostart(net.yetamine.osgi.launcher.deploying.BundleAutostart)
     */
    @Override
    public BundleDeployment autostart(BundleAutostart value) {
        super.autostart(value);
        return this;
    }

    /**
     * @see net.yetamine.osgi.launcher.deploying.DeploymentSettings#startLevel(int)
     */
    @Override
    public BundleDeployment startLevel(int value) {
        super.startLevel(value);
        return this;
    }

    /**
     * Sets the source of the bundle data.
     *
     * @param value
     *            the source of the bundle data. Providing {@code null} suggest
     *            the absence of the source.
     *
     * @return this instance
     */
    public BundleDeployment source(InputStreamSource value) {
        source = value;
        return this;
    }

    /**
     * @return the source
     */
    public Optional<InputStreamSource> source() {
        return Optional.ofNullable(source);
    }

    /**
     * @return the location of the bundle
     */
    public String location() {
        return location;
    }
}
