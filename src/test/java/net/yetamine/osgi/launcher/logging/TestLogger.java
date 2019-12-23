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

package net.yetamine.osgi.launcher.logging;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.StringReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

/**
 * Tests {@link DefaultLogger} implementation.
 */
public final class TestLogger {

    @Test
    public void thresholds() throws Exception {
        // GIVEN
        final Charset charset = StandardCharsets.UTF_8;
        final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        final PrintStream printer = new PrintStream(buffer, true, charset.name());
        final DefaultLogger logger = new DefaultLogger(printer).level(DefaultLogger.Level.INFO);

        // WHEN
        logger.error("This is an error.");
        logger.warn("This is a warning.");
        logger.info("This is an info.");
        logger.debug("This is debug is hidden.");
        logger.level(DefaultLogger.Level.DEBUG);
        logger.debug("This debug is shown.");

        // THEN
        // @formatter:off
        final List<String> expected = Arrays.asList(
                "[ERROR] This is an error.",
                "[WARN ] This is a warning.",
                "[INFO ] This is an info.",
                "[DEBUG] This debug is shown."
            );
        // @formatter:on

        assertEquals(expected, lines(buffer.toByteArray(), charset));
    }

    @Test
    public void stacktrace() throws Exception {
        // GIVEN
        final Charset charset = StandardCharsets.UTF_8;
        final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        final PrintStream printer = new PrintStream(buffer, true, charset.name());
        final DefaultLogger logger = new DefaultLogger(printer).level(DefaultLogger.Level.INFO).stackTrace(true);

        // WHEN
        logger.error("This is an error.", new IOException("Test a failure."));

        // THEN
        // @formatter:off
        final List<String> expected = Arrays.asList(
                "[ERROR] This is an error.",
                "java.io.IOException: Test a failure."
            );
        // @formatter:on

        final List<String> actual = lines(buffer.toByteArray(), charset);

        // Take just the two first lines since the rest should be a stack trace that depends
        assertEquals(expected, actual.subList(0, 2));
        assertTrue(actual.size() > 2);
    }

    @Test
    public void missing_arguments() throws Exception {
        // GIVEN
        final Charset charset = StandardCharsets.UTF_8;
        final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        final PrintStream printer = new PrintStream(buffer, true, charset.name());
        final DefaultLogger logger = new DefaultLogger(printer).level(DefaultLogger.Level.INFO);

        // WHEN
        logger.error((String) null);

        // THEN
        // @formatter:off
        final List<String> expected = Arrays.asList(
                "[BUG!?] Invalid arguments for logging: no message source provided.",
                "java.lang.IllegalArgumentException"
            );
        // @formatter:on

        final List<String> actual = lines(buffer.toByteArray(), charset);

        // Take just the two first lines since the rest should be a stack trace that depends
        assertEquals(expected, actual.subList(0, 2));
        assertTrue(actual.size() > 2);
    }

    @Test
    public void missing_message() throws Exception {
        // GIVEN
        final Charset charset = StandardCharsets.UTF_8;
        final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        final PrintStream printer = new PrintStream(buffer, true, charset.name());
        final DefaultLogger logger = new DefaultLogger(printer).level(DefaultLogger.Level.DEBUG);

        // WHEN
        logger.error((String) null, new IllegalArgumentException());

        // THEN
        // @formatter:off
        final List<String> expected = Arrays.asList(
                "[ERROR] Exception occurred. See the stack trace for details.",
                "java.lang.IllegalArgumentException"
            );
        // @formatter:on

        final List<String> actual = lines(buffer.toByteArray(), charset);

        // Take just the two first lines since the rest should be a stack trace that depends
        assertEquals(expected, actual.subList(0, 2));
        assertTrue(actual.size() > 2);
    }

    private static List<String> lines(byte[] buffer, Charset charset) {
        return new BufferedReader(new StringReader(new String(buffer, charset))).lines().collect(Collectors.toList());
    }
}
