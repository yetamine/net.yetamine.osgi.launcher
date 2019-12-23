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

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import net.yetamine.osgi.launcher.deploying.BundleAutostart;
import net.yetamine.osgi.launcher.deploying.BundleDeployment;
import net.yetamine.osgi.launcher.deploying.DeploymentAction;
import net.yetamine.osgi.launcher.deploying.DeploymentLocation;
import net.yetamine.osgi.launcher.deploying.DeploymentSettings;
import net.yetamine.osgi.launcher.deploying.DeploymentUmbrella;
import net.yetamine.osgi.launcher.deploying.InputStreamSource;
import net.yetamine.osgi.launcher.logging.Logger;

/**
 * Composes the deployment from the configuration.
 */
public final class DeploymentSetup extends LoggerSupporter {

    /**
     * Name of the property file with the configuration for a deployment
     * location.
     */
    public static final String DEPLOYMENT_PROPERTIES = "deployment.properties";

    /**
     * Name of the property with the deployment actions. The value must be a
     * comma-separated list of case-insensitive {@link DeploymentAction} values.
     */
    public static final String PROPERTY_DEPLOYMENT_ACTION = "deployment.action";

    /**
     * Name of the property with the location root for derived bundle locations.
     * If not specified, the deployment path is used instead.
     */
    private static final String PROPERTY_LOCATION_ROOT = "bundle.location.root";

    /**
     * Name of the property with the deployment filter for searching bundles.
     * The value must be a glob expression.
     */
    public static final String PROPERTY_DEPLOYMENT_FILTER = "deployment.filter";

    /**
     * Name of the property with the default start level. The value must be an
     * integer. If positive, the bundle automatic start on the given start level
     * is persisted, if negative, the start level is set to the negation of that
     * value and stopped persistently. Zero makes no change and other defaults
     * may apply.
     */
    public static final String PROPERTY_START_LEVEL = "start.level";

    /**
     * Bundle-scoped variant of {@link #PROPERTY_DEPLOYMENT_ACTION}.
     */
    public static final String SCOPED_DEPLOYMENT_ACTION = PROPERTY_DEPLOYMENT_ACTION + '@';

    /**
     * Overrides the location of a bundle that would be otherwise derived from
     * {@link #PROPERTY_LOCATION_ROOT} and the bundle relative path.
     */
    public static final String SCOPED_LOCATION = "bundle.location@";

    /**
     * Bundle-scoped variant of {@link #PROPERTY_START_LEVEL}.
     */
    public static final String SCOPED_START_LEVEL = PROPERTY_START_LEVEL + '@';

    private final DeploymentUmbrella deployment = new DeploymentUmbrella();
    private final PathMatcherFactory filterFactory;

    /**
     * Creates a new instance.
     *
     * @param bundleFilterFactory
     *            the factory that provides filters to locate bundles. It must
     *            not be {@code null}.
     */
    public DeploymentSetup(PathMatcherFactory bundleFilterFactory) {
        filterFactory = Objects.requireNonNull(bundleFilterFactory);
    }

    /**
     * Creates a new instance that uses {@link PathMatcherFactory#glob()}.
     */
    public DeploymentSetup() {
        this(PathMatcherFactory.glob());
    }

    /**
     * Sets the logger to use.
     *
     * @param logger
     *            the logger
     *
     * @return this instance
     */
    public DeploymentSetup withLogger(Logger logger) {
        logger(logger);
        return this;
    }

    /**
     * Configures the default settings for {@link #deployment()}.
     *
     * @param defaults
     *            the properties with the defaults. It must not be {@code null}.
     */
    public void configureDefaults(Map<String, String> defaults) {
        configure(deployment.defaultSettings(), defaults);
    }

    /**
     * Configures a set of locations with the provided paths.
     *
     * @param locations
     *            the locations to configure. It must not be {@code null}.
     *
     * @throws IOException
     *             if the operation failed
     *
     * @see #configureLocation(Path)
     */
    public void configureLocations(Iterable<Path> locations) throws IOException {
        for (Path location : locations) {
            configureLocation(location);
        }
    }

    /**
     * Configures a set of locations with the provided paths.
     *
     * @param locations
     *            the locations to configure. It must not be {@code null}.
     *
     * @throws IOException
     *             if the operation failed
     *
     * @see #configureLocation(Path)
     */
    public void configureLocations(PathLister locations) throws IOException {
        try (Stream<Path> paths = locations.paths()) { // Ensure safe closing always
            for (Iterator<Path> it = paths.iterator(); it.hasNext();) {
                configureLocation(it.next());
            }
        }
    }

    /**
     * Configures the given location with the provided path. If the path points
     * to a file, the file is read as a properties file with the location
     * options and its parent directory is used for locating the bundles.
     * Otherwise the path must be a directory where the bundles are located; it
     * may contain {@value #DEPLOYMENT_PROPERTIES} file with the options to use.
     *
     * @param path
     *            the path for the location. It must not be {@code null}.
     *
     * @throws IOException
     *             if the operation failed
     */
    public void configureLocation(Path path) throws IOException {
        final Path location;
        final PropertiesFile file;

        // Normalize the path to get cleaner reports in the case of a failure,
        // otherwise the normalization should occur later for deterministic
        // location identifiers anyway (so it should not be necessary here)

        if ((path.getParent() == null) || Files.isDirectory(path)) {
            location = path.normalize();
            file = PropertiesFile.optional(location.resolve(DEPLOYMENT_PROPERTIES));
        } else {
            file = PropertiesFile.optional(path.normalize());
            location = file.path().getParent();
            assert (location != null);
        }

        logger().debug(logger -> {
            final String message = file.exists()        ///
                    ? "Loading deployment options: "    ///
                    : "Deployment options not found, using defaults instead: ";

            logger.debug(message + file.path());
        });

        configureLocation(location, file.load());
    }

    /**
     * Configures the given location with the provided path and options.
     *
     * @param location
     *            the path for the location. It must not be {@code null}.
     * @param defaults
     *            the configuration options. It must not be {@code null}.
     *
     * @throws IOException
     *             if the operation failed
     */
    public void configureLocation(Path location, Map<String, String> defaults) throws IOException {
        Objects.requireNonNull(location);

        logger().info(logger -> logger.info("Processing deployment location: " + location));

        if (!Files.isDirectory(location)) {
            throw new NoSuchFileException("Location is not a directory: " + location);
        }

        final DeploymentLocation deploymentLocation = defineLocation(location, defaults);
        configure(deploymentLocation, defaults);

        final String locationRoot = deploymentLocation.root();
        discoverBundles(location, locationRoot, defaults);
        applyScopedSettings(locationRoot, defaults);
    }

    /**
     * @return the deployment provided by this instance
     */
    public DeploymentUmbrella deployment() {
        return deployment;
    }

    private DeploymentLocation defineLocation(Path location, Map<String, String> defaults) {
        final String root = defaults.get(PROPERTY_LOCATION_ROOT);
        return (root == null) ? deployment.location(location) : deployment.location(URI.create(root));
    }

    private void configure(DeploymentSettings settings, Map<String, String> defaults) {
        configureDeploymentActions(settings, defaults.get(PROPERTY_DEPLOYMENT_ACTION));
        configureStartLevel(settings, defaults.get(PROPERTY_START_LEVEL));
    }

    private void configureDeploymentActions(DeploymentSettings settings, String value) {
        if (value == null) {
            return;
        }

        // @formatter:off
        final Set<DeploymentAction> actions = Stream.of(value.split(","))
                .map(String::trim)
                .map(this::parseDeploymentAction)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(() -> EnumSet.noneOf(DeploymentAction.class)));
        // @formatter:on

        settings.actions(actions);
    }

    private DeploymentAction parseDeploymentAction(String value) {
        try {
            return DeploymentAction.valueOf(value.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            logger().warn("Could not parse deployment action: " + value);
            return null;
        }
    }

    private void configureStartLevel(DeploymentSettings settings, String startLevel) {
        if (startLevel == null) {
            return;
        }

        configureStartLevel(settings, parseStartLevel(startLevel));
    }

    private static void configureStartLevel(DeploymentSettings settings, int startLevel) {
        if ((startLevel == 0) || (startLevel == Integer.MIN_VALUE)) { // Deal with the integer asymmetric value range
            settings.autostart(null);
            settings.startLevel(0);
            return;
        }

        if (startLevel < 0) {
            settings.autostart(BundleAutostart.STOPPED);
            settings.startLevel(-startLevel);
            return;
        }

        assert (startLevel > 0);
        settings.startLevel(startLevel);
        settings.autostart(BundleAutostart.STARTED);
    }

    private int parseStartLevel(String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            logger().warn("Could not parse start level: " + value, e);
            return 0;
        }
    }

    private void applyScopedSettings(String locationRoot, Map<String, String> defaults) {
        defaults.keySet().stream()                  ///
                .map(DeploymentSetup::scopeOf)      ///
                .filter(Objects::nonNull)           /// Get rid of the nulls (since we have no Option::stream yet)
                .distinct()                         /// Repetitions are quite benign, but rather avoid them
                .forEach(bundlePath -> applyScopedSettings(locationRoot, defaults, bundlePath));
    }

    private void applyScopedSettings(String locationRoot, Map<String, String> defaults, String bundlePath) {
        String bundleLocation = bundleLocation(bundlePath, locationRoot, defaults);
        final BundleDeployment bundleSettings = deployment.bundle(bundleLocation);
        configureStartLevel(bundleSettings, defaults.get(SCOPED_START_LEVEL + bundlePath));
        configureDeploymentActions(bundleSettings, defaults.get(SCOPED_DEPLOYMENT_ACTION + bundlePath));
    }

    private static String scopeOf(String name) {
        // Keep the branches ordered by the probability

        if (name.startsWith(SCOPED_START_LEVEL)) {
            return name.substring(SCOPED_START_LEVEL.length());
        }

        if (name.startsWith(SCOPED_DEPLOYMENT_ACTION)) {
            return name.substring(SCOPED_DEPLOYMENT_ACTION.length());
        }

        // This case ensures that explicitly relocated bundles are registered,
        // even if the bundle is absent and therefore could not be discovered.
        // This allows troubleshooting with wild configurations.

        if (name.startsWith(SCOPED_LOCATION)) {
            return name.substring(SCOPED_LOCATION.length());
        }

        return null;
    }

    private void discoverBundles(Path location, String root, Map<String, String> defaults) throws IOException {
        final PathMatcher filter = filter(location, defaults.get(PROPERTY_DEPLOYMENT_FILTER));
        final List<Path> bundlePaths = findBundles(location, filter);

        for (Path bundlePath : bundlePaths) {
            final String bundleLocation = bundleLocation(uniformPath(bundlePath), root, defaults);
            deployment.bundle(bundleLocation).source(InputStreamSource.from(location.resolve(bundlePath)));
        }
    }

    private static List<Path> findBundles(Path location, PathMatcher filter) throws IOException {
        try (Stream<Path> paths = Files.walk(location)) {
            return paths                                ///
                    .filter(Files::isRegularFile)       /// Not so efficient for many filtered files, which is not typical though
                    .map(location::relativize)          /// Relativize before path matching, otherwise relative filters fail
                    .filter(filter::matches)            ///
                    .sorted()                           /// Just make it nicely deterministic
                    .collect(Collectors.toList());
        }
    }

    private static String bundleLocation(String bundlePath, String locationRoot, Map<String, String> defaults) {
        final String result = defaults.get(SCOPED_LOCATION + bundlePath);
        return (result != null) ? result : locationRoot + bundlePath;
    }

    private static String uniformPath(Path path) {
        return StreamSupport.stream(path.spliterator(), false).map(Path::toString).collect(Collectors.joining("/"));
    }

    private PathMatcher filter(Path path, String filter) {
        return (filter == null) ? DeploymentSetup::defaultFilter : filterFactory.create(path, filter);
    }

    /**
     * Implements the default filter that finds {@code .jar} files.
     *
     * <p>
     * The implementation does not use the {@link PathMatcher} intentionally as
     * it serves as the fallback for all cases when the matches could not be
     * made. Moreover the matcher would rely on the default filesystem while
     * nothing prevents the caller to refer to a different filesystem.
     *
     * @param path
     *            the path to test. It must not be {@code null}.
     *
     * @return {@code true} if the path might refer a file with the file name
     *         with <i>.jar</i> suffix (but consisting of the suffix only)
     */
    private static boolean defaultFilter(Path path) {
        final int lastNameIndex = path.getNameCount() - 1;
        if (lastNameIndex < 0) {
            return false;
        }

        final String suffix = ".jar";
        final String lastName = path.getName(lastNameIndex).toString();
        return (lastName.length() > suffix.length()) && lastName.endsWith(suffix);
    }
}
