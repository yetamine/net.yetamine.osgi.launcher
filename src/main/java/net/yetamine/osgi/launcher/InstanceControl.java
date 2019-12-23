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

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import net.yetamine.osgi.launcher.locking.LockFile;
import net.yetamine.osgi.launcher.locking.LockFileException;

/**
 * Provides means for controlling and modifying a deployment instance.
 */
public final class InstanceControl extends InstanceInquiry implements Closeable {

    private static final String LOCK_FILE = "instance.lock";

    private final LockFile lockFile;

    /**
     * Creates a new instance.
     *
     * @param givenLocation
     *            the location of the location. It must not be {@code null}.
     *
     * @throws LockFileException
     *             if the instance control acquisition failed
     * @throws IOException
     *             if the location could not be created
     */
    public InstanceControl(Path givenLocation) throws IOException {
        super(Files.createDirectories(givenLocation));
        lockFile = LockFile.lock(lockPath());
        ensureValid();
    }

    /**
     * Deletes the specified instance while ensures that it could not be used
     * during the operation.
     *
     * @param path
     *            the instance location. It must not be {@code null}.
     *
     * @return {@code true} if the instance was deleted, {@code false} if the
     *         location does not point to an existing directory
     *
     * @throws LockFileException
     *             if the instance control acquisition failed
     * @throws IOException
     *             if the instance could not be deleted
     */
    public static boolean delete(Path path) throws IOException {
        if (Files.notExists(path)) {
            return false;
        }

        if (!InstanceInquiry.seemsValid(path)) {
            throw new IOException("Target path does not point to instance: " + path);
        }

        final Path lock;
        try (InstanceControl instance = new InstanceControl(path)) {
            instance.clean();
            lock = instance.lockPath();
        }

        // Delete selectively as it might clash with a concurrent acquisition
        Files.deleteIfExists(lock);
        Files.deleteIfExists(path);
        return true;
    }

    /**
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "InstanceControl[location=" + location() + ']';
    }

    /**
     * Releases the control of the instance.
     *
     * @throws IOException
     *             if the operation fails
     *
     * @see java.io.Closeable#close()
     */
    @Override
    public void close() throws IOException {
        lockFile.close();
    }

    /**
     * Executes an operation on this instance, ensuring that this instance is
     * {@link #acquired()} while executing the operation.
     *
     * @param <X>
     *            the exception that the operation may throw
     * @param operation
     *            the operation to execute. It must not be {@code null}.
     *
     * @return this instance
     *
     * @throws IllegalStateException
     *             if not {@link #acquired()}
     * @throws X
     *             if the operation throws it
     */
    public <X extends Exception> InstanceControl execute(Executable<? super InstanceControl, ? extends X> operation) throws X {
        ensureAcquired();
        operation.execute(this);
        return this;
    }

    /**
     * Invokes a function on this instance and returns its result, ensuring that
     * this instance is {@link #acquired()} while executing the operation.
     *
     * @param <R>
     *            the result of the function
     * @param <X>
     *            the exception that the function may throw
     * @param function
     *            the function to invoke. It must not be {@code null}.
     *
     * @return the result of the function
     *
     * @throws X
     *             if the function throws it
     */
    public <R, X extends Exception> R invoke(ThrowingFunction<? super InstanceControl, ? extends R, ? extends X> function) throws X {
        ensureAcquired();
        return function.apply(this);
    }

    /**
     * Deletes all data of the instance.
     *
     * @return this instance.
     *
     * @throws IllegalStateException
     *             if not {@link #acquired()}
     * @throws IOException
     *             if the operation failed
     */
    public InstanceControl clean() throws IOException {
        ensureAcquired();
        final Path rootPath = location();
        final Path lockPath = lockPath();
        FileHandling.delete(rootPath, path -> isDifferent(path, lockPath));
        return this;
    }

    /**
     * @return {@code true} if the control is still maintained
     */
    public boolean acquired() {
        return lockFile.locked();
    }

    private void ensureAcquired() {
        if (!acquired()) {
            throw new IllegalStateException("This operation requires the control to be held.");
        }
    }

    private void ensureValid() throws IOException {
        Files.createDirectories(path(InstanceLayout.ETC_PATH));
    }

    private Path lockPath() {
        return path(LOCK_FILE);
    }

    private static boolean isDifferent(Path path, Path lockPath) {
        try {
            return !Files.isSameFile(lockPath, path);
        } catch (IOException e) {
            return false;
        }
    }
}
