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

import java.util.Arrays;
import java.util.Collection;
import java.util.function.Function;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Tests {@link Interpolation}.
 */
public final class TestInterpolation {

    @ParameterizedTest
    @MethodSource("testParameters")
    public void test(String template, Function<? super String, String> placeholders, String result) {
        assertEquals(result, Interpolation.interpolate(template, placeholders));
    }

    public static Collection<Arguments> testParameters() {
        final Function<String, String> placeholders = name -> "test".equals(name) ? "VALUE" : null;

        // @formatter:off
        return Arrays.asList(
            Arguments.of("", placeholders, ""),

            Arguments.of("${missing}", placeholders, "${missing}"),
            Arguments.of("${trailing}---", placeholders, "${trailing}---"),
            Arguments.of("---${trailing}", placeholders, "---${trailing}"),

            Arguments.of("${test}", placeholders, "VALUE"),
            Arguments.of("trailing: ${test}", placeholders, "trailing: VALUE"),
            Arguments.of("${test}: trailing", placeholders, "VALUE: trailing"),

            Arguments.of("${test}:${test}", placeholders, "VALUE:VALUE")
        );
        // @formatter:on
    }
}
