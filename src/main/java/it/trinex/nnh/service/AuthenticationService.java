package it.trinex.nnh.service;

import it.trinex.nnh.dto.response.AuthResponse;
import it.trinex.nnh.security.jwt.JwtUserPrincipal;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Service for handling authentication operations.
 *
 * <p>This service provides methods for:</p>
 * <ul>
 *   <li>Creating JWT tokens (access and refresh)</li>
 *   <li>Managing authentication cookies</li>
 *   <li>Refreshing access tokens</li>
 *   <li>Logging out users</li>
 * </ul>
 */
public interface AuthenticationService {

    /**
     * Create tokens for a user principal.
     *
     * @param userPrincipal the user principal
     * @param rememberMe whether to remember the user
     * @return the authentication response
     */
    AuthResponse createTokens(JwtUserPrincipal userPrincipal, boolean rememberMe);

    /**
     * Set token cookies on the HTTP response.
     *
     * @param response the HTTP response
     * @param authResponse the authentication response
     * @param rememberMe whether to remember the user
     */
    void setTokenCookies(HttpServletResponse response, AuthResponse authResponse, boolean rememberMe);

    /**
     * Set only the access token cookie on the HTTP response.
     *
     * @param response the HTTP response
     * @param authResponse the authentication response
     */
    void setAccessTokenCookie(HttpServletResponse response, AuthResponse authResponse);

    /**
     * Clear all token cookies from the HTTP response.
     *
     * @param response the HTTP response
     */
    void clearTokenCookies(HttpServletResponse response);

    /**
     * Refresh an access token using a refresh token.
     *
     * @param refreshToken the refresh token
     * @return the new authentication response
     */
    AuthResponse refreshAccessToken(String refreshToken);

    /**
     * Get the current authenticated user.
     *
     * @return the JWT user principal
     */
    JwtUserPrincipal getCurrentUser();
}
