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

import net.yetamine.osgi.launcher.locking.LockFileException;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;

/**
 * Implements the start command.
 */
public final class CommandStart extends SystemAwareCommand {

    /**
     * Creates a new instance.
     *
     * @param systemPropertiesUpdate
     *            the handler to take care of updating configured system
     *            properties. It must not be {@code null}.
     */
    public CommandStart(BiConsumer<? super String, ? super String> systemPropertiesUpdate) {
        super(systemPropertiesUpdate);
    }

    /**
     * @see net.yetamine.osgi.launcher.Command#handle(java.util.List)
     */
    @Override
    protected void handle(List<String> args) throws ExecutionException {
        handle(configuration(args));
    }

    private void handle(Configuration configuration) throws ExecutionException {
        assert !configuration.skipStart(); // This would make no sense and therefore should not happen

        updateSystemProperties(configuration.systemProperties());

        logger().info(logger -> logger.info("Starting instance: " + configuration.instance()));
        try (InstanceControl control = new InstanceControl(configuration.instance())) {

            // @formatter:off
            final InstanceRuntime runtime = InstanceRuntime
                    .create(control, FrameworkRuntime.frameworkFactory(), configuration.frameworkProperties())
                    .withLogger(logger())
                    .shutdownTimeout(configuration.shutdownTimeout().orElse(null))
                    .executeOnFramework(framework -> FrameworkSystemServices.register(framework, configuration));
            // @formatter:on

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

    private Configuration configuration(List<String> args) {
        final Configuration result = ConfigurationSupport.parse(args, supportedOptions());
        logger().info("Restored instance properties.");
        InstanceSupport.restore(result);
        // Prevent cleaning the storage area!
        result.frameworkProperties().remove(Constants.FRAMEWORK_STORAGE_CLEAN);
        return result;
    }

    private static Set<ConfigurationOption> supportedOptions() {
        // @formatter:off
        return EnumSet.of(
                ConfigurationOption.COMMAND_ADDRESS,
                ConfigurationOption.COMMAND_SECRET,
                ConfigurationOption.DUMP_STATUS,
                ConfigurationOption.FRAMEWORK_PROPERTIES,
                ConfigurationOption.FRAMEWORK_PROPERTY,
                ConfigurationOption.LAUNCHING_PROPERTIES,
                ConfigurationOption.LAUNCHING_PROPERTY,
                ConfigurationOption.SYSTEM_PROPERTIES,
                ConfigurationOption.SYSTEM_PROPERTY
            );
        // @formatter:on
    }
}
