package net.yetamine.osgi.launcher.remoting;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketAddress;
import java.util.Objects;
import java.util.function.Function;

/**
 * Sends commands via UDP packets.
 */
public final class CommandSender {

    private final Function<? super String, ? extends byte[]> encoder;
    private final SocketAddress address;

    /**
     * Creates a new instance.
     *
     * @param targetAddress
     *            the address to send the command packet to. It must not be
     *            {@code null}.
     * @param commandEncoder
     *            the command encoder. It must not be {@code null}.
     */
    public CommandSender(SocketAddress targetAddress, Function<? super String, byte[]> commandEncoder) {
        encoder = Objects.requireNonNull(commandEncoder);
        address = Objects.requireNonNull(targetAddress);
    }

    /**
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "CommandSender[target=" + address + "]";
    }

    /**
     * Sends the command.
     *
     * @param command
     *            the command to send. It must not be {@code null}.
     *
     * @throws IOException
     *             if the operation failed
     */
    public void send(String command) throws IOException {
        send(encoder.apply(command));
    }

    /**
     * @return the address to send to
     */
    public SocketAddress address() {
        return address;
    }

    private void send(byte[] message) throws IOException {
        try (DatagramSocket socket = new DatagramSocket()) {
            socket.send(new DatagramPacket(message, message.length, address));
        }
    }
}
