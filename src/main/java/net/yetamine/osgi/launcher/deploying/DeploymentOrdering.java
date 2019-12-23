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

import java.util.Comparator;
import java.util.Set;

/**
 * Implements deterministic bundle deployment ordering that brings less hassle.
 */
public final class DeploymentOrdering {

    private DeploymentOrdering() {
        throw new AssertionError();
    }

    /**
     * @return the convenient deployment-order comparator based on
     *         {@link #compare(BundleDeployment, BundleDeployment)}
     */
    public static Comparator<BundleDeployment> comparator() {
        return DeploymentOrdering::compare;
    }

    /**
     * Compares two {@link BundleDeployment} instances with the respect to the
     * convenience of deployment procedure, so that it mitigates conflicts and
     * problems.
     *
     * @param a
     *            the first operand. It must not be {@code null}.
     * @param b
     *            the second operand. It must not be {@code null}.
     *
     * @return the usual expression of less or greater instance
     *
     * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
     */
    public static int compare(BundleDeployment a, BundleDeployment b) {
        int result = compareActions(a.actions(), b.actions());
        if (result != 0) {
            return result;
        }

        result = compareAutostart(a.autostart().orElse(null), b.autostart().orElse(null));
        if (result != 0) {
            return result;
        }

        result = compareStartLevel(a.startLevel(), b.startLevel());
        if (result != 0) {
            return result;
        }

        return a.location().compareTo(b.location());
    }

    private static int compareActions(Set<DeploymentAction> a, Set<DeploymentAction> b) {
        return Integer.compare(rank(a), rank(b));
    }

    private static int compareAutostart(BundleAutostart a, BundleAutostart b) {
        if (a == null) {
            return (b == null) ? 0 : 1;
        }

        if (b == null) {
            return -1;
        }

        return a.compareTo(b);
    }

    private static int compareStartLevel(int a, int b) {
        // Make zero the least number of all thanks to the negative/positive asymmetry,
        // while reverting the order of other values
        final int x = -(a - Integer.MIN_VALUE);
        final int y = -(b - Integer.MIN_VALUE);
        return Integer.compare(y, x);
    }

    private static int rank(Set<DeploymentAction> actions) {
        int result = 0;

        for (DeploymentAction action : actions) {
            result += 1 << action.ordinal();
        }

        return result;
    }
}
