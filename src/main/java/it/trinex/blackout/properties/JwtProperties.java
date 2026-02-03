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
     * Default: 15 minutes (900000 ms)
     */
    private Long accessTokenExp = 900000L;

    /**
     * Refresh token expiration time in milliseconds.
     * Default: 30 days (2592000000 ms)
     */
    private Long refreshTokenExp = 2592000000L;

    private Long refreshTokenExpNoRemember = 3600000L;
}
