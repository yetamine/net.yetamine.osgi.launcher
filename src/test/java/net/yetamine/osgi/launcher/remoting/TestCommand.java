package net.yetamine.osgi.launcher.remoting;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.InetAddress;
import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Tests {@link CommandSender} and {@link CommandServer} in conjunction.
 */
public final class TestCommand {

    private static final Duration RECEIVE_TIMEOUT = Duration.ofSeconds(5);

    @Test
    public void real_server() throws Exception {
        final String testCommand = "test";
        final CryptoProtection crypto = new CryptoProtection("secret");
        final CountDownLatch latch = new CountDownLatch(1);
        final BiConsumer<String, Object> handler = (command, origin) -> {
            assertEquals(testCommand, command);
            latch.countDown();
        };

        try (CommandServer server = CommandServer.configure(handler)    ///
                .withDecoder(crypto::decrypt)                           ///
                .onError(Assertions::fail)                              /// No failure happens
                .open(InetAddress.getLoopbackAddress(), 0)) {

            // Server is running, send the command to it, use the same encryption
            new CommandSender(server.address().get(), crypto::encrypt).send(testCommand);

            // Must receive the test command in time
            latch.await(RECEIVE_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
        }
    }

    @Test
    public void fake_server() throws Exception {
        final AtomicBoolean closed = new AtomicBoolean();
        try (CommandServer server = CommandServer.fake(that -> closed.set(true))) {
            assertFalse(server.address().isPresent());
        }

        assertTrue(closed.get());
    }
}
