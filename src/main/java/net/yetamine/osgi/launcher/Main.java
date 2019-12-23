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

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import net.yetamine.osgi.launcher.logging.DefaultLogger;
import net.yetamine.osgi.launcher.logging.Logger;

/**
 * Provides the application command interface.
 *
 * <p>
 * Although the implemented commands are meant for single-threaded execution and
 * any attempt to run multiple excluding commands concurrently results in an
 * {@link IllegalStateException}, the implementation is thread-safe, so that
 * interrupting a command being executed from a different thread is possible.
 *
 * <p>
 * The {@link #main(String...)} method assumes the overall control of the JVM
 * instance, so it registers a shutdown hook and terminates the JVM on finish
 * with the resulting exit code. However, this is not the case of an instance.
 */
public final class Main extends Command {

    /** Exit code for successful execution. */
    public static final int EXIT_SUCCESS = 0;
    /** Exit code for an unexpected runtime error. */
    public static final int EXIT_RUNTIME = 1;
    /** Exit code for wrong argument syntax. */
    public static final int EXIT_SYNTAX = 2;
    /** Exit code for inconsistent configuration. */
    public static final int EXIT_CONFIG = 3;
    /** Exit code for a fatal failure or error. */
    public static final int EXIT_FAILURE = 4;

    /** Name of the resource, a UTF-8 text file, with the help. */
    private static final String RESOURCE_HELP = "help.txt";

    private final BiConsumer<? super String, ? super String> updateSystemProperty;

    /**
     * Creates a new instance.
     *
     * @param systemPropertyUpdater
     *            the callback for updating a system property. It must not be
     *            {@code null}.
     */
    public Main(BiConsumer<? super String, ? super String> systemPropertyUpdater) {
        updateSystemProperty = Objects.requireNonNull(systemPropertyUpdater);
    }

    /**
     * Runs the application and terminates the JVM when finishes.
     *
     * @param args
     *            the arguments. It must not be {@code null}.
     */
    public static void main(String... args) {
        Objects.requireNonNull(args);

        int result;
        final Main main = new Main(Main::updateSystemProperty);
        final String loggerOutput = DefaultLogger.systemOutput();
        try (LoggerStream loggerStream = LoggerStream.from(loggerOutput)) {
            main.logger(new DefaultLogger(loggerStream.get()).level(DefaultLogger.systemLevel()));
            registerShutdownHook(main::cancel);
            result = main.run(args);
        } catch (IOException e) {
            System.err.println("[FATAL] Failed to open the log file: " + loggerOutput);
            e.printStackTrace(System.err);
            result = EXIT_RUNTIME;
        }

        System.exit(result);
    }

    /**
     * Runs the application.
     *
     * @param args
     *            the arguments. It must not be {@code null}.
     *
     * @return the exit code
     */
    public int run(String... args) {
        return run(Arrays.asList(args));
    }

    /**
     * Runs the application.
     *
     * @param args
     *            the arguments. It must not be {@code null}.
     *
     * @return the exit code
     */
    public int run(List<String> args) {
        try {
            execute(args);
        } catch (SyntaxException e) {
            logger().error(e);
            return EXIT_SYNTAX;
        } catch (SetupException e) {
            logger().error(e);
            return EXIT_CONFIG;
        } catch (ExecutionException e) {
            logger().error(e);
            return EXIT_FAILURE;
        } catch (RuntimeException e) {
            logger().error(e);
            return EXIT_RUNTIME;
        }

        return EXIT_SUCCESS;
    }

    /**
     * @see net.yetamine.osgi.launcher.Command#withLogger(net.yetamine.osgi.launcher.logging.Logger)
     */
    @Override
    public Main withLogger(Logger logger) {
        super.withLogger(logger);
        return this;
    }

    /**
     * Displays the help from the built-in resource to the standard output.
     *
     * @throws UncheckedIOException
     *             if the resource could not be found, which should generally
     *             never happen unless the artifact is damaged
     */
    public static void help() {
        help(System.out::println);
    }

    /**
     * Provides the help from the built-in resource.
     *
     * @param output
     *            the output that receives the help text line by line. It must
     *            not be {@code null}.
     *
     * @throws UncheckedIOException
     *             if the resource could not be found, which should generally
     *             never happen unless the artifact is damaged
     */
    public static void help(Consumer<? super String> output) {
        try (BufferedReader reader = bufferedReader(resourceInputStream(RESOURCE_HELP))) {
            reader.lines().forEach(System.out::println);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * @see net.yetamine.osgi.launcher.Command#handle(java.util.List)
     */
    @Override
    protected void handle(List<String> args) throws ExecutionException {
        if (args.isEmpty()) {
            help();
            throw new SyntaxException("No arguments specified.");
        }

        final String verb = args.get(0).toLowerCase(Locale.ROOT);
        if ("help".equals(verb)) {
            help();
            return;
        }

        final Command command = command(verb).withLogger(logger());

        if (onCancel(command::cancel)) {
            throw new CancellationException("Run aborted.");
        }

        command.execute(args.subList(1, args.size()));
    }

    private Command command(String verb) {
        switch (verb) {
            case "delete":
                return new CommandDelete();

            case "deploy":
                return new CommandDeploy(updateSystemProperty);

            case "launch":
                return new CommandLaunch(updateSystemProperty);

            case "start":
                return new CommandStart(updateSystemProperty);

            case "stop":
                return new CommandStop();

            default:
                throw new SyntaxException("Unknown command encountered. Run with the help command for assistance.");
        }
    }

    private static String updateSystemProperty(String name, String value) {
        return (value != null) ? System.setProperty(name, value) : System.clearProperty(name);
    }

    private static void registerShutdownHook(Runnable command) {
        final Thread shutdownHook = new Thread(Objects.requireNonNull(command));
        shutdownHook.setName("Main Shutdown Hook");
        Runtime.getRuntime().addShutdownHook(shutdownHook);
    }

    private static InputStream resourceInputStream(String resourcePath) throws IOException {
        final InputStream result = Main.class.getResourceAsStream(resourcePath);
        if (result != null) {
            return result;
        }

        throw new IOException("Missing resource: " + resourcePath);
    }

    private static BufferedReader bufferedReader(InputStream resourceStream) {
        return new BufferedReader(new InputStreamReader(resourceStream));
    }

    /**
     * Allocates and maintains a logging stream.
     */
    private static final class LoggerStream implements Closeable {

        private final PrintStream stream;
        private final boolean closeable;

        /**
         * Creates a new instance.
         *
         * @param output
         *            the output stream to wrap. It must not be {@code null}.
         * @param closeableOutput
         *            {@code true} if {@link #close()} should close the stream
         */
        private LoggerStream(PrintStream output, boolean closeableOutput) {
            stream = Objects.requireNonNull(output);
            closeable = closeableOutput;
        }

        /**
         * Creates a new instance.
         *
         * @param output
         *            the output stream to wrap. It must not be {@code null}.
         *
         * @return the new instance
         *
         * @throws IOException
         *             if could not open the log file
         */
        public static LoggerStream from(String output) throws IOException {
            if (output.isEmpty() || "stderr".equalsIgnoreCase(output)) {
                return new LoggerStream(System.err, false);
            }

            if ("stdout".equalsIgnoreCase(output)) {
                return new LoggerStream(System.out, false);
            }

            return new LoggerStream(
                    new PrintStream(Files.newOutputStream(Paths.get(output)), true, StandardCharsets.UTF_8.name()),
                    true);
        }

        /**
         * @see java.io.Closeable#close()
         */
        @Override
        public void close() {
            if (closeable) {
                stream.close();
            }
        }

        /**
         * @return the stream
         */
        public PrintStream get() {
            return stream;
        }
    }
}
