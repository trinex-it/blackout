package it.trinex.nnh.util;

import it.trinex.nnh.config.SecurityProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Utility class for creating and managing HTTP-only cookies for JWT tokens.
 *
 * <p>This utility creates cookies with the following security features:</p>
 * <ul>
 *   <li>HttpOnly - Prevents JavaScript access (XSS protection)</li>
 *   <li>Secure - Only sent over HTTPS</li>
 *   <li>SameSite - CSRF protection</li>
 * </ul>
 */
@Component
@RequiredArgsConstructor
public class CookieUtils {

    private final SecurityProperties securityProperties;

    /**
     * Create an access token cookie.
     *
     * @param token the JWT access token
     * @param maxAgeMs the max age in milliseconds
     * @return the ResponseCookie
     */
    public ResponseCookie createAccessTokenCookie(String token, long maxAgeMs) {
        SecurityProperties.Cookie cookieConfig = securityProperties.getCookie();

        ResponseCookie.ResponseCookieBuilder builder = ResponseCookie.from("access_token", token)
                .httpOnly(cookieConfig.isHttpOnly())
                .secure(cookieConfig.isSecure())
                .path(cookieConfig.getPath())
                .maxAge(Duration.ofMillis(maxAgeMs))
                .sameSite(cookieConfig.getSameSite());

        if (cookieConfig.getDomain() != null && !cookieConfig.getDomain().isEmpty()) {
            builder.domain(cookieConfig.getDomain());
        }

        return builder.build();
    }

    /**
     * Create a refresh token cookie.
     *
     * @param token the JWT refresh token
     * @param rememberMe if true, creates a persistent cookie (90 days); if false, creates a session cookie
     * @return the ResponseCookie
     */
    public ResponseCookie createRefreshTokenCookie(String token, boolean rememberMe) {
        SecurityProperties.Cookie cookieConfig = securityProperties.getCookie();

        ResponseCookie.ResponseCookieBuilder builder = ResponseCookie.from("refresh_token", token)
                .httpOnly(cookieConfig.isHttpOnly())
                .secure(cookieConfig.isSecure())
                .path(cookieConfig.getPath())
                .sameSite(cookieConfig.getSameSite());

        if (cookieConfig.getDomain() != null && !cookieConfig.getDomain().isEmpty()) {
            builder.domain(cookieConfig.getDomain());
        }

        if (rememberMe) {
            // Persistent cookie with long expiration (90 days)
            builder.maxAge(Duration.ofDays(90));
        } else {
            // Session cookie (no Max-Age, deleted when browser closes)
            // Don't set maxAge - it will be a session cookie
        }

        return builder.build();
    }

    /**
     * Create a clear access token cookie (Max-Age=0).
     *
     * @return the ResponseCookie that will clear the cookie
     */
    public ResponseCookie clearAccessTokenCookie() {
        SecurityProperties.Cookie cookieConfig = securityProperties.getCookie();

        ResponseCookie.ResponseCookieBuilder builder = ResponseCookie.from("access_token", "")
                .httpOnly(cookieConfig.isHttpOnly())
                .secure(cookieConfig.isSecure())
                .path(cookieConfig.getPath())
                .maxAge(0)
                .sameSite(cookieConfig.getSameSite());

        if (cookieConfig.getDomain() != null && !cookieConfig.getDomain().isEmpty()) {
            builder.domain(cookieConfig.getDomain());
        }

        return builder.build();
    }

    /**
     * Create a clear refresh token cookie (Max-Age=0).
     *
     * @return the ResponseCookie that will clear the cookie
     */
    public ResponseCookie clearRefreshTokenCookie() {
        SecurityProperties.Cookie cookieConfig = securityProperties.getCookie();

        ResponseCookie.ResponseCookieBuilder builder = ResponseCookie.from("refresh_token", "")
                .httpOnly(cookieConfig.isHttpOnly())
                .secure(cookieConfig.isSecure())
                .path(cookieConfig.getPath())
                .maxAge(0)
                .sameSite(cookieConfig.getSameSite());

        if (cookieConfig.getDomain() != null && !cookieConfig.getDomain().isEmpty()) {
            builder.domain(cookieConfig.getDomain());
        }

        return builder.build();
    }
}
