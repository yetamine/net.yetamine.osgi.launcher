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

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Implements a composite options parser.
 */
final class ArgumentsOptions<T> implements ArgumentsHandler<T> {

    private final Map<String, ArgumentsHandler<? super T>> parsers = new HashMap<>();

    /**
     * Creates a new instance.
     *
     * @param options
     *            the options to handle. It must not be {@code null}.
     */
    public ArgumentsOptions(Iterable<? extends ArgumentsGrouping<? super T>> options) {
        options.forEach(this::add);
    }

    /**
     * Creates a new instance.
     *
     * @param options
     *            the options to handle. It must not be {@code null}.
     */
    @SafeVarargs
    @SuppressWarnings("varargs")
    public ArgumentsOptions(ArgumentsGrouping<? super T>... options) {
        this(Arrays.asList(options));
    }

    /**
     * @see net.yetamine.osgi.launcher.ArgumentsHandler#handle(net.yetamine.osgi.launcher.Arguments,
     *      java.lang.Object)
     */
    @Override
    public void handle(Arguments args, T context) {
        while (true) {
            final String option = args.option().orElse(null);
            if (option == null) {
                break;
            }

            if ("--".equals(option)) {
                args.next();
                break;
            }

            final ArgumentsHandler<? super T> parser = parsers.get(option);
            if (parser == null) {
                throw new SyntaxException("Unknown option: " + option);
            }

            parser.handle(args.next(), context);
        }
    }

    private void add(ArgumentsGrouping<? super T> option) {
        option.names().forEach(name -> add(name, option));
    }

    private void add(String name, ArgumentsHandler<? super T> parser) {
        if (!Arguments.isOption(name)) {
            throw new IllegalArgumentException("Malformed option name: " + name);
        }

        if (parsers.putIfAbsent(name, parser) != null) {
            throw new IllegalArgumentException("Duplicate option name: " + name);
        }
    }
}
