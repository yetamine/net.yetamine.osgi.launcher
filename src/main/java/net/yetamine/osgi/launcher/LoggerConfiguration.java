package net.yetamine.osgi.launcher;

import java.io.IOException;
import java.io.InputStream;
import java.util.logging.LogManager;

/**
 * Loads {@code java.util.logging} configuration from a resource.
 */
public final class LoggerConfiguration {

    /**
     * Creates a new instance.
     *
     * @throws IOException
     *             if reading the properties failed
     */
    public LoggerConfiguration() throws IOException {
        final LogManager logManager = LogManager.getLogManager();

        try (InputStream is = getClass().getResourceAsStream("/java.util.logging.properties")) {
            if (is != null) { // The resource might be absent
                logManager.readConfiguration(is);
            }
        }
    }
}
