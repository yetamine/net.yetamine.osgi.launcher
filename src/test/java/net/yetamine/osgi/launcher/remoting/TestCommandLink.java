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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.net.InetSocketAddress;
import java.nio.file.Path;
import java.util.Arrays;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Tests {@link CommandLink}
 */
public final class TestCommandLink {

    @Test
    public void construction() {
        // GIVEN
        final String secret = "TOPs3cr31!";
        final String host = "localhost";
        final int port = 4444;

        // WHEN
        final CommandLink link = new CommandLink(host, port, secret);

        // THEN
        assertEquals(host, link.host());
        assertEquals(port, link.port());
        assertEquals(secret, link.secret());
        assertEquals(new InetSocketAddress(host, port), link.address());
    }

    @Test
    public void parsing() {
        // GIVEN
        final String secret = "TOPs3cr31!";
        final String host = "localhost";
        final int port = 4444;

        // WHEN
        final CommandLink link = CommandLink.from(Arrays.asList(host, Integer.toString(port), secret));

        // THEN
        assertEquals(host, link.host());
        assertEquals(port, link.port());
        assertEquals(secret, link.secret());
        assertEquals(new InetSocketAddress(host, port), link.address());
    }

    @Test
    public void store_and_load(@TempDir Path workingDirectory) throws Exception {
        // GIVEN
        final String secret = "TOPs3cr31!";
        final String host = "localhost";
        final int port = 4444;
        final Path file = workingDirectory.resolve("instance.link");

        // WHEN
        final CommandLink original = new CommandLink(host, port, secret);
        original.save(file);
        final CommandLink restored = CommandLink.load(file);

        // THEN
        assertEquals(original.host(), restored.host());
        assertEquals(original.port(), restored.port());
        assertEquals(original.secret(), restored.secret());
        assertEquals(original.address(), restored.address());
    }

    @Test
    public void generated_secret() {
        assertFalse(new CommandLink(new InetSocketAddress("localhost", 4444), "").secret().isEmpty());
    }
}
