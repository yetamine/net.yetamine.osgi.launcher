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
import java.io.InputStream;
import java.util.Objects;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.startlevel.BundleStartLevel;

/**
 * Adapts provisioning operations to provide a more suitable interface.
 */
final class BundleProvisioning {

    private BundleProvisioning() {
        throw new AssertionError();
    }

    /**
     * Installs the bundle.
     *
     * @param context
     *            the context to use for the deployment. It must not be
     *            {@code null} and should be the system bundle context.
     * @param location
     *            the location of the bundle. It must not be {@code null}.
     * @param source
     *            the installation source. It must not be {@code null}.
     *
     * @return the installed bundle
     *
     * @throws BundleException
     *             if the installation fails
     * @throws IOException
     *             if the source fails to deliver the bundle
     */
    public static Bundle installBundle(BundleContext context, String location, InputStreamSource source) throws BundleException, IOException {
        try (InputStream is = source.open()) {
            return context.installBundle(location, is);
        }
    }

    /**
     * Updates the bundle.
     *
     * @param bundle
     *            the bundle to update. It must not be {@code null}.
     * @param source
     *            the installation source. It must not be {@code null}.
     *
     * @return the updated bundle
     *
     * @throws BundleException
     *             if the update fails
     * @throws IOException
     *             if the source fails to deliver the bundle
     */
    public static Bundle updateBundle(Bundle bundle, InputStreamSource source) throws BundleException, IOException {
        try (InputStream is = source.open()) {
            bundle.update(is);
        }

        return bundle;
    }

    /**
     * Uninstalls the given bundle unconditionally.
     *
     * @param bundle
     *            the bundle to uninstall. It must not be {@code null}.
     *
     * @return {@code true} if the bundle was actually uninstalled
     *
     * @throws BundleException
     *             if the operation fails
     */
    public static boolean uninstallBundle(Bundle bundle) throws BundleException {
        try { // No need to check the precondition since it may change meanwhile anyway
            bundle.uninstall();
        } catch (IllegalStateException e) {
            if (BundleState.UNINSTALLED.in(bundle.getState())) {
                return false;
            }

            throw new BundleException("Could not uninstall bundle " + bundle, BundleException.INVALID_OPERATION, e);
        }

        return true;
    }

    /**
     * Updates the start level of the bundle.
     *
     * @param bundle
     *            the bundle to configure. It must not be {@code null}.
     * @param startLevel
     *            the start level to set. Zero keeps the current start level. It
     *            must not be negative.
     *
     * @throws BundleException
     *             if the operation failed
     */
    public static void updateStartLevel(Bundle bundle, int startLevel) throws BundleException {
        Objects.requireNonNull(bundle);
        if (startLevel == 0) {
            return;
        }

        final BundleStartLevel setup = bundle.adapt(BundleStartLevel.class);

        if (setup == null) {
            throw new BundleException("Could not set start level of " + bundle, BundleException.UNSUPPORTED_OPERATION);
        }

        try {
            setup.setStartLevel(startLevel);
        } catch (IllegalStateException e) {
            throw new BundleException("Could not set start level of " + bundle, BundleException.INVALID_OPERATION);
        }
    }

    /**
     * Updates the autostart setting to the given bundle if provided.
     *
     * @param bundle
     *            the bundle to update. It must not be {@code null}.
     * @param setting
     *            the setting. It may be {@code null} to no change.
     *
     * @throws BundleException
     *             if the operation failed
     */
    public static void updateAutostart(Bundle bundle, BundleAutostart setting) throws BundleException {
        Objects.requireNonNull(bundle);

        if (setting != null) {
            setting.applyTo(bundle);
        }
    }
}
