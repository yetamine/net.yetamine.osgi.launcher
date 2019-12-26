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
import java.nio.file.InvalidPathException;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;
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
     * Name of the property with the location root for derived bundle locations.
     * If not specified, the deployment path is used instead.
     */
    public static final String PROPERTY_BUNDLE_LOCATION_ROOT = "bundle.location.root";

    /**
     * Name of the property with the deployment actions. The value must be a
     * comma-separated list of case-insensitive {@link DeploymentAction} values.
     */
    public static final String PROPERTY_DEPLOYMENT_ACTION = "deployment.action";

    /**
     * Name of the property with the filter for searching bundles to deploy. The
     * value must be a glob expression.
     */
    public static final String PROPERTY_DEPLOYMENT_SEARCH = "deployment.search";

    /**
     * Name of the property with the default start level. The value must be an
     * integer. If positive, the bundle automatic start on the given start level
     * is persisted, if negative, the start level is set to the negation of that
     * value and stopped persistently. Zero makes no change and other defaults
     * may apply.
     */
    public static final String PROPERTY_START_LEVEL = "start.level";

    /**
     * Overrides the location of a bundle that would be otherwise derived from
     * {@link #PROPERTY_BUNDLE_LOCATION_ROOT} and the bundle relative path.
     */
    public static final String SCOPED_BUNDLE_LOCATION = "bundle.location@";

    /**
     * Bundle-scoped variant of {@link #PROPERTY_DEPLOYMENT_ACTION}.
     */
    public static final String SCOPED_DEPLOYMENT_ACTION = PROPERTY_DEPLOYMENT_ACTION + '@';

    /**
     * Bundle-scoped variant of {@link #PROPERTY_START_LEVEL}.
     */
    public static final String SCOPED_START_LEVEL = PROPERTY_START_LEVEL + '@';

    private final DeploymentUmbrella deployment = new DeploymentUmbrella();
    private final PathMatcherFactory searchFilterFactory;

    /**
     * Creates a new instance.
     *
     * @param givenSearchFilterFactory
     *            the factory that provides filters for searching for bundles.
     *            It must not be {@code null}.
     */
    public DeploymentSetup(PathMatcherFactory givenSearchFilterFactory) {
        searchFilterFactory = Objects.requireNonNull(givenSearchFilterFactory);
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
        configureDefaults(deployment.defaultSettings(), defaults);
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
     * @param properties
     *            the configuration options. It must not be {@code null}.
     *
     * @throws IOException
     *             if the operation failed
     */
    public void configureLocation(Path location, Map<String, String> properties) throws IOException {
        Objects.requireNonNull(location);

        logger().info(logger -> logger.info("Processing deployment location: " + location));

        if (!Files.isDirectory(location)) {
            throw new NoSuchFileException("Location is not a directory: " + location);
        }

        final DeploymentLocation deploymentLocation = defineLocation(location, properties);
        configureDefaults(deploymentLocation, properties);

        final String locationRoot = deploymentLocation.root();
        final Map<String, BundleDeployment> bundles = new HashMap<>();
        discoverBundles(location, locationRoot, properties, bundles::put);
        redefineBundles(location, locationRoot, properties, bundles::put);
        applyScopedSettings(bundles, properties);
    }

    /**
     * @return the deployment provided by this instance
     */
    public DeploymentUmbrella deployment() {
        return deployment;
    }

    private DeploymentLocation defineLocation(Path location, Map<String, String> defaults) {
        final String root = defaults.get(PROPERTY_BUNDLE_LOCATION_ROOT);
        if ((root == null) || root.isEmpty()) {
            return deployment.location(location);
        }

        // Make the normalized form that allows appending uniform bundle paths to make a bundle location
        if (root.endsWith("/") || root.endsWith(":")) {
            return deployment.location(root);
        }

        return deployment.location(URI.create(root));
    }

    private void configureDefaults(DeploymentSettings settings, Map<String, String> defaults) {
        configureDeploymentActions(settings, defaults.get(PROPERTY_DEPLOYMENT_ACTION));
        configureStartLevel(settings, defaults.get(PROPERTY_START_LEVEL));
    }

    private boolean configureDeploymentActions(DeploymentSettings settings, String value) {
        if (value == null) {
            return false;
        }

        // @formatter:off
        final Set<DeploymentAction> actions = Stream.of(value.split(","))
                .map(String::trim)
                .map(this::parseDeploymentAction)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(() -> EnumSet.noneOf(DeploymentAction.class)));
        // @formatter:on

        settings.actions(actions);
        return true;
    }

    private DeploymentAction parseDeploymentAction(String value) {
        try {
            return DeploymentAction.valueOf(value.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            logger().warn("Could not parse deployment action: " + value);
            return null;
        }
    }

    private boolean configureStartLevel(DeploymentSettings settings, String startLevel) {
        if (startLevel == null) {
            return false;
        }

        configureStartLevel(settings, parseStartLevel(startLevel));
        return true;
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

    private void applyScopedSettings(Map<String, BundleDeployment> bundles, Map<String, String> properties) {
        final List<BundlePathMatcher> matchers = bundleMatchers(properties);

        bundles.forEach((path, bundle) -> {
            final List<BundlePathMatcher> matches = bundleMatches(matchers, path);
            if (matches.isEmpty()) {
                return;
            }

            final BundlePathMatcher match = matches.get(0);

            if (matches.size() > 1) {
                logger().warn(logger -> {
                    final String message = "Bundle path '" + path + "' matching with multiple expressions: " + matches;
                    logger.warn(message);
                });

                return;
            }

            final String bundleSpecifier = match.toString();
            configureStartLevel(bundle, properties.get(SCOPED_START_LEVEL + bundleSpecifier));
            configureDeploymentActions(bundle, properties.get(SCOPED_DEPLOYMENT_ACTION + bundleSpecifier));
        });
    }

    /**
     * Finds all best-matching matchers that match the path.
     *
     * @param matchers
     *            the collection of matchers ordered consistently with
     *            {@link BundlePathMatcher#compareTo(BundlePathMatcher)}. It
     *            must not be {@code null}.
     * @param path
     *            the path to match. It must not be {@code null}.
     *
     * @return the list of matchers
     */
    private List<BundlePathMatcher> bundleMatches(Iterable<BundlePathMatcher> matchers, String path) {
        final List<BundlePathMatcher> result = new ArrayList<>();

        int ranking = -1;
        for (BundlePathMatcher matcher : matchers) {
            if (matcher.test(path)) {
                if (result.isEmpty()) { // First match sets the rank to find
                    ranking = matcher.ranking();
                    result.add(matcher);
                    continue;
                }

                if (ranking == matcher.ranking()) {
                    result.add(matcher);
                    continue;
                }

                break; // Thanks to sorting no need to continue
            }
        }

        return result;
    }

    /**
     * Makes a list of unique matchers for uniform bundle paths.
     *
     * <p>
     * The matchers are ordered by their natural ordering, which prefers the
     * more specific matchers.
     *
     * @param properties
     *            the bundle source properties. It must not be {@code null}.
     *
     * @return the matcher list
     */
    private static List<BundlePathMatcher> bundleMatchers(Map<String, String> properties) {
        return properties.keySet().stream()         ///
                .map(DeploymentSetup::scopeOf)      ///
                .filter(Objects::nonNull)           /// Get rid of the nulls (since we have no Option::stream yet)
                .distinct()                         /// Unique matchers needed for detecting their uniqueness
                .map(BundlePathMatcher::new)        ///
                .sorted()                           /// Prefer the specific ones
                .collect(Collectors.toList());
    }

    private static String scopeOf(String name) {
        // Keep the branches ordered by the probability
        if (name.startsWith(SCOPED_START_LEVEL)) {
            return name.substring(SCOPED_START_LEVEL.length());
        }
        if (name.startsWith(SCOPED_DEPLOYMENT_ACTION)) {
            return name.substring(SCOPED_DEPLOYMENT_ACTION.length());
        }

        return null;
    }

    /**
     * Defines bundles and redefines existing bundle definitions using
     * bundle-specific configuration entries.
     *
     * @param location
     *            the bundle source location. It must not be {@code null}.
     * @param locationRoot
     *            the location root for bundle locations. It must not be
     *            {@code null}.
     * @param properties
     *            the properties of the bundle source. It must not be
     *            {@code null}.
     * @param onDefine
     *            the callback to invoke for each discovered bundle. It must not
     *            be {@code null}.
     */
    private void redefineBundles(Path location, String locationRoot, Map<String, String> properties, BiConsumer<? super String, ? super BundleDeployment> onDefine) {
        properties.forEach((name, value) -> {
            if (!name.startsWith(SCOPED_BUNDLE_LOCATION)) {
                return;
            }

            final String bundlePath = name.substring(SCOPED_BUNDLE_LOCATION.length());

            if (bundlePath.isEmpty()) {
                logger().warn(logger -> logger.warn("Invalid property: " + name));
                return;
            }

            final Path filePath;
            try {
                filePath = location.resolve(bundlePath);
            } catch (InvalidPathException e) {
                logger().warn(logger -> logger.warn("Invalid bundle path: " + bundlePath));
                return;
            }

            final String uniformPath = uniformPath(filePath);
            final InputStreamSource source = Files.exists(filePath) ? InputStreamSource.from(filePath) : null;
            final BundleDeployment bundle = deployment.bundle(locationRoot + uniformPath).source(source);
            onDefine.accept(uniformPath, bundle);
        });
    }

    /**
     * Discovers bundles in the given bundle source.
     *
     * @param location
     *            the bundle source location. It must not be {@code null}.
     * @param locationRoot
     *            the location root for bundle locations. It must not be
     *            {@code null}.
     * @param properties
     *            the properties of the bundle source. It must not be
     *            {@code null}.
     * @param onDefine
     *            the callback to invoke for each discovered bundle. It must not
     *            be {@code null}.
     *
     * @throws IOException
     *             if the operation fails
     */
    private void discoverBundles(Path location, String locationRoot, Map<String, String> properties, BiConsumer<? super String, ? super BundleDeployment> onDefine) throws IOException {
        final PathMatcher filter = searchFilter(location, properties.get(PROPERTY_DEPLOYMENT_SEARCH));
        final List<Path> bundleFilePaths = findBundles(location, filter);

        for (Path bundleFilePath : bundleFilePaths) {
            final String bundleUniformPath = uniformPath(bundleFilePath);
            final String bundleLocation = bundleLocation(bundleUniformPath, locationRoot, properties);
            final InputStreamSource bundleSource = InputStreamSource.from(location.resolve(bundleFilePath));
            final BundleDeployment bundle = deployment.bundle(bundleLocation).source(bundleSource);
            onDefine.accept(bundleUniformPath, bundle);
        }
    }

    /**
     * Finds bundles from the given bundle source.
     *
     * @param location
     *            the bundle source location. It must not be {@code null}.
     * @param filter
     *            the bundle search filter. It must not be {@code null}.
     *
     * @return the list of bundle file paths, relative to the given location and
     *         ordered by the length and components
     *
     * @throws IOException
     *             if the operation fails
     */
    private static List<Path> findBundles(Path location, PathMatcher filter) throws IOException {
        try (Stream<Path> paths = Files.walk(location)) {
            return paths                                    ///
                    .filter(Files::isRegularFile)           /// Not so efficient for many filtered files, which is not typical though
                    .map(location::relativize)              /// Relativize before path matching, otherwise relative filters fail
                    .filter(filter::matches)                ///
                    .sorted(DeploymentSetup::comparePaths)  /// Just make it nicely deterministic
                    .collect(Collectors.toList());
        }
    }

    private static String bundleLocation(String bundlePath, String locationRoot, Map<String, String> defaults) {
        final String result = defaults.get(SCOPED_BUNDLE_LOCATION + bundlePath);
        return ((result == null) || result.isEmpty()) ? (locationRoot + bundlePath) : result;
    }

    /**
     * Returns the platform-independent path using <i>/</i> as the path
     * component separator.
     *
     * @param path
     *            the path. It must not be {@code null}.
     *
     * @return the uniform path
     */
    private static String uniformPath(Path path) {
        return StreamSupport.stream(path.spliterator(), false).map(Path::toString).collect(Collectors.joining("/"));
    }

    /**
     * Compares paths by their lengths and secondary by their components, which
     * sorts the paths in layers like a BFS traversal would do.
     *
     * @param a
     *            the first path. It must not be {@code null}.
     * @param b
     *            the second path. It must not be {@code null}.
     *
     * @return the result as {@link Comparator#compare(Object, Object)} defines
     */
    private static int comparePaths(Path a, Path b) {
        int result = Integer.compare(a.getNameCount(), b.getNameCount());
        if (result != 0) {
            return result;
        }

        for (Iterator<Path> aIt = a.iterator(), bIt = b.iterator(); aIt.hasNext() && bIt.hasNext();) {
            result = aIt.next().toString().compareTo(bIt.next().toString());
            if (result != 0) {
                return result;
            }
        }

        return 0;
    }

    private PathMatcher searchFilter(Path path, String filter) {
        return (filter == null) ? DeploymentSetup::defaultSearchFilter : searchFilterFactory.create(path, filter);
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
    private static boolean defaultSearchFilter(Path path) {
        final int lastNameIndex = path.getNameCount() - 1;
        if (lastNameIndex < 0) {
            return false;
        }

        final String suffix = ".jar";
        final String lastName = path.getName(lastNameIndex).toString();
        return (lastName.length() > suffix.length()) && lastName.endsWith(suffix);
    }
}
