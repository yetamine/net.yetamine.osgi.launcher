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
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

import net.yetamine.osgi.launcher.deploying.BundleDeployment;
import net.yetamine.osgi.launcher.deploying.DeploymentUmbrella;
import net.yetamine.osgi.launcher.logging.Logger;
import net.yetamine.osgi.launcher.logging.LoggerSupport;
import net.yetamine.osgi.launcher.remoting.CommandLink;
import net.yetamine.osgi.launcher.remoting.CommandServer;
import net.yetamine.osgi.launcher.remoting.CryptoProtection;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.launch.Framework;
import org.osgi.framework.launch.FrameworkFactory;

/**
 * Runs a framework instance.
 */
public final class InstanceRuntime implements FrameworkControl, LoggerSupport {

    /** The command to stop an instance. */
    public static final String COMMAND_STOP = "stop";

    /** Name of the property with the instance root path. */
    public static final String PROPERTY_INSTANCE_ROOT = "net.yetamine.osgi.launcher.instance";
    /** Name of the property with the instance configuration path. */
    public static final String PROPERTY_INSTANCE_CONF = "net.yetamine.osgi.launcher.instance.configuration";

    private final FrameworkRuntime runtime;
    private final InstanceControl control;
    private Consumer<? super InstanceRuntime> onLaunch;

    /**
     * Creates a new instance.
     *
     * @param instanceControl
     *            the instance control. It must not be {@code null}.
     * @param frameworkRuntime
     *            the framework instance bound to the instance control. It must
     *            not be {@code null}.
     */
    private InstanceRuntime(InstanceControl instanceControl, FrameworkRuntime frameworkRuntime) {
        runtime = frameworkRuntime;
        control = instanceControl;
    }

    /**
     * Creates a new instance.
     *
     * @param instanceControl
     *            the instance control. It must not be {@code null}.
     * @param frameworkFactory
     *            the framework factory. It must not be {@code null}.
     * @param frameworkProperties
     *            the properties for the new framework instance. It must not be
     *            {@code null}.
     *
     * @return the new instance
     *
     * @throws BundleException
     *             if the framework could not be initialized
     */
    public static InstanceRuntime create(InstanceControl instanceControl, FrameworkFactory frameworkFactory, Map<String, String> frameworkProperties) throws BundleException {
        return instanceControl.invoke(context -> {
            final Map<String, String> updatedProperties = frameworkProperties(context, frameworkProperties);
            final FrameworkRuntime frameworkRuntime = new FrameworkRuntime(updatedProperties, frameworkFactory);
            return new InstanceRuntime(context, frameworkRuntime);
        });
    }

    /**
     * @see net.yetamine.osgi.launcher.logging.LoggerSupport#logger()
     */
    @Override
    public Logger logger() {
        return runtime.logger();
    }

    /**
     * @see net.yetamine.osgi.launcher.logging.LoggerSupport#logger(net.yetamine.osgi.launcher.logging.Logger)
     */
    @Override
    public void logger(Logger value) {
        runtime.logger(value);
    }

    /**
     * Sets the logger to use.
     *
     * @param logger
     *            the logger
     *
     * @return this instance
     */
    public InstanceRuntime withLogger(Logger logger) {
        logger(logger);
        return this;
    }

    /**
     * @see net.yetamine.osgi.launcher.FrameworkControl#framework()
     */
    @Override
    public Framework framework() {
        return runtime.framework();
    }

    /**
     * @see net.yetamine.osgi.launcher.FrameworkControl#executeOnFramework(net.yetamine.osgi.launcher.Executable)
     */
    @Override
    public <X extends Exception> InstanceRuntime executeOnFramework(Executable<? super Framework, ? extends X> operation) throws X {
        runtime.executeOnFramework(operation);
        return this;
    }

    /**
     * @see net.yetamine.osgi.launcher.FrameworkControl#executeOnController(net.yetamine.osgi.launcher.Executable)
     */
    @Override
    public <X extends Exception> InstanceRuntime executeOnController(Executable<? super FrameworkControl, ? extends X> operation) throws X {
        runtime.executeOnController(operation);
        return this;
    }

    /**
     * @see net.yetamine.osgi.launcher.FrameworkControl#deploy(net.yetamine.osgi.launcher.deploying.DeploymentUmbrella)
     */
    @Override
    public InstanceRuntime deploy(DeploymentUmbrella deployment) {
        runtime.deploy(deployment);
        return this;
    }

    /**
     * @see net.yetamine.osgi.launcher.FrameworkControl#deploy(net.yetamine.osgi.launcher.deploying.BundleDeployment)
     */
    @Override
    public InstanceRuntime deploy(BundleDeployment deployment) {
        runtime.deploy(deployment);
        return this;
    }

    /**
     * @see net.yetamine.osgi.launcher.FrameworkControl#undeploy(java.util.function.Predicate)
     */
    @Override
    public InstanceRuntime undeploy(Predicate<? super Bundle> filter) {
        runtime.undeploy(filter);
        return this;
    }

    /**
     * @see net.yetamine.osgi.launcher.FrameworkControl#shutdownTimeout(java.time.Duration)
     */
    @Override
    public InstanceRuntime shutdownTimeout(Duration value) {
        runtime.shutdownTimeout(value);
        return this;
    }

    /**
     * Sets the callback for launching the framework.
     *
     * @param value
     *            the callback to set. It must not be {@code null}.
     *
     * @return this instance
     */
    public InstanceRuntime onLaunch(Consumer<? super InstanceRuntime> value) {
        onLaunch = value;
        return this;
    }

    /**
     * Launches the framework instance with an optional command server that
     * receives commands to stop and waits for the framework to terminate.
     *
     * @param address
     *            the address to listen at. It may be {@code null} for no
     *            listening.
     * @param secret
     *            the secret to use. If {@code null} or empty, a random secret
     *            would be generated.
     *
     * @return see {@link FrameworkRuntime#launch()}
     *
     * @throws ExecutionException
     *             if the execution failed
     * @throws InterruptedException
     *             if waiting for the termination was interrupted. The framework
     *             might be stopping, but not stopped yet. The command server is
     *             stopped, however.
     */
    public boolean launch(InetSocketAddress address, String secret) throws ExecutionException, InterruptedException {
        return (address != null) ? launch(new CommandLink(address, secret)) : launch();
    }

    /**
     * Launches the framework instance with a command server that receives
     * commands to stop and waits for the framework to terminate.
     *
     * @param link
     *            the command link parameters. It must not be {@code null}.
     *
     * @return see {@link FrameworkRuntime#launch()}
     *
     * @throws ExecutionException
     *             if the execution failed
     * @throws InterruptedException
     *             if waiting for the termination was interrupted. The framework
     *             might be stopping, but not stopped yet. The command server is
     *             stopped, however.
     */
    public boolean launch(CommandLink link) throws InterruptedException, ExecutionException {
        Objects.requireNonNull(link);

        try {
            deleteCommandLinkFile(); // Delete first to avoid misleading data if the next part fails

            try (CommandServer server = commandServer(link)) {
                final InetSocketAddress listening = server.address().orElseThrow(() -> {
                    return new IllegalStateException("Could not establish the command link.");
                });

                storeCommandLinkFile(link.address(listening));
                return launchRuntime();
            } finally {
                deleteCommandLinkFile();
            }
        } catch (GeneralSecurityException | IOException e) {
            throw new ExecutionException(e);
        }
    }

    /**
     * @see net.yetamine.osgi.launcher.FrameworkControl#launch()
     */
    @Override
    public boolean launch() throws ExecutionException, InterruptedException {
        try {
            deleteCommandLinkFile();
            return launchRuntime();
        } catch (IOException e) {
            throw new ExecutionException(e);
        }
    }

    /**
     * @see net.yetamine.osgi.launcher.FrameworkControl#stop()
     */
    @Override
    public boolean stop() throws ExecutionException, InterruptedException {
        return runtime.stop();
    }

    /**
     * @see net.yetamine.osgi.launcher.FrameworkControl#kill()
     */
    @Override
    public boolean kill() {
        return runtime.kill();
    }

    /**
     * @see net.yetamine.osgi.launcher.FrameworkControl#running()
     */
    @Override
    public boolean running() {
        return runtime.running();
    }

    /**
     * @return the properties used for creating the framework
     */
    public Map<String, String> properties() {
        return runtime.properties();
    }

    /**
     * Executes the command.
     *
     * @param command
     *            the command to execute
     * @param origin
     *            the origin of the command
     */
    public void command(String command, Object origin) {
        logger().debug(log -> log.debug("Received command from: " + origin));

        for (String verb : command.split("\n")) {
            logger().debug(log -> log.debug("Executing command: " + verb));
            if (verb.startsWith("#")) {
                continue;
            }

            if (COMMAND_STOP.equals(verb)) {
                logger().info(log -> log.info("Received the stop command from: " + origin));
                kill();
            } else {
                logger().warn(log -> log.warn("Unknown command: " + verb));
            }
        }
    }

    private void deleteCommandLinkFile() throws IOException {
        control.execute(context -> Files.deleteIfExists(context.path(InstanceLayout.COMMAND_LINK_FILE)));
    }

    private void storeCommandLinkFile(CommandLink link) throws IOException {
        control.execute(context -> link.save(context.path(InstanceLayout.COMMAND_LINK_FILE)));
    }

    private CommandServer commandServer(CommandLink link) throws GeneralSecurityException, IOException {
        final CryptoProtection protection = new CryptoProtection(link.secret());
        logger().info(log -> log.info("Using command link: " + link.address()));

        // @formatter:off
        return CommandServer.configure(this::command)
                .onError(e -> logger().error("Command link dropped unexpectedly.", e))
                .onClose(server -> logger().debug("Command link closed."))
                .withDecoder(protection::decrypt)
                .open(link.address());
        // @formatter:on
    }

    private boolean launchRuntime() throws ExecutionException, InterruptedException {
        return runtime.launch(that -> notifyLaunch());
    }

    private void notifyLaunch() {
        final Consumer<? super InstanceRuntime> callback = onLaunch;

        if (callback != null) {
            callback.accept(this);
        }
    }

    private static Map<String, String> frameworkProperties(InstanceControl instanceControl, Map<String, String> frameworkProperties) {
        final Map<String, String> result = new HashMap<>(frameworkProperties);

        final Path instanceRoot = FileHandling.absolutePath(instanceControl.location());
        final Map<String, String> updates = new HashMap<>(2);
        updates.put(PROPERTY_INSTANCE_ROOT, instanceRoot.toString());
        updates.put(PROPERTY_INSTANCE_CONF, instanceRoot.resolve(InstanceLayout.CONF_PATH).toString());

        final Function<String, String> placeholders = updates::get;
        result.replaceAll((name, value) -> Interpolation.interpolate(value, placeholders));
        result.putAll(updates);

        result.putIfAbsent(Constants.FRAMEWORK_STORAGE, instanceRoot.resolve(InstanceLayout.DATA_PATH).toString());
        return result;
    }
}
