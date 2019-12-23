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

import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.Objects;

/**
 * Creates {@link PathMatcher} instances to work within the given path context.
 */
@FunctionalInterface
public interface PathMatcherFactory {

    /**
     * Creates a {@link PathMatcher} instance.
     *
     * @param path
     *            the path in whose context the resulting matcher shall be used.
     *            It must not be {@code null}.
     * @param pattern
     *            the pattern to use. It must not be {@code null}.
     *
     * @return the matcher
     */
    PathMatcher create(Path path, String pattern);

    /**
     * Returns a factory that uses {@link FileSystem#getPathMatcher(String)}
     * with the given syntax (the commonly supported are <i>glob</i> and
     * <i>regex</i>).
     *
     * @param syntax
     *            the syntax. It must not be {@code null}.
     *
     * @return the factory
     */
    static PathMatcherFactory withSyntax(String syntax) {
        Objects.requireNonNull(syntax);

        return (path, pattern) -> {
            try {
                return path.getFileSystem().getPathMatcher(syntax + ':' + Objects.requireNonNull(pattern));
            } catch (IllegalArgumentException e) {
                // Alas, the exception carries almost no details, so let's wrap it to add more
                throw new IllegalArgumentException("Invalid path filter: " + pattern, e);
            }
        };
    }

    /**
     * @return the glob-based matcher
     */
    static PathMatcherFactory glob() {
        return withSyntax("glob");
    }

    /**
     * @return the regex-based matcher
     */
    static PathMatcherFactory regex() {
        return withSyntax("regex");
    }
}
