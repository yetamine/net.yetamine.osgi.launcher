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

import java.util.Collection;
import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;

/**
 * Encapsulates the common settings for deploying a bundle.
 */
public class DeploymentSettings {

    private final Set<DeploymentAction> actions;
    private BundleAutostart autostart;
    private int startLevel;

    /**
     * Creates a new instance.
     */
    public DeploymentSettings() {
        actions = EnumSet.noneOf(DeploymentAction.class);
    }

    /**
     * Creates a copy of an existing instance.
     *
     * @param origin
     *            the source instance. It must not be {@code null}.
     */
    public DeploymentSettings(DeploymentSettings origin) {
        actions = EnumSet.copyOf(origin.actions);
        startLevel = origin.startLevel;
        autostart = origin.autostart;
    }

    /**
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        // @formatter:off
        return "DeploymentSettings[startLevel=" + startLevel
                + ", autostart=" + autostart
                + ", actions=" + actions
                + ']';
        // @formatter:on
    }

    /**
     * Sets the actions to perform.
     *
     * @param values
     *            the actions to perform. It must not be {@code null}.
     *
     * @return this instance
     */
    public DeploymentSettings actions(Collection<DeploymentAction> values) {
        // Keep the original reference, so that updating the live set works
        final Set<DeploymentAction> copy = EnumSet.copyOf(values);
        actions.clear();
        actions.addAll(copy);
        return this;
    }

    /**
     * @return the live set of the actions to perform
     */
    public Set<DeploymentAction> actions() {
        return actions;
    }

    /**
     * Sets the autostart setting.
     *
     * @param value
     *            the autostart setting. If {@code null}, no autostart setting
     *            shall be changed.
     *
     * @return this instance
     */
    public DeploymentSettings autostart(BundleAutostart value) {
        autostart = value;
        return this;
    }

    /**
     * @return the autostart setting
     */
    public Optional<BundleAutostart> autostart() {
        return Optional.ofNullable(autostart);
    }

    /**
     * Sets the start level.
     *
     * @param value
     *            the start level. It may be zero no to set the start level,
     *            otherwise it must be a positive value.
     *
     * @return this instance
     */
    public DeploymentSettings startLevel(int value) {
        if (value < 0) {
            throw new IllegalArgumentException("Invalid start level: " + value);
        }

        startLevel = value;
        return this;
    }

    /**
     * @return the start level, or zero for no change required
     */
    public int startLevel() {
        return startLevel;
    }
}
