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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Test {@link LockFile}.
 */
public final class TestLockFile {

    @Test
    @SuppressWarnings("resource")
    public void reentrancy(@TempDir Path lockDir) throws IOException {
        final Path lockPath = lockDir.resolve("instance.lock");

        final LockFile leaking;
        try (LockFile lock = LockFile.lock(lockPath)) {
            assertEquals(lockPath, lock.path());
            lock.lock();
            try {
                assertTrue(lock.locked());
            } finally {
                assertTrue(lock.unlock());
            }

            assertTrue(lock.locked());
            leaking = lock;
        }

        assertFalse(leaking.locked());
        assertFalse(leaking.unlock());
    }

    @Test
    public void aborting(@TempDir Path lockDir) throws IOException {
        final Path lockPath = lockDir.resolve("instance.lock");
        try (LockFile lock = LockFile.lock(lockPath)) {
            assertEquals(lockPath, lock.path());
            assertTrue(lock.locked());
            lock.abort();
            assertFalse(lock.locked());
        }
    }

    @Test
    public void excluded_lock(@TempDir Path lockDir) throws IOException {
        final Path lockPath = lockDir.resolve("instance.lock");
        try (LockFile successfulLock = LockFile.lock(lockPath)) {
            assertEquals(lockPath, successfulLock.path());
            assertThrows(LockFileException.class, () -> {
                try (LockFile excludedLock = LockFile.lock(lockPath)) {
                    fail("Lock should be occupied: " + excludedLock);
                }
            });
        }
    }

    @Test
    public void inactive_lock(@TempDir Path lockDir) throws IOException {
        final Path lockPath = lockDir.resolve("instance.lock");
        try (LockFile inactiveLock = new LockFile(lockPath); LockFile successfulLock = LockFile.lock(lockPath)) {
            assertEquals(inactiveLock.path(), successfulLock.path());
        }
    }

    @Test
    public void missing_path(@TempDir Path lockDir) {
        assertThrows(IOException.class, () -> {
            try (LockFile missingLock = LockFile.lock(lockDir.resolve("missing/instance.lock"))) {
                fail("Lock directory should be missing: " + missingLock);
            }
        });
    }
}
