package it.trinex.nnh.security.jwt;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.HashMap;
import java.util.Map;

/**
 * JWT-specific configuration properties.
 *
 * <p>This class provides a convenient way to access JWT configuration values.
 * For most use cases, you can inject and use {@link it.trinex.nnh.config.SecurityProperties} instead.</p>
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "nnh.security.jwt")
public class JwtProperties {

    /**
     * Base64-encoded secret key.
     * Must be at least 256 bits (32 bytes) when decoded.
     */
    private String secret;

    /**
     * Access token expiration times per role (in milliseconds).
     */
    private Map<String, Long> accessTokenExpiration = new HashMap<String, Long>() {{
        put("OWNER", 3600000L);      // 15 minutes
        put("ADMIN", 3600000L);      // 15 minutes
    }};

    /**
     * Refresh token expiration times per role (in milliseconds).
     */
    private Map<String, Long> refreshTokenExpiration = new HashMap<String, Long>() {{
        put("OWNER", 7776000000L);   // 90 days
        put("ADMIN", 604800000L);    // 7 days
    }};

    /**
     * Get access token expiration for a specific role.
     *
     * @param role the role (e.g., "OWNER", "ADMIN")
     * @return expiration time in milliseconds, or 15 minutes default
     */
    public long getAccessTokenExpiration(String role) {
        return accessTokenExpiration.getOrDefault(role, 3600000L);
    }

    /**
     * Get the JWT secret key.
     *
     * @return the base64-encoded secret
     */
    public String getSecret() {
        return secret;
    }

    /**
     * Get refresh token expiration for a specific role.
     *
     * @param role the role (e.g., "OWNER", "ADMIN")
     * @return expiration time in milliseconds, or 7 days default
     */
    public long getRefreshTokenExpiration(String role) {
        return refreshTokenExpiration.getOrDefault(role, 604800000L);
    }

    /**
     * Set access token expiration for a specific role.
     *
     * @param role the role
     * @param expirationMs expiration time in milliseconds
     */
    public void setAccessTokenExpiration(String role, Long expirationMs) {
        if (accessTokenExpiration == null) {
            accessTokenExpiration = new HashMap<>();
        }
        accessTokenExpiration.put(role, expirationMs);
    }

    /**
     * Set refresh token expiration for a specific role.
     *
     * @param role the role
     * @param expirationMs expiration time in milliseconds
     */
    public void setRefreshTokenExpiration(String role, Long expirationMs) {
        if (refreshTokenExpiration == null) {
            refreshTokenExpiration = new HashMap<>();
        }
        refreshTokenExpiration.put(role, expirationMs);
    }
}
