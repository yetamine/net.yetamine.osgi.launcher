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
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Lists paths depending on encapsulated criteria.
 */
@FunctionalInterface
public interface PathLister {

    /**
     * Returns the paths.
     *
     * <p>
     * The returned stream must be consumed or closed as it may employ various
     * resources. Note that the result may be different on each invocation
     * depending on the file system changes.
     *
     * @return the path stream
     *
     * @throws IOException
     *             if failed to oped the stream
     */
    Stream<Path> paths() throws IOException;

    /**
     * Resolves {@link #paths()} into a stable list.
     *
     * @return the list of {@link #paths()} results
     *
     * @throws IOException
     *             if the operation failed
     */
    default List<Path> resolve() throws IOException {
        return paths().collect(Collectors.toList());
    }
}
