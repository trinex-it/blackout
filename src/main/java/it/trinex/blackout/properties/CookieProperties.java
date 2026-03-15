package it.trinex.blackout.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for cookie-based authentication.
 * Allows customization of cookie behavior through application.yml or application.properties.
 */
@Data
@ConfigurationProperties(prefix = "blackout.cookie")
public class CookieProperties {

    /**
     * Whether cookie-based authentication is enabled.
     * When enabled, JWT tokens are stored in cookies instead of requiring Bearer headers.
     * Default: true
     */
    private boolean enabled = true;

    /**
     * Whether to automatically refresh access tokens using the refresh token cookie.
     * When enabled, the system will attempt to refresh expired access tokens automatically.
     * Default: true
     */
    private boolean autoRefresh = true;
}
