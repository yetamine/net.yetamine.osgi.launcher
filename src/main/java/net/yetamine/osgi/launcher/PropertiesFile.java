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
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.TreeMap;

/**
 * Implements a bridge between {@link Map} and properties files.
 */
final class PropertiesFile {

    private final Path path;
    private final boolean required;

    /**
     * Creates a new instance.
     *
     * @param givenPath
     *            the path to the file. It must not be {@code null}.
     * @param givenRequired
     *            {@code true} if the file must exist for successful loading
     */
    private PropertiesFile(Path givenPath, boolean givenRequired) {
        path = Objects.requireNonNull(givenPath);
        required = givenRequired;
    }

    /**
     * Creates a new instance that rather returns an empty result than fails
     * when attempting to load a missing file.
     *
     * @param path
     *            the path to the file. It must not be {@code null}.
     *
     * @return the new instance
     */
    public static PropertiesFile optional(Path path) {
        return new PropertiesFile(path, false);
    }

    /**
     * Creates a new instance that fails when attempting to load a missing file.
     *
     * @param path
     *            the path to the file. It must not be {@code null}.
     *
     * @return the new instance
     */
    public static PropertiesFile required(Path path) {
        return new PropertiesFile(path, true);
    }

    /**
     * Saves the data to the file.
     *
     * @param data
     *            the data to store. It must not be {@code null}.
     * @param file
     *            the file to store the data into. It must not be {@code null}.
     *
     * @throws IOException
     *             if the operation failed
     */
    public static void save(Map<String, String> data, Path file) throws IOException {
        Objects.requireNonNull(data);

        try (OutputStream os = Files.newOutputStream(file)) {
            final Properties properties = new Properties();
            data.forEach(properties::put);
            properties.store(os, null);
        }
    }

    /**
     * @return {@code true} if the file exists
     */
    public boolean exists() {
        return Files.exists(path);
    }

    /**
     * @return the path to the file
     */
    public Path path() {
        return path;
    }

    /**
     * Loads the file and returns the data.
     *
     * <p>
     * If the instance is {@link #optional(Path)} and the file does not exist,
     * the operation does not fail and rather returns an empty map. Note that
     * the map type is not specified and an immutable instance may be returned.
     *
     * @return the data from the file
     *
     * @throws IOException
     *             if the operation failed
     */
    public Map<String, String> load() throws IOException {
        return mergeTo(new TreeMap<>());
    }

    /**
     * Saves the data to the file.
     *
     * @param data
     *            the data to store. It must not be {@code null}.
     *
     * @throws IOException
     *             if the operation failed
     */
    public void save(Map<String, String> data) throws IOException {
        save(data, path);
    }

    /**
     * Loads the data from the file and updates entries of the given map with
     * the entries from the file.
     *
     * <p>
     * If the instance is {@link #optional(Path)} and the file does not exist,
     * the operation performs no change. Otherwise the file must exist, or an
     * exception is thrown.
     *
     * @param <M>
     *            the type of the map
     * @param result
     *            the map to update. It must be a mutable instance.
     *
     * @return the given map
     *
     * @throws IOException
     *             if the operation failed
     */
    public <M extends Map<? super String, ? super String>> M mergeTo(M result) throws IOException {
        Objects.requireNonNull(result);

        try (InputStream is = Files.newInputStream(path)) {
            final Properties properties = new Properties();
            properties.load(is);
            properties.forEach((k, v) -> result.put(k.toString(), v.toString()));
        } catch (FileNotFoundException | NoSuchFileException e) {
            if (required) {
                throw e;
            }
        }

        return result;
    }
}
