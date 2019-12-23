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

import java.util.Objects;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Implements a simple placeholder interpolation.
 */
final class Interpolation {

    private static final Pattern PLACEHOLDER = Pattern.compile("\\$\\{(?<placeholder>[^}]*)\\}");

    private Interpolation() {
        throw new AssertionError();
    }

    /**
     * Replaces the placeholders found in the given template.
     *
     * @param template
     *            the template that may contain placeholders in the form
     *            <i>${name}</i> where <i>name</i> is resolved using the
     *            placeholders. It must not be {@code null}.
     * @param placeholders
     *            the source of placeholder values. It must not be {@code null}.
     *
     * @return the resolved template
     */
    public static String interpolate(String template, Function<? super String, String> placeholders) {
        Objects.requireNonNull(placeholders);

        final Matcher matcher = PLACEHOLDER.matcher(template);
        if (!matcher.find()) {
            return template;
        }

        final StringBuilder result = new StringBuilder();
        int copyFrom = 0;

        do {
            final String placeholder = matcher.group("placeholder");
            final String value = placeholders.apply(placeholder);

            if (value != null) {
                result.append(template.substring(copyFrom, matcher.start()));
                result.append(value);
                copyFrom = matcher.end();
            }
        } while (matcher.find());

        // Append trailing part
        result.append(template.substring(copyFrom));
        return result.toString();
    }
}
