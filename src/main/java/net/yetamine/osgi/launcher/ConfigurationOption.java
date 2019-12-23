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
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Implements handlers for parsing arguments into the configuration.
 */
enum ConfigurationOption implements ArgumentsGrouping<Configuration> {

    BUNDLE_STORE("--bundle-store", "-B") {
        @Override
        public void handle(Arguments args, Configuration context) {
            final Path path = Paths.get(args.requireString("PATH"));
            context.bundles().add(new BundleStore(path));
            args.next();
        }
    },

    BUNDLES("--bundles", "-b") {
        @Override
        public void handle(Arguments args, Configuration context) {
            final Path path = Paths.get(args.requireString("PATH"));
            context.bundles().add(new BundleSource(path));
            args.next();
        }
    },

    CLEAN_CONFIGURATION("--clean-configuration") {
        @Override
        public void handle(Arguments args, Configuration context) {
            final Optional<Boolean> value = args.optionSwitch();
            context.cleanConfiguration(value.orElse(Boolean.TRUE));
            if (value.isPresent()) {
                args.next();
            }
        }
    },

    CLEAN_INSTANCE("--clean-instance") {
        @Override
        public void handle(Arguments args, Configuration context) {
            final Optional<Boolean> value = args.optionSwitch();
            context.cleanInstance(value.orElse(Boolean.TRUE));
            if (value.isPresent()) {
                args.next();
            }
        }
    },

    COMMAND_ADDRESS("--command-address", "-a") {
        @Override
        public void handle(Arguments args, Configuration context) {
            final String host = args.requireString("HOST");
            final int port = Integer.parseUnsignedInt(args.next().requireString("PORT"));
            context.commandAddress(new InetSocketAddress(host, port));
            args.next();
        }
    },

    COMMAND_SECRET("--command-secret", "-t") {
        @Override
        public void handle(Arguments args, Configuration context) {
            final String secret = args.requireString("SECRET");
            context.commandSecret(secret);
            args.next();
        }
    },

    CREATE_CONFIGURATION("--create-configuration", "-c") {
        @Override
        public void handle(Arguments args, Configuration context) {
            final Path dir = Paths.get(args.requireString("DIR"));
            context.createConfiguration().add(dir);
            args.next();
        }
    },

    DUMP_STATUS("--dump-status") {
        @Override
        public void handle(Arguments args, Configuration context) {
            final Optional<Boolean> value = args.optionSwitch();
            context.dumpStatus(value.orElse(Boolean.TRUE));
            if (value.isPresent()) {
                args.next();
            }
        }
    },

    FRAMEWORK_PROPERTIES("--framework-properties", "-f") {

        @Override
        public void handle(Arguments args, Configuration context) {
            mergeProperties(context.frameworkProperties(), args);
        }
    },

    FRAMEWORK_PROPERTY("--framework-property", "-F") {
        @Override
        public void handle(Arguments args, Configuration context) {
            mergeProperty(context.frameworkProperties(), args);
        }
    },

    LAUNCHING_PROPERTIES("--launching-properties", "-l") {
        @Override
        public void handle(Arguments args, Configuration context) {
            mergeProperties(context.launchingProperties(), args);
        }
    },

    LAUNCHING_PROPERTY("--launching-property", "-L") {
        @Override
        public void handle(Arguments args, Configuration context) {
            mergeProperty(context.launchingProperties(), args);
        }
    },

    SKIP_DEPLOY("--skip-deploy") {
        @Override
        public void handle(Arguments args, Configuration context) {
            final Optional<Boolean> value = args.optionSwitch();
            context.skipDeploy(value.orElse(Boolean.TRUE));
            if (value.isPresent()) {
                args.next();
            }
        }
    },

    SKIP_START("--skip-start") {
        @Override
        public void handle(Arguments args, Configuration context) {
            final Optional<Boolean> value = args.optionSwitch();
            context.skipStart(value.orElse(Boolean.TRUE));
            if (value.isPresent()) {
                args.next();
            }
        }
    },

    SYSTEM_PROPERTIES("--system-properties", "-s") {
        @Override
        public void handle(Arguments args, Configuration context) {
            mergeProperties(context.systemProperties(), args);
        }
    },

    SYSTEM_PROPERTY("--system-property", "-S") {
        @Override
        public void handle(Arguments args, Configuration context) {
            mergeProperty(context.systemProperties(), args);
        }
    },

    UNINSTALL_BUNDLES("--uninstall-bundles", "-U") {
        @Override
        public void handle(Arguments args, Configuration context) {
            context.uninstallBundles().add(args.requireString("LOCATION"));
            args.next();
        }
    },

    UPDATE_CONFIGURATION("--update-configuration", "-u") {
        @Override
        public void handle(Arguments args, Configuration context) {
            final Path dir = Paths.get(args.requireString("DIR"));
            context.updateConfiguration().add(dir);
            args.next();
        }
    };

    private final Set<String> names;

    /**
     * Prepares a new instance.
     *
     * @param givenNames
     *            the names to return by {@link #names()}. It must not be
     *            {@code null}.
     */
    private ConfigurationOption(String... givenNames) {
        names = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(givenNames)));
    }

    /**
     * @see net.yetamine.osgi.launcher.ArgumentsGrouping#names()
     */
    @Override
    public Set<String> names() {
        return names;
    }

    // Private helpers (not private though Java version below 11)

    static void mergeProperties(Map<String, String> result, Arguments args) {
        try {
            final Path file = Paths.get(args.requireString("FILE"));
            args.next(); // Move past the argument then yet!
            PropertiesFile.required(file).mergeTo(result);
        } catch (IOException e) {
            throw new SetupException(e);
        }
    }

    static void mergeProperty(Map<String, String> result, Arguments args) {
        // Keep the statement rather separated to read the values in the proper order
        final String name = args.requireString("NAME");
        final String value = args.next().requireString("VALUE");
        args.next(); // Move past the second argument then yet!
        result.put(name, value);
    }

    /**
     * Represents a single bundle source, i.e., a directory containing optional
     * deployment properties that might be provided explicitly by pointing to a
     * file.
     */
    private static final class BundleSource implements PathLister {

        private final Path path;

        /**
         * Creates a new instance.
         *
         * @param givenPath
         *            the path to encapsulate. It must not be {@code null}.
         */
        public BundleSource(Path givenPath) {
            path = givenPath.normalize();
        }

        /**
         * @see java.lang.Object#toString()
         */
        @Override
        public String toString() {
            return path.toString();
        }

        /**
         * @see net.yetamine.osgi.launcher.PathLister#paths()
         */
        @Override
        public Stream<Path> paths() throws IOException {
            return Stream.of(path);
        }
    }

    /**
     * Represents a whole set of bundle sources.
     */
    private static final class BundleStore implements PathLister {

        private final Path root;

        /**
         * Creates a new instance.
         *
         * @param givenRoot
         *            the path to encapsulate. It must not be {@code null}.
         */
        public BundleStore(Path givenRoot) {
            root = givenRoot.normalize();
        }

        /**
         * @see java.lang.Object#toString()
         */
        @Override
        public String toString() {
            return root.toString() + root.getFileSystem().getSeparator() + '*';
        }

        /**
         * @see net.yetamine.osgi.launcher.PathLister#paths()
         */
        @Override
        public Stream<Path> paths() throws IOException {
            try {
                return Files.list(root).filter(Files::isDirectory).sorted();
            } catch (IOException e) {
                throw new IOException("Could not list bundle store: " + root, e);
            }
        }
    }
}
