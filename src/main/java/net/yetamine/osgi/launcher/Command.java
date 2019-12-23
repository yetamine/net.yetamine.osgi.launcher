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

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;

import net.yetamine.osgi.launcher.logging.Logger;

/**
 * Encapsulates a command with a parameter-oriented interface.
 *
 * <p>
 * The command is a one-shot action: it may be invoked at most once. A command
 * can be eventually cancelled, which either prevents it from starting when it
 * is triggered, or the command implementation may attempt to terminate as soon
 * as possible when the command is already being executed.
 */
public abstract class Command extends LoggerSupporter implements Executable<List<String>, ExecutionException> {

    private final Object lock = new Object();
    private Runnable onCancel;
    private boolean cancelled;
    private boolean completed;
    private boolean running;

    /**
     * Creates a new instance.
     */
    protected Command() {
        // Default constructor
    }

    /**
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return getClass().getName();
    }

    /**
     * Executes the command.
     *
     * @param args
     *            the command arguments. It must not be {@code null}.
     *
     * @throws CancellationException
     *             if this instance is in the cancelled command state
     * @throws SetupException
     *             if the given arguments lead to an invalid configuration
     * @throws SyntaxException
     *             if the given arguments are syntactically invalid
     * @throws ExecutionException
     *             if the command execution failed
     *
     * @see net.yetamine.osgi.launcher.Executable#execute(java.lang.Object)
     */
    @Override
    public final void execute(List<String> args) throws ExecutionException {
        Objects.requireNonNull(args);

        synchronized (lock) {
            if (completed || running) {
                throw new IllegalStateException("Command was invoked already.");
            }
            if (cancelled) {
                throw new CancellationException("Command was cancelled.");
            }

            running = true;
        }

        try {
            handle(args);
        } finally {
            synchronized (lock) {
                running = false;
                onCancel = null;
            }
        }
    }

    /**
     * Requests cancelling the command.
     */
    public final void cancel() {
        synchronized (lock) {
            final Runnable action = onCancel;
            // Prevent repeated invocations!
            onCancel = null;

            cancelled = true;

            if (action != null) {
                action.run();
            }
        }
    }

    /**
     * @return {@code true} if the command was cancelled
     */
    public final boolean cancelled() {
        synchronized (lock) {
            return cancelled;
        }
    }

    /**
     * @return {@code true} if the command was executed
     */
    public final boolean completed() {
        synchronized (lock) {
            return completed;
        }
    }

    /**
     * @return {@code true} if the command is being executed
     */
    public final boolean running() {
        synchronized (lock) {
            return running;
        }
    }

    /**
     * Sets the logger to use.
     *
     * @param logger
     *            the logger
     *
     * @return this instance
     */
    public Command withLogger(Logger logger) {
        logger(logger);
        return this;
    }

    /**
     * Executes the command.
     *
     * <p>
     * This method is the actual command workhorse that is meant for overriding.
     * It is invoked by {@link #execute(List)} after the method ensures that the
     * command has not been invoked nor cancelled and updates the command state
     * accordingly.
     *
     * @param args
     *            the command arguments. It must not be {@code null}.
     *
     * @throws SetupException
     *             if the given arguments lead to an invalid configuration
     * @throws SyntaxException
     *             if the given arguments are syntactically invalid
     * @throws ExecutionException
     *             if the command execution failed
     */
    protected abstract void handle(List<String> args) throws ExecutionException;

    /**
     * Sets the cancellation handler that {@link #cancel()} shall delegate to.
     *
     * @param action
     *            the action to run. If {@code null}, no action shall be run.
     *
     * @return {@code true} if the cancellation occurred already and thus the
     *         previous handler was executed and disposed, {@code false} when
     *         the handler was updated
     */
    protected final boolean onCancel(Runnable action) {
        synchronized (lock) {
            if (cancelled) {
                return true;
            }

            onCancel = (action == null) ? null : () -> invokeCancellationHandler(action);
            return false;
        }
    }

    private void invokeCancellationHandler(Runnable action) {
        assert (action != null);

        try {
            logger().debug(logger -> logger.debug("Cancellation handler starting: " + this));
            action.run();
            logger().debug(logger -> logger.debug("Cancellation handler finished: " + this));
        } catch (Throwable t) {
            logger().warn(logger -> logger.warn("Cancellation handler finished: " + this));
        }
    }
}
