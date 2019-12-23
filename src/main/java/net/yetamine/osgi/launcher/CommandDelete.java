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
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * Implements the delete command.
 */
public final class CommandDelete extends Command {

    /**
     * Creates a new instance.
     */
    public CommandDelete() {
        // Default constructor
    }

    /**
     * @see net.yetamine.osgi.launcher.Command#handle(java.util.List)
     */
    @Override
    protected void handle(List<String> args) throws ExecutionException {
        final Path path = Paths.get(new Arguments(args).requireString("INSTANCE"));

        try {
            logger().info(logger -> logger.info("Deleting instance: " + path));

            if (!InstanceControl.delete(path)) {
                logger().info("The instance was missing already.");
            }
        } catch (IOException e) {
            throw new ExecutionException(e);
        }
    }
}
