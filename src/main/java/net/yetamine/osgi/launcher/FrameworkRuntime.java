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
import java.time.Duration;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.ServiceLoader;
import java.util.Spliterators;
import java.util.TreeMap;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import net.yetamine.osgi.launcher.deploying.BundleDeployment;
import net.yetamine.osgi.launcher.deploying.BundleState;
import net.yetamine.osgi.launcher.deploying.DeploymentUmbrella;
import net.yetamine.osgi.launcher.logging.Logger;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.FrameworkEvent;
import org.osgi.framework.launch.Framework;
import org.osgi.framework.launch.FrameworkFactory;

/**
 * Encapsulates the launch of the framework.
 */
public final class FrameworkRuntime extends LoggerSupporter implements FrameworkControl {

    /** Avoid the dependency on a newer Framework API. */
    private static final long SYSTEM_BUNDLE_ID = 0;

    private final Framework framework;
    private final Map<String, String> properties;
    private volatile Duration shutdownTimeout;
    private boolean killed;

    /**
     * Creates a new instance.
     *
     * @param frameworkProperties
     *            the properties to pass to the factory. It must not be
     *            {@code null}.
     * @param frameworkFactory
     *            the factory to use for making the framework instance. It must
     *            not be {@code null}.
     *
     * @throws BundleException
     *             if the framework instance could not be created and
     *             initialized
     */
    public FrameworkRuntime(Map<String, String> frameworkProperties, FrameworkFactory frameworkFactory) throws BundleException {
        properties = Collections.unmodifiableMap(new TreeMap<>(frameworkProperties)); // Make it nicely sorted for printing
        framework = frameworkFactory.newFramework(frameworkProperties);
        framework.init();
    }

    /**
     * Creates a new instance with the single {@link FrameworkFactory} that
     * could be found.
     *
     * @param frameworkProperties
     *            the properties to pass to the factory. It must not be
     *            {@code null}.
     *
     * @throws BundleException
     *             if the framework instance could not be created and
     *             initialized
     */
    public FrameworkRuntime(Map<String, String> frameworkProperties) throws BundleException {
        this(frameworkProperties, frameworkFactory());
    }

    /**
     * Finds the single framework factory service.
     *
     * @param classLoader
     *            the class loader to use. It may be {@code null} to use
     *            {@link Thread#getContextClassLoader()} instead.
     *
     * @return the single framework factory service if there is such
     *
     * @throws NoSuchElementException
     *             if there are more than one or no factory
     */
    public static FrameworkFactory frameworkFactory(ClassLoader classLoader) {
        final ServiceLoader<FrameworkFactory> loader = ServiceLoader.load(FrameworkFactory.class, classLoader);
        final List<FrameworkFactory> factories = streamFrom(loader.iterator()).collect(Collectors.toList());
        if (factories.size() == 1) {
            return factories.get(0);
        }

        throw new NoSuchElementException("Exactly one FrameworkFactory service not found.");
    }

    /**
     * Finds the single framework factory service using the class loader of this
     * class.
     *
     * @return the single framework factory service if there is such
     *
     * @throws NoSuchElementException
     *             if there are more than one or no factory
     */
    public static FrameworkFactory frameworkFactory() {
        return frameworkFactory(FrameworkRuntime.class.getClassLoader());
    }

    /**
     * Sets the logger to use.
     *
     * @param logger
     *            the logger
     *
     * @return this instance
     */
    public FrameworkRuntime withLogger(Logger logger) {
        logger(logger);
        return this;
    }

    /**
     * @see net.yetamine.osgi.launcher.FrameworkControl#framework()
     */
    @Override
    public Framework framework() {
        return framework;
    }

    /**
     * @see net.yetamine.osgi.launcher.FrameworkControl#executeOnFramework(net.yetamine.osgi.launcher.Executable)
     */
    @Override
    public <X extends Exception> FrameworkRuntime executeOnFramework(Executable<? super Framework, ? extends X> operation) throws X {
        operation.execute(framework);
        return this;
    }

    /**
     * @see net.yetamine.osgi.launcher.FrameworkControl#executeOnController(net.yetamine.osgi.launcher.Executable)
     */
    @Override
    public <X extends Exception> FrameworkControl executeOnController(Executable<? super FrameworkControl, ? extends X> operation) throws X {
        operation.execute(this);
        return this;
    }

    /**
     * @see net.yetamine.osgi.launcher.FrameworkControl#deploy(net.yetamine.osgi.launcher.deploying.DeploymentUmbrella)
     */
    @Override
    public FrameworkRuntime deploy(DeploymentUmbrella deployment) {
        final BundleContext systemBundle = framework.getBundleContext();

        Stream.of(systemBundle.getBundles())                                ///
                .filter(bundle -> bundle.getBundleId() != SYSTEM_BUNDLE_ID) /// Avoid touching the system bundle
                .map(Bundle::getLocation)                                   ///
                .forEachOrdered(deployment::bundle);                        /// Update with defaults for the location

        deployment.bundles().forEach(bundle -> deploy(bundle, systemBundle));
        return this;
    }

    /**
     * @see net.yetamine.osgi.launcher.FrameworkControl#deploy(net.yetamine.osgi.launcher.deploying.BundleDeployment)
     */
    @Override
    public FrameworkRuntime deploy(BundleDeployment deployment) {
        deploy(deployment, framework.getBundleContext());
        return this;
    }

    /**
     * @see net.yetamine.osgi.launcher.FrameworkControl#undeploy(java.util.function.Predicate)
     */
    @Override
    public FrameworkControl undeploy(Predicate<? super Bundle> filter) {
        Objects.requireNonNull(filter);
        for (Bundle bundle : framework.getBundleContext().getBundles()) {
            if (filter.test(bundle)) {
                uninstall(bundle);
            }
        }

        return this;
    }

    /**
     * @see net.yetamine.osgi.launcher.FrameworkControl#shutdownTimeout(java.time.Duration)
     */
    @Override
    public FrameworkRuntime shutdownTimeout(Duration value) {
        if ((value != null) && (value.isNegative() || value.isZero())) {
            throw new IllegalArgumentException("Shutdown timeout must have a positive duration.");
        }

        shutdownTimeout = value;
        return this;
    }

    /**
     * Launches the framework.
     *
     * @param onStart
     *            the callback to be invoked when {@link Framework#start()}
     *            completes successfully. It must not be {@code null}.
     *
     * @return {@code true} if the framework stopped, {@code false} when it was
     *         killed before it could be started or restarted between updates
     *
     * @throws ExecutionException
     *             if a framework operation failed
     * @throws InterruptedException
     *             if waiting for the framework to stop was interrupted
     */
    public boolean launch(Consumer<? super Framework> onStart) throws ExecutionException, InterruptedException {
        Objects.requireNonNull(onStart);

        while (true) {
            logger().debug("Framework to be started.");

            synchronized (this) {
                if (killed) {
                    logger().debug("Framework start aborted.");
                    return false;
                }

                try {
                    framework.start();
                } catch (BundleException e) {
                    throw new ExecutionException("Failed to start the framework.", e);
                }

                onStart.accept(framework);
            }

            if (framework.waitForStop(0).getType() != FrameworkEvent.STOPPED_UPDATE) {
                logger().debug("Framework stopped.");
                return true;
            }

            logger().debug("Framework stopped due to a system bundle updated and shall be restarted.");
        }
    }

    /**
     * @see net.yetamine.osgi.launcher.FrameworkControl#launch()
     */
    @Override
    public boolean launch() throws ExecutionException, InterruptedException {
        return launch(that -> {});
    }

    /**
     * @see net.yetamine.osgi.launcher.FrameworkControl#stop()
     */
    @Override
    public boolean stop() throws ExecutionException, InterruptedException {
        try {
            logger().debug("Framework to be stopped.");
            framework.stop();
            waitForStop();
            return !running();
        } catch (BundleException e) {
            throw new ExecutionException("Failed to stop the framework.", e);
        }
    }

    /**
     * @see net.yetamine.osgi.launcher.FrameworkControl#kill()
     */
    @Override
    public boolean kill() {
        try {
            synchronized (this) {
                killed = true;
                logger().debug("Framework to be killed.");
                framework.stop();
            }

            waitForStop();
        } catch (BundleException e) {
            logger().warn("Stopping the framework finished with an error.", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        return !running();
    }

    /**
     * @see net.yetamine.osgi.launcher.FrameworkControl#running()
     */
    @Override
    public boolean running() {
        return ((framework().getState() & (Framework.ACTIVE | Framework.STARTING | Framework.STOPPING)) == 0);
    }

    /**
     * @return the properties used for creating the framework
     */
    public Map<String, String> properties() {
        return properties;
    }

    private void deploy(BundleDeployment deployment, BundleContext systemBundle) {
        logger().debug(logger -> logger.debug("Executing operation " + deployment));
        try {
            deployment.execute(systemBundle);
        } catch (BundleException | IOException e) {
            logger().error(logger -> logger.error("Failed to execute deployment actions: " + deployment.location(), e));
        }
    }

    private void uninstall(Bundle bundle) {
        logger().debug(logger -> logger.debug("Uninstalling bundle: " + bundle.getLocation()));
        try {
            bundle.uninstall();
        } catch (BundleException | IllegalStateException e) {
            if (BundleState.UNINSTALLED.in(bundle.getState())) {
                logger().error(logger -> logger.error("Failed to uninstall bundle: " + bundle, e));
            }
        }
    }

    private void waitForStop() throws InterruptedException {
        if (framework.waitForStop(shutdownTimeoutMillis()).getType() == FrameworkEvent.WAIT_TIMEDOUT) {
            logger().warn("Timeout when waiting for framework to terminate.");
        }
    }

    private long shutdownTimeoutMillis() {
        final Duration duration = shutdownTimeout;
        return (duration == null) ? 0 : duration.toMillis();
    }

    private static <T> Stream<T> streamFrom(Iterator<? extends T> iterator) {
        return StreamSupport.stream(Spliterators.spliteratorUnknownSize(iterator, 0), false);
    }
}
