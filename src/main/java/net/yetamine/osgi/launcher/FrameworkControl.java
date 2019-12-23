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

import java.time.Duration;
import java.util.concurrent.ExecutionException;
import java.util.function.Predicate;

import net.yetamine.osgi.launcher.deploying.BundleDeployment;
import net.yetamine.osgi.launcher.deploying.DeploymentUmbrella;
import org.osgi.framework.Bundle;
import org.osgi.framework.launch.Framework;

/**
 * Maintains a framework instance.
 *
 * <p>
 * Implementations of this interface are not required to be thread-safe.
 * However, it must be possible to invoke methods {@link #stop()} and
 * {@link #kill()} safely from any thread.
 */
public interface FrameworkControl {

    /**
     * @return the framework instance
     */
    Framework framework();

    /**
     * Executes an operation on the framework.
     *
     * @param <X>
     *            the exception that the operation may throw
     * @param operation
     *            the operation to execute. It must not be {@code null}.
     *
     * @return this instance
     *
     * @throws X
     *             if the operation throws it
     */
    <X extends Exception> FrameworkControl executeOnFramework(Executable<? super Framework, ? extends X> operation) throws X;

    /**
     * Executes an operation on this instance.
     *
     * @param <X>
     *            the exception that the operation may throw
     * @param operation
     *            the operation to execute. It must not be {@code null}.
     *
     * @return this instance
     *
     * @throws X
     *             if the operation throws it
     */
    <X extends Exception> FrameworkControl executeOnController(Executable<? super FrameworkControl, ? extends X> operation) throws X;

    /**
     * Invokes a function on the framework and returns its result.
     *
     * @param <R>
     *            the result of the function
     * @param <X>
     *            the exception that the function may throw
     * @param function
     *            the function to invoke. It must not be {@code null}.
     *
     * @return the result of the function
     *
     * @throws X
     *             if the function throws it
     */
    default <R, X extends Exception> R invoke(ThrowingFunction<? super Framework, ? extends R, ? extends X> function) throws X {
        return function.apply(framework());
    }

    /**
     * Executes all deployment steps described by the given deployment.
     *
     * @param deployment
     *            the deployment to apply. It must not be {@code null}.
     *
     * @return this instance
     */
    FrameworkControl deploy(DeploymentUmbrella deployment);

    /**
     * Executes all deployment steps described by the given deployment.
     *
     * @param deployment
     *            the deployment to apply. It must not be {@code null}.
     *
     * @return this instance
     */
    FrameworkControl deploy(BundleDeployment deployment);

    /**
     * Undeploys all bundles matching the filter.
     *
     * @param filter
     *            the filter for bundles to undeploy. It must not be
     *            {@code null}.
     *
     * @return this instance
     */
    FrameworkControl undeploy(Predicate<? super Bundle> filter);

    /**
     * Sets the shutdown timeout.
     *
     * @param value
     *            the timeout to set. It may be {@code null} for no timeout,
     *            otherwise it must be a positive value.
     *
     * @return this instance
     */
    FrameworkControl shutdownTimeout(Duration value);

    /**
     * Launches the framework.
     *
     * @return {@code true} if the framework stopped, {@code false} when it was
     *         killed before it could be started or restarted between updates
     *
     * @throws ExecutionException
     *             if a framework operation failed
     * @throws InterruptedException
     *             if waiting for the framework to stop was interrupted
     */
    boolean launch() throws ExecutionException, InterruptedException;

    /**
     * Stops the framework if running and waits for it to terminate.
     *
     * @return {@code true} if the framework was stopped
     *
     * @throws ExecutionException
     *             if the operation failed
     * @throws InterruptedException
     *             if the waiting was interrupted
     */
    boolean stop() throws ExecutionException, InterruptedException;

    /**
     * Terminates the framework if running and prevents {@link #launch()} from
     * starting it again.
     *
     * <p>
     * This method waits for the framework to stop if necessary. If interrupted,
     * it sets interruption state of the current thread and returns immediately.
     *
     * @return {@code true} if the framework was stopped
     */
    boolean kill();

    /**
     * @return {@code true} if the framework is running
     */
    boolean running();
}
