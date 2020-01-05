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

import java.net.InetSocketAddress;
import java.nio.file.Path;
import java.time.Duration;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.osgi.framework.Constants;

/**
 * Holds the complete configuration for deploying and starting an instance.
 */
final class Configuration {

    /**
     * Name of the property that defines the shutdown timeout. The value should
     * be in the ISO-8601 duration format or in the form of {@code TIME UNIT}
     * where {@code UNIT} must be <i>m</i>, <i>s</i> or <i>ms</i> (to specify
     * minutes, seconds or milliseconds). The special value <i>none</i> or
     * <i>null</i> means waiting forever, which is the default.
     */
    public static final String PROPERTY_SHUTDOWN_TIMEOUT = "shutdown.timeout";

    private Map<String, String> systemProperties = new TreeMap<>();
    private Map<String, String> frameworkProperties = new TreeMap<>();
    private Map<String, String> launchingProperties = new TreeMap<>();
    private InetSocketAddress commandAddress;
    private String commandSecret = "";
    private List<PathLister> bundles = new ArrayList<>();
    private List<Path> createConfiguration = new ArrayList<>();
    private List<Path> updateConfiguration = new ArrayList<>();
    private List<String> parameters = new ArrayList<>();
    private List<String> uninstallBundles = new ArrayList<>();
    private Path instance;
    private boolean cleanInstance;
    private boolean cleanConfiguration;
    private boolean dumpStatus;
    private boolean skipDeploy;
    private boolean skipStart;

    /**
     * Creates a new blank instance.
     */
    public Configuration() {
        // Default constructor
    }

    /**
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        // @formatter:off
        return "InstanceLaunch[instance=" + instance
                + ", parameters=" + parameters
                + ", bundles=" + bundles
                + ", cleanInstance=" + cleanInstance
                + ", cleanConfiguration=" + cleanConfiguration
                + ", createConfiguration=" + createConfiguration
                + ", updateConfiguration=" + updateConfiguration
                + ", frameworkProperties=" + frameworkProperties
                + ", launchingProperties=" + launchingProperties
                + ", systemProperties=" + systemProperties
                + ", uninstallBundles=" + uninstallBundles
                + ", commandLink=" + commandAddress
                + ", dumpStatus=" + dumpStatus
                + ", skipDeploy=" + skipDeploy
                + ", skipStart=" + skipStart
                + ']';
        // @formatter:on
    }

    /**
     * Validates this instance and throws an appropriate exception if the
     * validation fails.
     *
     * <p>
     * This method should be used after a set of modification and before
     * providing the modified instance outside of the scope that assumes
     * possible inconsistencies and deficiencies.
     *
     * @return this instance
     *
     * @throws IllegalStateException
     *             if the current state does not represent a plausible state
     */
    public Configuration validate() {
        if (instance == null) {
            throw new IllegalStateException("Missing instance path.");
        }

        instance = instance.normalize();
        updateFrameworkProperties();
        shutdownTimeout(); // Try parsing
        return this;
    }

    /**
     * Sets the path to the instance.
     *
     * @param value
     *            the path to the instance
     */
    public void instance(Path value) {
        instance = value;
    }

    /**
     * @return the instance path
     */
    public Path instance() {
        return instance;
    }

    /**
     * Sets if the whole instance should be cleaned.
     *
     * @param value
     *            {@code true} to perform the clean-up
     */
    public void cleanInstance(boolean value) {
        cleanInstance = value;
    }

    /**
     * @return {@code true} if the instance should be cleaned
     */
    public boolean cleanInstance() {
        return cleanInstance;
    }

    /**
     * Sets if the configuration should be cleaned.
     *
     * @param value
     *            {@code true} to perform the clean-up
     */
    public void cleanConfiguration(boolean value) {
        cleanConfiguration = value;
    }

    /**
     * @return {@code true} if the instance should be cleaned
     */
    public boolean cleanConfiguration() {
        return cleanConfiguration;
    }

    /**
     * @return a list of paths to be copied, recursively, to the configuration
     *         directory of the instance, in the specified order, when the
     *         instance shall be initialized
     */
    public List<Path> createConfiguration() {
        return createConfiguration;
    }

    /**
     * @return a list of paths to be copied, recursively, to the configuration
     *         directory of the instance, in the specified order, to update it
     *         always
     */
    public List<Path> updateConfiguration() {
        return updateConfiguration;
    }

    /**
     * @return the list of parameters for the application itself
     */
    public List<String> parameters() {
        return parameters;
    }

    /**
     * Sets the command link.
     *
     * @param value
     *            the address to receive commands. It may be {@code null} to
     *            disable the command link.
     */
    public void commandAddress(InetSocketAddress value) {
        commandAddress = value;
    }

    /**
     * @return the address to receive commands if the feature is enabled
     */
    public Optional<InetSocketAddress> commandAddress() {
        return Optional.ofNullable(commandAddress);
    }

    /**
     * Sets the secret to use for the command link.
     *
     * @param value
     *            the secret value. It must not be {@code null} and the empty
     *            value indicates that a random secret should be generated if
     *            needed.
     */
    public void commandSecret(String value) {
        commandSecret = Objects.requireNonNull(value);
    }

    /**
     * @return the secret to use, with an empty string indicating no actual
     *         secret, so that a random secret should rather be generated
     */
    public String commandSecret() {
        return commandSecret;
    }

    /**
     * @return the list of paths to search for bundles
     */
    public List<PathLister> bundles() {
        return bundles;
    }

    /**
     * @return the list of expression for the locations to remove
     */
    public List<String> uninstallBundles() {
        return uninstallBundles;
    }

    /**
     * @return the properties passed to the framework
     */
    public Map<String, String> frameworkProperties() {
        return frameworkProperties;
    }

    /**
     * @return the properties used by the launcher
     */
    public Map<String, String> launchingProperties() {
        return launchingProperties;
    }

    /**
     * @return the system properties to be set up as soon as possible
     */
    public Map<String, String> systemProperties() {
        return systemProperties;
    }

    /**
     * Sets if the detailed status should be displayed.
     *
     * @param value
     *            {@code true} the verbose information should be displayed
     */
    public void dumpStatus(boolean value) {
        dumpStatus = value;
    }

    /**
     * @return {@code true} if the detailed status should be shown
     */
    public boolean dumpStatus() {
        return dumpStatus;
    }

    /**
     * Sets if the deploy phase should be skipped.
     *
     * @param value
     *            {@code true} the deploy phase should be skipped
     */
    public void skipDeploy(boolean value) {
        skipDeploy = value;
    }

    /**
     * @return {@code true} if the deploy phase should be skipped
     */
    public boolean skipDeploy() {
        return skipDeploy;
    }

    /**
     * Sets if the start phase should be skipped.
     *
     * @param value
     *            {@code true} the start phase should be skipped
     */
    public void skipStart(boolean value) {
        skipStart = value;
    }

    /**
     * @return {@code true} if the start phase should be skipped
     */
    public boolean skipStart() {
        return skipStart;
    }

    /**
     * @return the shutdown timeout from {@link #launchingProperties()}
     */
    public Optional<Duration> shutdownTimeout() {
        final String value = launchingProperty(PROPERTY_SHUTDOWN_TIMEOUT);

        if (value == null) {
            return Optional.empty();
        }

        switch (value) {
            case "none":
            case "null":
                return Optional.empty();
        }

        Duration result;

        try {
            result = Duration.parse(value);
        } catch (DateTimeParseException e) {
            try {
                result = timeValue(value);
            } catch (IllegalArgumentException inner) {
                inner.addSuppressed(e);
                throw new IllegalArgumentException("Could not parse shutdown timeout: " + value, inner);
            }
        }

        if (result.isNegative() || result.isZero()) {
            throw new IllegalArgumentException("Negative or zero shutdown timeout not allowed.");
        }

        return Optional.of(result);
    }

    private static Duration timeValue(String string) {
        // Used basically once, so it is not necessary to cache this pattern as a static field
        final Matcher matcher = Pattern.compile("(?<value>\\d+)\\s*(?<unit>(m|s|ms))").matcher(string);

        if (!matcher.matches()) {
            throw new IllegalArgumentException("Could not parse time value: " + string);
        }

        return Duration.of(Integer.parseInt(matcher.group("value")), timeUnit(matcher.group("unit")));
    }

    private static TemporalUnit timeUnit(String unit) {
        switch (unit) {
            case "m":
                return ChronoUnit.MINUTES;

            case "s":
                return ChronoUnit.SECONDS;

            case "ms":
                return ChronoUnit.MILLIS;

            default:
                throw new IllegalArgumentException("Unknown time unit: " + unit);
        }
    }

    private void updateFrameworkProperties() {
        frameworkProperties.remove(Constants.FRAMEWORK_STORAGE);
    }

    private String launchingProperty(String name) {
        return launchingProperties.get(name);
    }
}
