package it.trinex.blackout.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for Redis connection.
 * Allows customization of Redis server connection settings through application.yml or application.properties.
 */
@Data
@ConfigurationProperties(prefix = "blackout.redis")
public class RedisProperties {

    /**
     * Redis server host.
     * Default: localhost
     */
    private String host = "localhost";

    /**
     * Redis server port.
     * Default: 6379
     */
    private int port = 6379;

    /**
     * Optional password for Redis authentication.
     * Leave empty if Redis server doesn't require authentication.
     */
    private String password;

    /**
     * Whether Redis integration is enabled.
     * When disabled, no Redis beans will be created.
     * Default: false
     */
    private boolean enabled = false;
}
