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

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;

/**
 * Defines the autostart setting of a bundle.
 */
public enum BundleAutostart {

    /**
     * The bundle should not be started automatically due to its start level.
     */
    STOPPED {

        /**
         * @see net.yetamine.osgi.launcher.deploying.BundleAutostart#applyTo(org.osgi.framework.Bundle)
         */
        @Override
        public void applyTo(Bundle bundle) throws BundleException {
            if (isFragmentBundle(bundle)) {
                return;
            }

            try {
                bundle.stop();
            } catch (IllegalStateException e) {
                if (BundleState.UNINSTALLED.in(bundle.getState())) {
                    throw new BundleException("Could not stop " + bundle, BundleException.INVALID_OPERATION, e);
                }
            }
        }
    },

    /**
     * The bundle should start automatically due to its start level.
     */
    STARTED {

        /**
         * @see net.yetamine.osgi.launcher.deploying.BundleAutostart#applyTo(org.osgi.framework.Bundle)
         */
        @Override
        public void applyTo(Bundle bundle) throws BundleException {
            if (isFragmentBundle(bundle)) {
                return;
            }

            try {
                bundle.start(Bundle.START_ACTIVATION_POLICY);
            } catch (IllegalStateException e) {
                throw new BundleException("Could not start " + bundle, BundleException.INVALID_OPERATION, e);
            }
        }
    };

    /**
     * Sets the autostart setting to the given bundle.
     *
     * @param bundle
     *            the bundle to apply the setting to. It must not be
     *            {@code null}.
     *
     * @throws BundleException
     *             if the operation failed
     */
    public abstract void applyTo(Bundle bundle) throws BundleException;

    static boolean isFragmentBundle(Bundle bundle) {
        return (bundle.getHeaders().get(Constants.FRAGMENT_HOST) != null);
    }
}
