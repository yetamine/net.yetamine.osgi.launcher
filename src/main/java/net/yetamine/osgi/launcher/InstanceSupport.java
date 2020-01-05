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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import net.yetamine.osgi.launcher.deploying.DeploymentUmbrella;
import net.yetamine.osgi.launcher.logging.Logger;
import org.osgi.framework.Bundle;

/**
 * Provides shared functionality for multiple commands.
 */
final class InstanceSupport extends LoggerSupporter {

    private final Configuration configuration;
    private final InstanceControl control;

    /**
     * Creates a new instance.
     *
     * @param givenControl
     *            the instance control. It must not be {@code null}.
     * @param givenConfiguration
     *            the instance configuration. It must not be {@code null}.
     */
    public InstanceSupport(InstanceControl givenControl, Configuration givenConfiguration) {
        configuration = Objects.requireNonNull(givenConfiguration);
        control = Objects.requireNonNull(givenControl);
    }

    /**
     * Restores original properties from an instance's stored properties, which
     * keeps the existing values, while filling the gaps from the stored values.
     *
     * @param result
     *            the resulting configuration. It must not be {@code null}.
     * @param instance
     *            the instance path. It must not be {@code null}.
     *
     * @return the result
     *
     * @throws SetupException
     *             if could not load the stored properties
     */
    public static Configuration restore(Configuration result, Path instance) {
        final Path path = new InstanceInquiry(instance).path(InstanceLayout.ETC_PATH);

        try {
            restoreProperties(result.systemProperties(), path.resolve(InstanceLayout.SYSTEM_PROPERTIES));
            restoreProperties(result.launchingProperties(), path.resolve(InstanceLayout.LAUNCHING_PROPERTIES));
            restoreProperties(result.frameworkProperties(), path.resolve(InstanceLayout.FRAMEWORK_PROPERTIES));
        } catch (IOException e) {
            throw new SetupException("Could not restore instance properties.", e);
        }

        return result;
    }

    /**
     * Restores original properties from an instance's stored properties, which
     * keeps the existing values, while filling the gaps from the stored values.
     *
     * @param result
     *            the resulting configuration. It must not be {@code null} and
     *            it must have {@link Configuration#instance()} defined.
     *
     * @return the result
     *
     * @throws SetupException
     *             if could not load the stored properties
     */
    public static Configuration restore(Configuration result) {
        return restore(result, result.instance());
    }

    /**
     * Sets the logger to use.
     *
     * @param logger
     *            the logger
     *
     * @return this instance
     */
    public InstanceSupport withLogger(Logger logger) {
        logger(logger);
        return this;
    }

    /**
     * Uninstalls all configured bundles.
     *
     * @param frameworkControl
     *            the framework to process. It must not be {@code null}.
     *
     * @return this instance
     */
    public InstanceSupport uninstall(FrameworkControl frameworkControl) {
        final Collection<String> locations = configuration.uninstallBundles();
        if (locations.isEmpty()) {
            return this;
        }

        frameworkControl.undeploy(uninstallFilter());
        return this;
    }

    /**
     * Creates a filter based on the given expressions.
     *
     * @param expressions
     *            the expressions to use. It must be a collection containing
     *            zero or more restricted glob expressions that are used to
     *            match the bundle locations.
     *
     * @return the filter
     */
    public static Predicate<Bundle> uninstallFilter(Collection<String> expressions) {
        final Collection<BundlePathMatcher> matchers = expressions.stream() ///
                .map(BundlePathMatcher::new)                                ///
                .collect(Collectors.toList());

        return bundle -> matchers.stream().anyMatch(matcher -> matcher.test(bundle.getLocation()));
    }

    /**
     * @return the filter for uninstalling bundles for the configured locations
     */
    public Predicate<? super Bundle> uninstallFilter() {
        return uninstallFilter(configuration.uninstallBundles());
    }

    /**
     * Performs various cleaning operations.
     *
     * @return this instance
     *
     * @throws IOException
     *             if the operation failed
     */
    public InstanceSupport clean() throws IOException {
        if (configuration.cleanInstance()) { // Perform the total clean first, then it can be done
            logger().info("Cleaning the instance.");
            control.clean();
            return this;
        }

        if (configuration.cleanConfiguration()) {
            logger().info("Cleaning the configuration.");
            control.execute(context -> FileHandling.delete(context.path(InstanceLayout.CONF_PATH)));
        }

        return this;
    }

    /**
     * Computes the deployment for the instance.
     *
     * @return the deployment
     *
     * @throws IOException
     *             if the operation failed
     */
    public DeploymentUmbrella deployment() throws IOException {
        // Prepare the deployment plan first as this only reads and does not touch the instance yet
        final DeploymentSetup setup = new DeploymentSetup().withLogger(logger());
        setup.configureDefaults(configuration.launchingProperties());
        for (PathLister bundleLister : configuration.bundles()) {
            setup.configureLocations(bundleLister);
        }

        return setup.deployment();
    }

    /**
     * Configures the instance.
     *
     * @return this instance
     *
     * @throws IOException
     *             if the operation failed
     */
    public InstanceSupport configure() throws IOException {
        logger().debug("Setting up the configuration.");

        control.execute(context -> {
            try {
                final Path target = context.path(InstanceLayout.CONF_PATH);

                if (Files.notExists(target)) {
                    Files.createDirectories(target);
                    for (Path source : configuration.createConfiguration()) {
                        FileHandling.copyTo(target, source);
                    }
                }

                for (Path source : configuration.updateConfiguration()) {
                    FileHandling.copyTo(target, source);
                }
            } catch (IOException e) {
                throw new IOException("Failed to setup the configuration.", e);
            }
        });

        return this;
    }

    /**
     * Stores the instance properties.
     *
     * @return this instance
     *
     * @throws IOException
     *             if the operation failed
     */
    public InstanceSupport storeProperties() throws IOException {
        logger().debug("Storing current properties.");

        control.execute(context -> {
            try {
                // @formatter:off
                final Path path = Files.createDirectories(context.path(InstanceLayout.ETC_PATH));
                PropertiesFile.save(configuration.frameworkProperties(), path.resolve(InstanceLayout.FRAMEWORK_PROPERTIES));
                PropertiesFile.save(configuration.launchingProperties(), path.resolve(InstanceLayout.LAUNCHING_PROPERTIES));
                PropertiesFile.save(configuration.systemProperties(), path.resolve(InstanceLayout.SYSTEM_PROPERTIES));
                // @formatter:on
            } catch (IOException e) {
                throw new IOException("Failed to store current properties.", e);
            }
        });

        return this;
    }

    private static void restoreProperties(Map<String, String> properties, Path path) throws IOException {
        PropertiesFile.optional(path).load().forEach(properties::putIfAbsent); // Fill the missing ones only, keep the overrides
    }
}
