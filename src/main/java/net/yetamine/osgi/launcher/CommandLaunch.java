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
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.function.BiConsumer;

import net.yetamine.osgi.launcher.deploying.DeploymentUmbrella;
import net.yetamine.osgi.launcher.locking.LockFileException;
import org.osgi.framework.BundleException;

/**
 * Implements the launch command.
 */
public final class CommandLaunch extends SystemAwareCommand {

    /**
     * Creates a new instance.
     *
     * @param systemPropertiesUpdate
     *            the handler to take care of updating configured system
     *            properties. It must not be {@code null}.
     */
    public CommandLaunch(BiConsumer<? super String, ? super String> systemPropertiesUpdate) {
        super(systemPropertiesUpdate);
    }

    /**
     * @see net.yetamine.osgi.launcher.Command#handle(java.util.List)
     */
    @Override
    protected void handle(List<String> args) throws ExecutionException {
        handle(ConfigurationSupport.parse(args, supportedOptions()));
    }

    private void handle(Configuration configuration) throws ExecutionException {
        updateSystemProperties(configuration.systemProperties());

        logger().info(logger -> logger.info("Launching instance: " + configuration.instance()));
        try (InstanceControl control = new InstanceControl(configuration.instance())) {
            final DeploymentUmbrella deployment;
            final InstanceSupport support;

            if (configuration.skipDeploy()) {
                logger().info("Skipping deployment as requested.");
                deployment = null;
                support = null;
            } else {
                logger().info("Preparing deployment.");
                support = new InstanceSupport(control, configuration).withLogger(logger());
                // Prepare the deployment plan first as this only reads and does not touch the instance yet
                deployment = support.deployment();
                // Now we can really touch something and update it
                support.clean().configure().storeProperties();
            }

            // @formatter:off
            final InstanceRuntime runtime = InstanceRuntime
                    .create(control, FrameworkRuntime.frameworkFactory(), configuration.frameworkProperties())
                    .withLogger(logger())
                    .shutdownTimeout(configuration.shutdownTimeout().orElse(null))
                    .executeOnFramework(framework -> FrameworkSystemServices.register(framework, configuration));
            // @formatter:on

            if (support != null) {
                runtime.executeOnController(support::uninstall);
            }
            if (deployment != null) {
                runtime.deploy(deployment);
            }

            if (configuration.skipStart()) {
                StatusFormatter.dump(logger(), runtime, configuration);
                logger().info("Skipping start as requested.");
                runtime.kill();
                return;
            }

            if (onCancel(runtime::kill)) {
                logger().info("Start aborted.");
                return;
            }

            logger().info("Starting the framework.");

            runtime.onLaunch(context -> {
                logger().debug("Framework started.");
                context.onLaunch(null);
                StatusFormatter.dump(logger(), runtime, configuration);
            }).launch(configuration.commandAddress().orElse(null), configuration.commandSecret());
        } catch (LockFileException e) {
            throw new ExecutionException("Could not acquire the instance control, the instance is busy.", e);
        } catch (IOException e) {
            throw new SetupException(e);
        } catch (BundleException e) {
            throw new ExecutionException(e);
        } catch (IllegalArgumentException e) {
            throw new SetupException(e); // Most cases should be caused by that (e.g., path conversions)
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger().error("Execution interrupted unexpectedly.");
        }
    }

    private static Set<ConfigurationOption> supportedOptions() {
        return EnumSet.allOf(ConfigurationOption.class);
    }
}
