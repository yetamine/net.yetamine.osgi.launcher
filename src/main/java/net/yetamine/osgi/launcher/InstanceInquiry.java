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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.Optional;

import net.yetamine.osgi.launcher.remoting.CommandLink;

/**
 * Provides means for inspecting a deployment instance.
 */
public class InstanceInquiry {

    private final Path location;

    /**
     * Creates a new instance.
     *
     * @param givenLocation
     *            the location of the location. It must not be {@code null}.
     */
    public InstanceInquiry(Path givenLocation) {
        location = givenLocation.normalize();
    }

    /**
     * Tests the path if it could be an instance.
     *
     * <p>
     * The implementation currently tests the existence of <i>etc/</i>
     * subdirectory, which is always created to store the properties.
     *
     * @param path
     *            the path to test. It must not be {@code null}.
     *
     * @return {@code true} if the instance seems valid
     */
    public static boolean seemsValid(Path path) {
        return Files.isDirectory(path.resolve(InstanceLayout.ETC_PATH));
    }

    /**
     * @return {@code true} if the instance seems valid
     *
     * @see #seemsValid(Path)
     */
    public final boolean seemsValid() {
        return seemsValid(location);
    }

    /**
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "InstanceInquiry[location=" + location() + ']';
    }

    /**
     * Resolves a path relative to {@link #location()}.
     *
     * @param other
     *            the other path. It must not be {@code null}.
     *
     * @return the resolved path
     *
     * @see Path#resolve(String)
     */
    public final Path path(String other) {
        return location.resolve(other);
    }

    /**
     * Resolves a path relative to {@link #location()}.
     *
     * @param other
     *            the other path. It must not be {@code null}.
     *
     * @return the resolved path
     *
     * @see Path#resolve(Path)
     */
    public final Path path(Path other) {
        return location.resolve(other);
    }

    /**
     * @return the instance's location
     */
    public final Path location() {
        return location;
    }

    /**
     * Attempts to load and parse the command link file.
     *
     * @return the link if the file could be read
     *
     * @throws IOException
     *             if the file exists, but reading it failed
     */
    public final Optional<CommandLink> commandLink() throws IOException {
        try { // Rely on the exceptions to avoid race conditions
            final Path linkPath = path(InstanceLayout.COMMAND_LINK_FILE);
            return Optional.of(CommandLink.load(linkPath));
        } catch (FileNotFoundException | NoSuchFileException e) {
            return Optional.empty();
        }
    }
}
