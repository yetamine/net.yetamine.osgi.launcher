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

/**
 * Represents a bundle state in the simplified form.
 */
public enum BundleState {

    // @formatter:off
    INSTALLED(Bundle.INSTALLED),
    ACTIVE(Bundle.ACTIVE),
    STARTING(Bundle.STARTING),
    STOPPING(Bundle.STOPPING),
    RESOLVED(Bundle.RESOLVED),
    UNINSTALLED(Bundle.UNINSTALLED);
    // @formatter:on

    private final int stateMask;

    private BundleState(int mask) {
        stateMask = mask;
    }

    /**
     * Tests if the state is contained in the bit mask.
     *
     * @param mask
     *            the bit mask
     *
     * @return {@code true} if this state is contained
     */
    public boolean in(int mask) {
        return ((mask & stateMask) == stateMask);
    }

    /**
     * @return the bit mask as defined in {@link Bundle#getState()}
     */
    public int stateMask() {
        return stateMask;
    }

    /**
     * Determines the state of a bundle.
     *
     * @param bundle
     *            the bundle to inspect. It must not be {@code null}.
     *
     * @return the state of the bundle
     */
    public static BundleState of(Bundle bundle) {
        return from(bundle.getState());
    }

    /**
     * Determines the state of a bundle.
     *
     * @param mask
     *            the bundle state mask to inspect. It must not be {@code null}.
     *
     * @return the state of the bundle
     */
    public static BundleState from(int mask) {
        if ((mask & Bundle.UNINSTALLED) != 0) {
            return UNINSTALLED;
        }
        if ((mask & Bundle.ACTIVE) != 0) {
            return ACTIVE;
        }
        if ((mask & Bundle.STARTING) != 0) {
            return STARTING;
        }
        if ((mask & Bundle.STOPPING) != 0) {
            return STOPPING;
        }
        if ((mask & Bundle.RESOLVED) != 0) {
            return RESOLVED;
        }
        if ((mask & Bundle.INSTALLED) != 0) {
            return INSTALLED;
        }

        throw new IllegalArgumentException("Could not determine the bundle state for state mask: " + mask);
    }
}
