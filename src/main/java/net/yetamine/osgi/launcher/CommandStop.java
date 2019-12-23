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
import java.security.GeneralSecurityException;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import net.yetamine.osgi.launcher.remoting.CommandLink;
import net.yetamine.osgi.launcher.remoting.CommandSender;
import net.yetamine.osgi.launcher.remoting.CryptoProtection;

/**
 * Implements the stop command.
 */
public final class CommandStop extends Command {

    /** The command to stop an instance using the command link. */
    public static final String REMOTE_COMMAND = InstanceRuntime.COMMAND_STOP;

    /**
     * Creates a new instance.
     */
    public CommandStop() {
        // Default constructor
    }

    /**
     * @see net.yetamine.osgi.launcher.Command#handle(java.util.List)
     */
    @Override
    protected void handle(List<String> args) throws ExecutionException {
        try {
            final CommandLink link = parse(args);
            final CryptoProtection protection = new CryptoProtection(link.secret());
            final String commandId = UUID.randomUUID().toString();
            final String command = command(commandId);
            logger().info(logger -> logger.info("Sending the stop command '" + commandId + "' to: " + link.address()));
            new CommandSender(link.address(), protection::encrypt).send(command);
        } catch (GeneralSecurityException e) {
            throw new SetupException("Could not protect the secret.", e);
        } catch (IOException e) {
            throw new ExecutionException("Could not send the stop command.", e);
        }
    }

    private static CommandLink parse(List<String> args) {
        try {
            switch (args.size()) {
                case 1:
                    final Path instancePath = Paths.get(args.get(0));
                    final InstanceInquiry instance = new InstanceInquiry(instancePath);
                    return instance.commandLink().orElseThrow(() -> new SetupException("No command link exposed."));

                case 3:
                    return CommandLink.from(args);

                default:
                    throw new SyntaxException("Invalid number of arguments passed.");
            }
        } catch (IllegalArgumentException e) {
            throw new SetupException("Parameters given for the instance to stop are invalid.", e);
        } catch (IOException e) {
            throw new SetupException("Could not retrieve parameters for the instance to stop.", e);
        }
    }

    private static String command(String commandId) {
        return "#id: " + UUID.randomUUID().toString() + '\n' + REMOTE_COMMAND;
    }
}
