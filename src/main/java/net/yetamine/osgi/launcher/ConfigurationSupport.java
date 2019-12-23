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

import java.nio.file.Paths;
import java.util.List;
import java.util.Objects;

/**
 * Implements support for configuration processing.
 */
final class ConfigurationSupport {

    private ConfigurationSupport() {
        throw new AssertionError();
    }

    /**
     * Parses the configuration with the given option set.
     *
     * <p>
     * This method throws whatever exception the options may raise during the
     * process.
     *
     * @param args
     *            the arguments to parse. It must not be {@code null}.
     * @param supportedOptions
     *            the options to use. It must not be {@code null}.
     * @param result
     *            the configuration to fill. It must not be {@code null}.
     *
     * @return the configuration
     */
    public static Configuration parse(List<String> args, Iterable<? extends ArgumentsGrouping<? super Configuration>> supportedOptions, Configuration result) {
        Objects.requireNonNull(result);
        final Arguments arguments = new Arguments(args);
        // All arguments are allowed here, so just group then and parse that all
        new ArgumentsOptions<>(supportedOptions).handle(arguments, result);
        result.instance(Paths.get(arguments.requireString("INSTANCE")));
        result.parameters().addAll(arguments.next().remaining());

        return result;
    }

    /**
     * Parses the configuration with the given option set.
     *
     * <p>
     * This method throws whatever exception the options may raise during the
     * process.
     *
     * @param args
     *            the arguments to parse. It must not be {@code null}.
     * @param supportedOptions
     *            the options to use. It must not be {@code null}.
     *
     * @return the validated configuration
     */
    public static Configuration parse(List<String> args, Iterable<? extends ArgumentsGrouping<? super Configuration>> supportedOptions) {
        return parse(args, supportedOptions, new Configuration()).validate();
    }
}
