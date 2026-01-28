package it.trinex.nnh.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Central configuration properties for the NNH Security system.
 *
 * <p>Configure these properties in application.yml:</p>
 * <pre>
 * nnh:
 *   security:
 *     enabled: true
 *     use-jpa: true
 *     use-in-memory-auth: false
 *     expose-controller: true
 *     jwt:
 *       secret: YOUR_BASE64_SECRET
 *       access-token:
 *         expiration:
 *           OWNER: 3600000
 *           ADMIN: 3600000
 *       refresh-token:
 *         expiration:
 *           OWNER: 7776000000
 *           ADMIN: 604800000
 *     cookie:
 *       http-only: true
 *       secure: true
 *       same-site: None
 *       path: /
 *     cors:
 *       allowed-origins:
 *         - http://localhost:4200
 *       allow-credentials: true
 * </pre>
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "nnh.security")
public class SecurityProperties {

    /**
     * Enable/disable entire security system.
     */
    private boolean enabled = true;

    /**
     * Use JPA repositories (set false for custom UserDetailsService).
     */
    private boolean useJpa = true;

    /**
     * Use in-memory authentication (for testing only).
     */
    private boolean useInMemoryAuth = false;

    /**
     * Expose default AuthController (set false to use custom controller).
     */
    private boolean exposeController = true;

    /**
     * JWT configuration properties.
     */
    private Jwt jwt = new Jwt();

    /**
     * Cookie configuration properties.
     */
    private Cookie cookie = new Cookie();

    /**
     * CORS configuration properties.
     */
    private Cors cors = new Cors();

    @Getter
    @Setter
    public static class Jwt {
        /**
         * Base64-encoded secret key (minimum 256 bits).
         */
        private String secret;

        /**
         * Access token configuration.
         */
        private TokenExpiration accessToken = new TokenExpiration();

        /**
         * Refresh token configuration.
         */
        private TokenExpiration refreshToken = new TokenExpiration();
    }

    @Getter
    @Setter
    public static class TokenExpiration {
        /**
         * Token expiration times per role (in milliseconds).
         */
        private Map<String, Long> expiration = new HashMap<String, Long>() {{
            put("OWNER", 3600000L);      // 15 minutes
            put("ADMIN", 3600000L);      // 15 minutes
        }};

        /**
         * Get token expiration for a specific role.
         *
         * @param role the role (e.g., "OWNER", "ADMIN")
         * @return expiration time in milliseconds
         */
        public Long getExpirationForRole(String role) {
            return expiration.getOrDefault(role, 3600000L);
        }

        /**
         * Set token expiration for a specific role.
         *
         * @param role the role (e.g., "OWNER", "ADMIN")
         * @param expirationMs expiration time in milliseconds
         */
        public void setExpirationForRole(String role, Long expirationMs) {
            expiration.put(role, expirationMs);
        }
    }

    @Getter
    @Setter
    public static class Cookie {
        /**
         * HTTP-only cookie (prevents XSS).
         */
        private boolean httpOnly = true;

        /**
         * Secure cookie (HTTPS only).
         */
        private boolean secure = true;

        /**
         * SameSite attribute: Strict, Lax, None.
         */
        private String sameSite = "None";

        /**
         * Cookie path.
         */
        private String path = "/";

        /**
         * Cookie domain (optional).
         */
        private String domain;

        /**
         * Get SameSite value as proper enum-like format.
         */
        public String getSameSite() {
            if ("None".equalsIgnoreCase(sameSite) || "Lax".equalsIgnoreCase(sameSite) || "Strict".equalsIgnoreCase(sameSite)) {
                return sameSite.substring(0, 1).toUpperCase() + sameSite.substring(1).toLowerCase();
            }
            return "None";
        }
    }

    @Getter
    @Setter
    public static class Cors {
        /**
         * Allowed origins for CORS.
         */
        private List<String> allowedOrigins = new ArrayList<>();

        /**
         * Allowed HTTP methods.
         */
        private List<String> allowedMethods = Arrays.asList("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS");

        /**
         * Allowed headers.
         */
        private List<String> allowedHeaders = Arrays.asList("*");

        /**
         * Allow credentials.
         */
        private Boolean allowCredentials = true;

        /**
         * Preflight cache max age (in seconds).
         */
        private Long maxAge = 3600L;
    }
}
