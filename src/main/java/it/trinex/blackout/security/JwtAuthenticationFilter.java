package it.trinex.blackout.security;

import jakarta.servlet.Filter;

/**
 * Marker interface for JWT authentication filters.
 * Implementations extract JWT tokens from different sources (header or cookie)
 * and set the authentication in the SecurityContext.
 */
public interface JwtAuthenticationFilter extends Filter {
}
