package com.datarocks.schemaregistry.test;

import io.confluent.kafka.schemaregistry.rest.SchemaRegistryConfig;
import io.confluent.rest.RestConfigException;
import joptsimple.internal.Strings;

import java.util.Optional;
import java.util.Properties;
import java.util.function.Supplier;

/**
 * Helper class to define {@link SchemaRegistryConfig} to start up an instance of
 * {@link io.confluent.kafka.schemaregistry.rest.SchemaRegistryRestApplication}.
 *
 * Properties are created with the fallowing default values
 * <ul>
 *  <li>{@link SchemaRegistryConfig#LISTENERS_CONFIG} with value {@code http://localhost:8081}</li>
 *  <li>{@link SchemaRegistryConfig#HOST_NAME_CONFIG} with value {@code localhost}</li>
 *  <li>{@link SchemaRegistryConfig#DEBUG_CONFIG} with value {@code true}</li>
 * </ul>
 *
 * Every default value can be overridden by using {@link SchemaRegistryProperties#addProperty(String, Object)}. Only
 * exception is {@link SchemaRegistryConfig#KAFKASTORE_BOOTSTRAP_SERVERS_CONFIG} which can be overridden also via
 * {@link SchemaRegistryProperties#addBootstrapServersSupplier(Supplier)}. The {@link Supplier} will only be invoked
 * upon constructing {@link SchemaRegistryConfig}.
 */
class SchemaRegistryProperties {

    static final String DEFAULT_HOST = "localhost";
    static final int DEFAULT_PORT = 8081;

    private static final Properties DEFAULT_PROPERTIES;

    static {
        DEFAULT_PROPERTIES = new Properties();
        DEFAULT_PROPERTIES.put(SchemaRegistryConfig.LISTENERS_CONFIG, singleListenerString(DEFAULT_PORT));
        DEFAULT_PROPERTIES.put(SchemaRegistryConfig.HOST_NAME_CONFIG, DEFAULT_HOST);
        DEFAULT_PROPERTIES.put(SchemaRegistryConfig.DEBUG_CONFIG, true);
    }

    private final Properties properties;
    private Supplier<String> bootstrapServersSupplier;

    /**
     * Construct an instance of {@link SchemaRegistryProperties} by using the provided {@link Properties}
     * as baseline.
     * @param properties {@link Properties} default value.
     */
    SchemaRegistryProperties(Properties properties) {
        this.properties = new Properties();
        this.properties.putAll(DEFAULT_PROPERTIES);
        this.properties.putAll(properties);
    }

    /**
     * Add a new property used to configure {@link SchemaRegistryProperties}.
     * @param name {@link String} defining the key property.
     * @param value {@link String} stating the value of the property.
     * @throws IllegalArgumentException if name argument is null or empty.
     */
    void addProperty(String name, Object value) {
        if (Strings.isNullOrEmpty(name)) {
            throw new IllegalArgumentException("Cannot pass null or empty name argument");
        }

        properties.put(name, value);
    }

    /**
     * Provide a way to define {@link SchemaRegistryConfig#KAFKASTORE_BOOTSTRAP_SERVERS_CONFIG} postponing
     * the actual invocation of the {@link Supplier} till the last moment before constructing
     * {@link SchemaRegistryConfig}. This function can be used as an alternative to {@link #addProperty(String, Object)}
     * when the {@link String} to connect to Kafka cannot be provided immediately.
     * @param bootstrapServers {@link Supplier<String>} to provide a {@link String} to connect to Kafka.
     */
    public void addBootstrapServersSupplier(Supplier<String> bootstrapServers) {
        this.bootstrapServersSupplier = bootstrapServers;
    }

    /**
     * Create an instance of {@link SchemaRegistryConfig} that can be used to start up a
     * {@link io.confluent.kafka.schemaregistry.rest.SchemaRegistryRestApplication}.
     * @return an instance of {@link SchemaRegistryConfig} which can be used to configure a SchemaRegistry server.
     * @throws RestConfigException if {@link SchemaRegistryConfig} cannot be built.
     */
    SchemaRegistryConfig schemaRegistryConfig() throws RestConfigException {
        Optional.ofNullable(bootstrapServersSupplier).ifPresent(s ->
                addProperty(SchemaRegistryConfig.KAFKASTORE_BOOTSTRAP_SERVERS_CONFIG, s.get()));

        return new SchemaRegistryConfig(properties);
    }

    /**
     * URL to connect to the SchemaRegistry instantiated by the {@link SchemaRegistryConfig} created by this instance.
     * @return {@link String} that can be used to connect to the SchemaRegistry created using the
     * {@link SchemaRegistryConfig} generated by this instance.
     */
    String url() {
        return properties.get(SchemaRegistryConfig.LISTENERS_CONFIG).toString().split(",")[0];
    }

    /**
     * Define a {@link String} to connect to a service exposing {@code port} on {@code localhost}.
     * @param port int defining the port to connect.
     * @return a {@link String} that can be used to connect to service running on {@code localhost:[port]}.
     */
    static String singleListenerString(int port) {
        return "http://" + DEFAULT_HOST + ":" + port;
    }
}
