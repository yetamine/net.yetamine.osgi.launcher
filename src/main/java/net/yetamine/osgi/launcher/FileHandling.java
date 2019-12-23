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
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.NavigableSet;
import java.util.Objects;
import java.util.TreeSet;
import java.util.function.Predicate;

/**
 * Provides a few file-handling utilities for the needs of the commands.
 */
final class FileHandling {

    private FileHandling() {
        throw new AssertionError();
    }

    /**
     * Returns the absolute path, preferably the real path.
     *
     * @param path
     *            the path. It must not be {@code null}.
     *
     * @return the absolute path
     *
     * @see Path#toAbsolutePath()
     * @see Path#toRealPath(java.nio.file.LinkOption...)
     */
    public static Path absolutePath(Path path) {
        try {
            return path.toRealPath();
        } catch (IOException e) {
            return path.toAbsolutePath();
        }
    }

    /**
     * Copies all files matching the filter from the source to the target.
     *
     * <p>
     * The target is assumed to be a directory, which is created if missing. If
     * the source is a file, the file is copied to the target directory keeping
     * its name. If the source is a directory, its content is copied to the
     * target directory.
     *
     * @param target
     *            the target. It must not be {@code null}.
     * @param source
     *            the source. It must not be {@code null}.
     * @param filter
     *            the filter that chooses the files to copy. It must not be
     *            {@code null}.
     *
     * @throws IOException
     *             if the operation failed
     */
    public static void copyTo(Path target, Path source, Predicate<? super Path> filter) throws IOException {
        Objects.requireNonNull(filter);

        if (Files.isDirectory(source)) {
            copyDirectoryContent(source, target, filter);
        } else if (Files.exists(source)) {
            copyFileToDirectory(source, target);
        } else {
            throw new NoSuchFileException(source.toString());
        }
    }

    /**
     * Copies all files from the source to the target.
     *
     * <p>
     * The target is assumed to be a directory, which is created if missing. If
     * the source is a file, the file is copied to the target directory keeping
     * its name. If the source is a directory, its content is copied to the
     * target directory.
     *
     * @param target
     *            the target. It must not be {@code null}.
     * @param source
     *            the source. It must not be {@code null}.
     *
     * @throws IOException
     *             if the operation failed
     */
    public static void copyTo(Path target, Path source) throws IOException {
        copyTo(target, source, path -> true);
    }

    /**
     * Deletes the path and its subtree.
     *
     * @param target
     *            the path to delete. It must not be {@code null}.
     * @param filter
     *            the filter choosing what to delete. It must not be
     *            {@code null}.
     *
     * @return {@code true} if the path exists no more, {@code false} if the
     *         path could not be completely deleted because the filter chose
     *         some entries to survive
     *
     * @throws IOException
     *             if the operation failed
     */
    public static boolean delete(Path target, Predicate<? super Path> filter) throws IOException {
        Objects.requireNonNull(filter);
        return Files.notExists(target) || Files.notExists(Files.walkFileTree(target, new DeletingFileVisitor(filter)));
    }

    /**
     * Deletes the path and its subtree.
     *
     * @param target
     *            the path to delete. It must not be {@code null}.
     *
     * @throws IOException
     *             if the operation failed
     */
    public static void delete(Path target) throws IOException {
        if (!delete(target, path -> true)) {
            throw new IOException("Could not delete: " + target);
        }
    }

    private static void copyFileToDirectory(Path source, Path target) throws IOException {
        final Path targetFile = target.resolve(source.getFileName());
        Files.createDirectories(target);
        Files.copy(source, targetFile, StandardCopyOption.REPLACE_EXISTING);
    }

    private static void copyDirectoryContent(Path source, Path target, Predicate<? super Path> filter) throws IOException {
        assert (source != null);
        assert (target != null);
        assert (filter != null);

        Files.createDirectories(target);

        Files.walkFileTree(source, new SimpleFileVisitor<Path>() {

            /**
             * @see java.nio.file.SimpleFileVisitor#preVisitDirectory(java.lang.Object,
             *      java.nio.file.attribute.BasicFileAttributes)
             */
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                if (filter.test(dir)) {
                    Files.createDirectories(targetFromSource(dir));
                    return super.preVisitDirectory(dir, attrs);
                }

                return FileVisitResult.SKIP_SUBTREE;
            }

            /**
             * @see java.nio.file.SimpleFileVisitor#visitFile(java.lang.Object,
             *      java.nio.file.attribute.BasicFileAttributes)
             */
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                if (filter.test(file)) {
                    Files.copy(file, targetFromSource(file), StandardCopyOption.REPLACE_EXISTING);
                }

                return super.visitFile(file, attrs);
            }

            private Path targetFromSource(Path path) {
                return target.resolve(source.relativize(path));
            }
        });
    }

    private static final class DeletingFileVisitor extends SimpleFileVisitor<Path> {

        private final Predicate<? super Path> omission;
        private NavigableSet<Path> omitted;

        /**
         * Creates a new instance.
         *
         * @param givenFilter
         *            the filter to choose paths to delete. It must not be
         *            {@code null}.
         */
        public DeletingFileVisitor(Predicate<? super Path> givenFilter) {
            omission = givenFilter.negate();
        }

        /**
         * @see java.nio.file.SimpleFileVisitor#preVisitDirectory(java.lang.Object,
         *      java.nio.file.attribute.BasicFileAttributes)
         */
        @Override
        public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
            if (omission.test(dir)) {
                omitted().add(dir);
                return FileVisitResult.SKIP_SUBTREE;
            }

            return super.preVisitDirectory(dir, attrs);
        }

        /**
         * @see java.nio.file.SimpleFileVisitor#visitFile(java.lang.Object,
         *      java.nio.file.attribute.BasicFileAttributes)
         */
        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
            if (omission.test(file)) {
                omitted().add(file);
            } else {
                Files.delete(file);
            }

            return super.visitFile(file, attrs);
        }

        /**
         * @see java.nio.file.SimpleFileVisitor#postVisitDirectory(java.lang.Object,
         *      java.io.IOException)
         */
        @Override
        public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
            if (hasChildrenDeleted(dir)) {
                Files.delete(dir);
            }

            return super.postVisitDirectory(dir, exc);
        }

        private boolean hasChildrenDeleted(Path path) {
            if (omitted == null) {
                return true;
            }

            final Path found = omitted.ceiling(path);
            return ((found == null) || !found.startsWith(path));
        }

        private NavigableSet<Path> omitted() {
            if (omitted == null) {
                omitted = new TreeSet<>();
            }

            return omitted;
        }
    }
}
