package net.yetamine.osgi.launcher.deploying;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

/**
 * Opens an {@link InputStream} for the given source.
 */
@FunctionalInterface
public interface InputStreamSource {

    /**
     * Opens a data stream bound to this instance.
     *
     * @return the opened data stream
     *
     * @throws IOException
     *             if could not open the stream
     */
    InputStream open() throws IOException;

    /**
     * Creates an instance for the given source and {@link Object#toString()}
     * implementation.
     *
     * @param source
     *            the source to wrap. It must not be {@code null}.
     * @param toString
     *            the result of the {@link Object#toString()} method. It must
     *            not be {@code null}.
     *
     * @return the new instance
     */
    static InputStreamSource wrap(InputStreamSource source, String toString) {
        Objects.requireNonNull(toString);
        Objects.requireNonNull(source);

        return new InputStreamSource() {

            /**
             * @see net.yetamine.osgi.launcher.deploying.InputStreamSource#open()
             */
            @Override
            public InputStream open() throws IOException {
                return source.open();
            }

            /**
             * @see java.lang.Object#toString()
             */
            @Override
            public String toString() {
                return toString;
            }
        };
    }

    /**
     * Creates an instance opening the given {@link Path}.
     *
     * @param path
     *            the path to open. It must not be {@code null}.
     *
     * @return the new instance
     */
    static InputStreamSource from(Path path) {
        Objects.requireNonNull(path);
        return wrap(() -> Files.newInputStream(path), "InputStreamSource[from=" + path + ']');
    }
}
