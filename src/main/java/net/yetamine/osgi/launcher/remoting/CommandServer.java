package net.yetamine.osgi.launcher.remoting;

import java.io.Closeable;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Implements a server for receiving commands via UDP packets.
 */
public final class CommandServer implements Closeable {

    private static final int MAX_PACKET_LENGTH = 0xFFFF;

    private final Function<? super ByteBuffer, String> decoder;
    private final ExecutorService executor;
    private final DatagramSocket socket;

    private Consumer<? super CommandServer> onClose;
    private volatile BiConsumer<? super String, Object> onCommand;
    private volatile Consumer<? super Exception> onError;
    private volatile boolean destroyed;

    /**
     * Creates a new instance that never listens to anything.
     *
     * @param closeHandler
     *            the handler to invoke on {@link #close()}
     */
    private CommandServer(Consumer<? super CommandServer> closeHandler) {
        onClose = closeHandler;
        executor = null;
        decoder = null;
        socket = null;
    }

    /**
     * Creates a new instance.
     *
     * @param configuration
     *            the configuration to use. It must not be {@code null}.
     * @param address
     *            the address to bind to. It must not be {@code null}.
     *
     * @throws IOException
     *             if the operation fails
     */
    private CommandServer(Configuration configuration, SocketAddress address) throws IOException {
        decoder = Objects.requireNonNull(configuration.decoder);
        onCommand = configuration.onCommand;
        onError = configuration.onError;
        onClose = configuration.onClose;
        socket = new DatagramSocket(address);
        executor = Executors.newSingleThreadExecutor(CommandServer::threadFactory);

        if (onClose == null) {
            onClose = that -> that.onError(null);
        }
    }

    /**
     * Creates a new configuration.
     *
     * @param handler
     *            the handler to be notified when the server receives a command.
     *            It must not be {@code null}.
     *
     * @return a new configuration
     */
    public static Configuration configure(BiConsumer<? super String, Object> handler) {
        return new Configuration().onCommand(Objects.requireNonNull(handler));
    }

    /**
     * Creates a new instance that never listens to anything.
     *
     * @param closeHandler
     *            the handler to invoke on {@link #close()}
     *
     * @return the new instance
     */
    public static CommandServer fake(Consumer<? super CommandServer> closeHandler) {
        return new CommandServer(closeHandler);
    }

    /**
     * @return a new configuration
     */
    public static Configuration configure() {
        return new Configuration();
    }

    /**
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "CommandServer[socket=" + socket + ']';
    }

    /**
     * Sets the handler to be notified when the server receives a command.
     *
     * @param handler
     *            the handler
     */
    public void onCommand(BiConsumer<? super String, Object> handler) {
        onCommand = handler;
    }

    /**
     * Sets the handler to be notified when the socket reports an error.
     *
     * @param handler
     *            the handler
     */
    public void onError(Consumer<? super Exception> handler) {
        onError = handler;
    }

    /**
     * @return the bound address
     */
    public Optional<InetSocketAddress> address() {
        return Optional.ofNullable(socket).map(s -> (InetSocketAddress) s.getLocalSocketAddress());
    }

    /**
     * @see java.io.Closeable#close()
     */
    @Override
    public synchronized void close() {
        try {
            invokeHandler(onClose, this);
        } finally {
            onClose = null;
            destroy();
        }
    }

    /**
     * Creates a new instance.
     *
     * @param configuration
     *            the configuration to use. It must not be {@code null}.
     * @param address
     *            the address to bind to. It must not be {@code null}.
     *
     * @throws IOException
     *             if the operation fails
     */
    static CommandServer from(Configuration configuration, SocketAddress address) throws IOException {
        final CommandServer result = new CommandServer(configuration, address);
        result.launch();
        return result;
    }

    private void launch() {
        assert ((executor != null) && (socket != null));
        executor.execute(this::listen);
    }

    private void listen() {
        try {
            final DatagramPacket packet = new DatagramPacket(new byte[MAX_PACKET_LENGTH], MAX_PACKET_LENGTH);
            while (!Thread.currentThread().isInterrupted()) {
                socket.receive(packet);
                deliver(decode(packet), packet.getSocketAddress());
            }
        } catch (SocketException e) {
            if (!destroyed) { // Otherwise the receiving was interrupted by closing asynchronously
                invokeHandler(onError, e);
            }
        } catch (Exception e) {
            invokeHandler(onError, e);
        } finally {
            destroy();
        }
    }

    private String decode(DatagramPacket packet) {
        return decoder.apply(ByteBuffer.wrap(packet.getData(), packet.getOffset(), packet.getLength()));
    }

    private void deliver(String command, SocketAddress socketAddress) {
        if (command == null) {
            return;
        }

        invokeHandler(onCommand, command, socketAddress);
    }

    private void destroy() {
        if (executor != null) {
            executor.shutdownNow();
            destroyed = true;
            socket.close();
        }
    }

    private static <T, U> void invokeHandler(BiConsumer<? super T, ? super U> handler, T t, U u) {
        if (handler != null) {
            handler.accept(t, u);
        }
    }

    private static <T> void invokeHandler(Consumer<? super T> handler, T t) {
        if (handler != null) {
            handler.accept(t);
        }
    }

    private static Thread threadFactory(Runnable r) {
        final Thread result = new Thread(r);
        result.setName("ComandServer");
        result.setDaemon(true);
        return result;
    }

    /**
     * A builder for making the server.
     */
    public static final class Configuration {

        BiConsumer<? super String, Object> onCommand;
        Consumer<? super CommandServer> onClose;
        Consumer<? super Exception> onError;
        Function<? super ByteBuffer, String> decoder;

        /**
         * Creates a new instance.
         */
        public Configuration() {
            // Default constructor
        }

        /**
         * Sets the packet decoder.
         *
         * @param givenDecoder
         *            the decoder to use
         *
         * @return this instance
         */
        public Configuration withDecoder(Function<? super ByteBuffer, String> givenDecoder) {
            decoder = givenDecoder;
            return this;
        }

        /**
         * Sets the handler to be notified when the server receives a command.
         *
         * @param handler
         *            the handler
         *
         * @return this instance
         */
        public Configuration onCommand(BiConsumer<? super String, Object> handler) {
            onCommand = handler;
            return this;
        }

        /**
         * Sets the handler to be notified when the server is closed explicitly.
         *
         * @param handler
         *            the handler
         *
         * @return this instance
         */
        public Configuration onClose(Consumer<? super CommandServer> handler) {
            onClose = handler;
            return this;
        }

        /**
         * Sets the handler to be notified when the socket reports an error.
         *
         * @param handler
         *            the handler
         *
         * @return this instance
         */
        public Configuration onError(Consumer<? super Exception> handler) {
            onError = handler;
            return this;
        }

        /**
         * Opens the server for receiving.
         *
         * @param address
         *            the address to bind to. It must not be {@code null}.
         *
         * @return the server
         *
         * @throws IOException
         *             if the server could not bind the address
         */
        public CommandServer open(SocketAddress address) throws IOException {
            return CommandServer.from(this, address);
        }

        /**
         * Opens the server for receiving.
         *
         * @param address
         *            the address to bind to. It must not be {@code null}.
         * @param port
         *            the port to listen at. It must be in the acceptable port
         *            range.
         *
         * @return the server
         *
         * @throws IOException
         *             if the server could not bind the address
         */
        public CommandServer open(InetAddress address, int port) throws IOException {
            return open(new InetSocketAddress(address, port));
        }
    }
}
