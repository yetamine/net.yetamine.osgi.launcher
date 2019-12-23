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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import net.yetamine.osgi.launcher.logging.DefaultLogger;
import net.yetamine.osgi.launcher.logging.Logger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Tests {@link Main}.
 */
public final class TestMain {

    private static final Logger LOGGER = new DefaultLogger(System.err).level(Logger.Level.DEBUG);

    /**
     * Pause before shutting the framework.
     *
     * <p>
     * The high-level interface does not provide a fine-grained feedback, so
     * that a pause must be used instead of a callback reaction, which would
     * provide more certainty about the state.
     */
    private static final Duration PAUSE = Duration.ofMillis(1000);

    @Test
    public void standalone(@TempDir Path workingDirectory) throws Exception {
        final Path source = workingDirectory.resolve("source");
        final Path target = workingDirectory.resolve("target");
        runDeploy(source, target);
        runStartAndStop(target);
        runDelete(target);
    }

    @Test
    public void launch(@TempDir Path workingDirectory) throws Exception {
        // GIVEN
        final Path source = workingDirectory.resolve("source");
        final Path target = workingDirectory.resolve("target");

        Files.createDirectories(source.resolve("bundles/testing"));
        Files.createDirectories(source.resolve("init"));
        Files.createDirectories(source.resolve("conf"));

        final Path bundlePath = source.resolve("bundles/testing/testing-1.0.0.jar").toAbsolutePath();
        final URI filesystemUri = URI.create("jar:" + bundlePath.toUri().toASCIIString());
        final Map<String, Object> filesystemEnvironment = new HashMap<>();
        filesystemEnvironment.put("create", "true");

        // @formatter:off
        try (FileSystem fs = FileSystems.newFileSystem(filesystemUri, filesystemEnvironment)) {
            final Path metaInf = Files.createDirectories(fs.getPath("META-INF"));
            Files.write(metaInf.resolve("MANIFEST.MF"), Arrays.asList(
                    "Manifest-Version: 1.0",
                    "Bundle-SymbolicName: net.yetamine.osgi.launcher.testing",
                    "Bundle-Version: 1.0.0"
            ));
        }

        Files.write(source.resolve("framework.properties"), Arrays.asList(
                "org.osgi.framework.startlevel.beginning=100"
            ));

        Files.write(source.resolve("launching.properties"), Arrays.asList(
                "shutdown.timeout=5s"
            ));

        Files.write(source.resolve("init/testing.properties"), Arrays.asList(
                "# Dummy file to have something in init/"
            ));

        Files.write(source.resolve("conf/updated.properties"), Arrays.asList(
                "# Dummy file to have something in conf/"
            ));

        // WHEN
        final CompletableFuture<Integer> running = CompletableFuture.supplyAsync(() -> main(
                "launch",
                "--clean-instance",
                "--command-address", "localhost", "0",
                "--create-configuration", source.resolve("init").toString(),
                "--update-configuration", source.resolve("conf").toString(),
                "--framework-properties", source.resolve("framework.properties").toString(),
                "--launching-properties", source.resolve("launching.properties").toString(),
                "--bundle-store", source.resolve("bundles").toString(),
                target.toString()
            ));
        // @formatter:on

        Thread.sleep(PAUSE.toMillis());

        // THEN
        assertTrue(Files.isRegularFile(target.resolve("etc/framework.properties")));
        assertTrue(Files.isRegularFile(target.resolve("etc/launching.properties")));
        assertTrue(Files.isRegularFile(target.resolve("etc/system.properties")));
        assertTrue(Files.isRegularFile(target.resolve("conf/testing.properties")));
        assertTrue(Files.isRegularFile(target.resolve("conf/updated.properties")));

        final int stopExitCode = main("stop", target.toString());
        final int launchExitCode = running.get(10, TimeUnit.SECONDS);
        assertEquals(Main.EXIT_SUCCESS, launchExitCode);
        assertEquals(Main.EXIT_SUCCESS, stopExitCode);
    }

    @Test
    public void cancellation(@TempDir Path workingDirectory) throws Exception {
        // GIVEN
        final Path source = workingDirectory.resolve("source");
        final Path target = workingDirectory.resolve("target");
        runDeploy(source, target);

        // WHEN
        final Main main = main();
        final CompletableFuture<Integer> running = CompletableFuture.supplyAsync(() -> {
            return main.run("start", target.toString());
        });

        Thread.sleep(PAUSE.toMillis());
        main.cancel(); // Emulate Ctrl+C for the command line

        // THEN
        assertEquals(Main.EXIT_SUCCESS, running.get(10, TimeUnit.SECONDS));
    }

    private void runStartAndStop(Path instance) throws Exception {
        final CompletableFuture<Integer> running = CompletableFuture.supplyAsync(() -> {
            return main("start", "--command-address", "localhost", "0", instance.toString());
        });

        Thread.sleep(PAUSE.toMillis());

        final int stopExitCode = main("stop", instance.toString());
        final int startExitCode = running.get(10, TimeUnit.SECONDS);
        assertEquals(Main.EXIT_SUCCESS, startExitCode);
        assertEquals(Main.EXIT_SUCCESS, stopExitCode);
    }

    private void runDeploy(Path source, Path target) throws Exception {
        // GIVEN
        Files.createDirectories(source.resolve("bundles"));

        // @formatter:off
        Files.write(source.resolve("framework.properties"), Arrays.asList(
                "org.osgi.framework.startlevel.beginning=100"
            ));

        Files.write(source.resolve("launching.properties"), Arrays.asList(
                "shutdown.timeout=5s"
            ));

        // WHEN
        final int exitCode = main(
              "deploy",
              "--clean-instance",
              "--framework-properties", source.resolve("framework.properties").toString(),
              "--launching-properties", source.resolve("launching.properties").toString(),
              "--bundle-store", source.resolve("bundles").toString(),
              target.toString()
           );

        // THEN
        assertTrue(Files.isRegularFile(target.resolve("etc/framework.properties")));
        assertTrue(Files.isRegularFile(target.resolve("etc/launching.properties")));
        assertTrue(Files.isRegularFile(target.resolve("etc/system.properties")));
        assertTrue(Files.isDirectory(target.resolve("conf")));
        assertEquals(Main.EXIT_SUCCESS, exitCode);
    }

    private void runDelete(Path instance) {
        // WHEN
        final int deleteExitCode = main("delete", instance.toString());

        // THEN
        assertEquals(Main.EXIT_SUCCESS, deleteExitCode);
        assertFalse(Files.exists(instance));
    }

    private static int main(String... args) {
        return main().run(args);
    }

    private static Main main() {
        return new Main((name, value) -> {}).withLogger(LOGGER);
    }
}
