package io.github.avivcarmis.trafficante.core;

import com.fasterxml.jackson.databind.PropertyNamingStrategy;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Triggers the initialization of a server instance.
 * Holds the given settings of the started server.
 */
public class Trafficante {

    // Constants

    /**
     * Synchronizes a call to {@link #start(String, PropertyNamingStrategy, String, int, boolean, String[])},
     * to validate no multiple servers started
     */
    private static final AtomicBoolean SERVER_STARTED = new AtomicBoolean(false);

    // Static

    /**
     * The settings of the started server
     */
    private static Settings settings;

    /**
     * Starts a Trafficante server
     * @param basePackageName base name of package to look for endpoints in
     * @param namingStrategy  a naming strategy to be used for endpoint paths and IO serialization
     * @param host            the host to register the server with ("0.0.0.0" to enable all)
     * @param port            the port to register the server with
     * @param swaggerEnabled  whether or not to enable swagger
     * @param args            program arguments
     */
    public static void start(String basePackageName,
                             PropertyNamingStrategy namingStrategy,
                             String host,
                             int port,
                             boolean swaggerEnabled,
                             String[] args) {
        if (SERVER_STARTED.getAndSet(true)) {
            throw new RuntimeException("server already started");
        }
        System.setProperty(ApplicationLauncher.BASE_PACKAGE_INDICATOR, basePackageName);
        System.setProperty("server.address", host);
        System.setProperty("server.port", String.valueOf(port));
        System.setProperty("spring.resources.add-mappings", String.valueOf(swaggerEnabled));
        settings = new Settings(basePackageName, namingStrategy, host, port, swaggerEnabled);
        ApplicationLauncher.launch(args);
    }

    /**
     * @return the settings of the started server
     */
    public static Settings getSettings() {
        return settings;
    }

    /**
     * Settings of a Trafficante server
     */
    public static class Settings {

        private final String basePackageName;

        private final PropertyNamingStrategy namingStrategy;

        private final String host;

        private final int port;

        private final boolean swaggerEnabled;

        private Settings(String basePackageName,
                         PropertyNamingStrategy namingStrategy,
                         String host,
                         int port,
                         boolean swaggerEnabled) {
            this.basePackageName = basePackageName;
            this.namingStrategy = namingStrategy;
            this.host = host;
            this.port = port;
            this.swaggerEnabled = swaggerEnabled;
        }

        public String getBasePackageName() {
            return basePackageName;
        }

        public PropertyNamingStrategy getNamingStrategy() {
            return namingStrategy;
        }

        public String getHost() {
            return host;
        }

        public int getPort() {
            return port;
        }

        public boolean isSwaggerEnabled() {
            return swaggerEnabled;
        }

    }

}
