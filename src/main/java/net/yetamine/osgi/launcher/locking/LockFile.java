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

package net.yetamine.osgi.launcher.locking;

import java.io.Closeable;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.nio.file.Path;

/**
 * Encapsulates a lock file, which allows limited mutual exclusion of different
 * processes.
 *
 * <p>
 * The actual lock is acquired on the first successful call of {@link #lock()}.
 * The implementation is re-entrant, so {@link #lock()} may be invoked multiple
 * times. The lock is released when the locking calls are balanced with calling
 * {@link #unlock()}, or when {@link #close()} is called.
 */
public final class LockFile implements Closeable {

    private final RandomAccessFile file;
    private final Path path;
    private FileLock lock;
    private int lockCount;

    /**
     * Creates a new instance.
     *
     * @param givenPath
     *            the path to the file to use as the lock. It must not be
     *            {@code null}.
     *
     * @throws IOException
     *             if the file could not be opened
     */
    public LockFile(Path givenPath) throws IOException {
        file = new RandomAccessFile(givenPath.toString(), "rw");
        path = givenPath.toRealPath();
    }

    /**
     * Creates a new instance and invokes {@link #lock()} on it immediately.
     *
     * @param path
     *            the path to the file to use as the lock. It must not be
     *            {@code null}.
     *
     * @return the new instance
     *
     * @throws IOException
     *             if the file could not be opened or the lock could not be
     *             acquired
     */
    public static LockFile lock(Path path) throws IOException {
        final LockFile result = new LockFile(path);

        try {
            result.lock();
        } catch (IOException e) {
            result.close();
            throw e;
        }

        return result;
    }

    /**
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "LockFile[path=" + path + ']';
    }

    /**
     * Closes the lock file and releases the lock.
     *
     * @see java.io.Closeable#close()
     */
    @Override
    public synchronized void close() throws IOException {
        lockCount = 0;
        lock = null;
        file.close(); // This should release the lock anyway
    }

    /**
     * Closes the lock file and releases the lock like {@link #close()}, but
     * ignores any failures.
     */
    public void abort() {
        try {
            close();
        } catch (IOException e) {
            // Ignore it for good
        }
    }

    /**
     * Ensures that the lock on the file is acquired.
     *
     * @throws IllegalStateException
     *             if locked too many times already
     * @throws LockFileException
     *             if the lock could not be acquired
     */
    public synchronized void lock() throws LockFileException {
        if (lock != null) {
            if (lockCount == Integer.MAX_VALUE) {
                throw new IllegalStateException("Too many lock attempts.");
            }

            ++lockCount;
            return;
        }

        try {
            assert (lockCount == 0);
            lock = file.getChannel().tryLock();
            if (lock != null) {
                lockCount = 1;
                return;
            }
        } catch (OverlappingFileLockException | IOException e) {
            throw new LockFileException(e);
        }

        throw new LockFileException();
    }

    /**
     * Negates the effect of a single {@link #lock()} invocation and when the
     * invocations are balanced, the lock on the file is released.
     *
     * @return {@code false} if the lock was not locked (e.g., because
     *         {@link #abort()} was invoked before and the lock broke)
     *
     * @throws IOException
     *             if the operation failed
     */
    public synchronized boolean unlock() throws IOException {
        if (lockCount == 0) {
            return false;
        }

        assert (lockCount > 0);
        if (--lockCount == 0) {
            final FileLock current = lock;
            lock = null; // Failure-resilient
            current.release();
        }

        return true;
    }

    /**
     * @return {@code true} if the lock is still acquired
     */
    public synchronized boolean locked() {
        return (lock != null);
    }

    /**
     * @return the path of the lock file
     */
    public Path path() {
        return path;
    }
}
