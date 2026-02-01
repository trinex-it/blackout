package it.trinex.blackout.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "blackout.jwt")
public class JwtProperties {

    /**
     * Secret key for JWT token signing and verification.
     * Should be a base64-encoded string.
     */
    private String secret;

    /**
     * Access token expiration time in milliseconds.
     * Default: 7 days (604800000 ms)
     */
    private Long accessTokenExp = 604800000L;

    /**
     * Refresh token expiration time in milliseconds.
     * Default: 30 days (2592000000 ms)
     */
    private Long refreshTokenExp = 2592000000L;

    private Long defaultRefreshExpirationNoRemember = 3600000L;
}
