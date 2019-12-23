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

package net.yetamine.osgi.launcher.remoting;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Encapsulates the link parameters.
 */
public final class CommandLink {

    private static final int ARGUMENTS_COUNT = 3;

    private final String host;
    private final int port;
    private final String secret;

    /**
     * Creates a new instance.
     *
     * @param address
     *            the address to use. It must not be {@code null}.
     * @param givenSecret
     *            the secret to use. It must not be {@code null} and it may not
     *            contain characters outside 0x20-0x80 range, but it may be
     *            empty to generate some.
     */
    public CommandLink(InetSocketAddress address, String givenSecret) {
        secret = secretFrom(givenSecret);
        host = address.getHostString();
        port = address.getPort();
    }

    /**
     * Creates a new instance with a generated secret.
     *
     * @param address
     *            the address to use. It must not be {@code null}.
     */
    public CommandLink(InetSocketAddress address) {
        this(address.getHostString(), address.getPort(), "");
    }

    /**
     * Creates a new instance.
     *
     * @param givenHost
     *            the host to use. It must not be {@code null}.
     * @param givenPort
     *            the port to use. It must be a valid port.
     * @param givenSecret
     *            the secret to use. It must not be {@code null} and it may not
     *            contain characters outside 0x20-0x80 range, but it may be
     *            empty to generate some.
     */
    public CommandLink(String givenHost, int givenPort, String givenSecret) {
        this(InetSocketAddress.createUnresolved(givenHost, givenPort), givenSecret); // Handles the check
    }

    /**
     * Parses the argument list in the form {@code HOST PORT SECRET}.
     *
     * @param args
     *            the arguments to parse. It must not be {@code null}.
     *
     * @return the parsed arguments
     */
    public static CommandLink from(List<String> args) {
        if (args.size() < ARGUMENTS_COUNT) {
            throw new IllegalArgumentException("Requiring host, port and secret for the command link.");
        }

        final String host = args.get(0);
        final String port = args.get(1);
        final String secret = args.get(2);
        return new CommandLink(host, Integer.parseUnsignedInt(port), secret);
    }

    /**
     * Loads the parameters from the given file.
     *
     * @param path
     *            the path to the file. It must not be {@code null}.
     *
     * @return the parameters
     *
     * @throws IllegalArgumentException
     *             if the file does not provide legal values
     * @throws IOException
     *             if could not load the file
     */
    public static CommandLink load(Path path) throws IOException {
        try (Stream<String> lines = Files.lines(path)) {
            return from(lines.limit(ARGUMENTS_COUNT).collect(Collectors.toList()));
        }
    }

    /**
     * Saves the parameters to the given file.
     *
     * @param path
     *            the path of the file to save the parameters to. It must not be
     *            {@code null}.
     *
     * @throws IOException
     *             if the operation failed
     */
    public void save(Path path) throws IOException {
        Files.write(path, Arrays.asList(host, Integer.toString(port), secret));
    }

    /**
     * @return the secret
     */
    public String secret() {
        return secret;
    }

    /**
     * @return the host
     */
    public String host() {
        return host;
    }

    /**
     * @return the port
     */
    public int port() {
        return port;
    }

    /**
     * @return the address constructed from the host and port, which should be
     *         resolved if possible
     */
    public InetSocketAddress address() {
        return new InetSocketAddress(host, port);
    }

    /**
     * Returns an instance with the same secret, but updated address.
     *
     * @param value
     *            the address to update. It must not be {@code null}.
     *
     * @return the new instance
     */
    public CommandLink address(InetSocketAddress value) {
        return new CommandLink(value, secret);
    }

    private static String secretFrom(String secret) {
        if (secret.isEmpty()) {
            return UUID.randomUUID().toString();
        }

        if (secret.chars().anyMatch(CommandLink::invalidSecretCharacter)) {
            throw new IllegalArgumentException("Secret contains forbidden characters.");
        }

        return secret;
    }

    private static boolean invalidSecretCharacter(int c) {
        return ((c < 0x20) || (c > 0x80));
    }
}
