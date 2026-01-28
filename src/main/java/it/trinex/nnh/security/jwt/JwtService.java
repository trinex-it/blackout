package it.trinex.nnh.security.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;
import java.util.function.Function;

/**
 * Service for JWT token generation, validation, and extraction.
 *
 * <p>This service handles:</p>
 * <ul>
 *   <li>Access token generation and validation</li>
 *   <li>Refresh token generation and validation</li>
 *   <li>User principal extraction from tokens</li>
 *   <li>Current authenticated user retrieval</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class JwtService {

    private final JwtProperties jwtProperties;

    /**
     * Get the signing key from the base64-encoded secret.
     *
     * @return the signing key
     */
    private SecretKey getSigningKey() {
        byte[] keyBytes = Base64.getDecoder().decode(jwtProperties.getSecret());
        return Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * Generate an access token for the given user principal.
     *
     * @param userPrincipal the user principal
     * @return the access token
     */
    public String generateAccessToken(JwtUserPrincipal userPrincipal) {
        long expiration = jwtProperties.getAccessTokenExpiration(userPrincipal.getRole());
        return generateToken(userPrincipal, expiration, "ACCESS");
    }

    /**
     * Generate a refresh token for the given user principal.
     *
     * @param userPrincipal the user principal
     * @return the refresh token
     */
    public String generateRefreshToken(JwtUserPrincipal userPrincipal) {
        long expiration = jwtProperties.getRefreshTokenExpiration(userPrincipal.getRole());
        return generateToken(userPrincipal, expiration, "REFRESH");
    }

    /**
     * Generate a JWT token with the specified parameters.
     *
     * @param userPrincipal the user principal
     * @param expirationMs expiration time in milliseconds
     * @param tokenType the token type (ACCESS or REFRESH)
     * @return the JWT token
     */
    private String generateToken(JwtUserPrincipal userPrincipal, long expirationMs, String tokenType) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expirationMs);

        return Jwts.builder()
                .subject(userPrincipal.getUsername())
                .id(UUID.randomUUID().toString())
                .claim("uid", userPrincipal.getId())
                .claim("role", userPrincipal.getRole())
                .claim("tokenType", tokenType)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * Validate an access token.
     *
     * @param token the token to validate
     * @return true if valid, false otherwise
     */
    public boolean isTokenValid(String token) {
        try {
            Claims claims = extractAllClaims(token);
            String tokenType = claims.get("tokenType", String.class);
            return "ACCESS".equals(tokenType) && !isTokenExpired(claims);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Validate a refresh token.
     *
     * @param token the token to validate
     * @return true if valid, false otherwise
     */
    public boolean isRefreshTokenValid(String token) {
        try {
            Claims claims = extractAllClaims(token);
            String tokenType = claims.get("tokenType", String.class);
            return "REFRESH".equals(tokenType) && !isTokenExpired(claims);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Extract user principal from a token.
     *
     * @param token the JWT token
     * @return the user principal
     */
    public JwtUserPrincipal extractUserPrincipal(String token) {
        Claims claims = extractAllClaims(token);
        return JwtUserPrincipal.fromClaims(claims);
    }

    /**
     * Extract username (email) from a token.
     *
     * @param token the JWT token
     * @return the username
     */
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    /**
     * Extract user ID from a token.
     *
     * @param token the JWT token
     * @return the user ID
     */
    public Long extractUserId(String token) {
        return extractClaim(token, claims -> claims.get("uid", Long.class));
    }

    /**
     * Extract role from a token.
     *
     * @param token the JWT token
     * @return the role
     */
    public String extractRole(String token) {
        return extractClaim(token, claims -> claims.get("role", String.class));
    }

    /**
     * Extract expiration date from a token.
     *
     * @param token the JWT token
     * @return the expiration date
     */
    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    /**
     * Extract JWT ID from a token (for token revocation).
     *
     * @param token the JWT token
     * @return the JWT ID
     */
    public String extractJti(String token) {
        return extractClaim(token, Claims::getId);
    }

    /**
     * Extract a specific claim from a token.
     *
     * @param token the JWT token
     * @param claimsResolver the function to extract the claim
     * @param <T> the claim type
     * @return the claim value
     */
    private <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    /**
     * Extract all claims from a token.
     *
     * @param token the JWT token
     * @return the claims
     */
    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * Check if a token is expired.
     *
     * @param claims the JWT claims
     * @return true if expired, false otherwise
     */
    private boolean isTokenExpired(Claims claims) {
        return claims.getExpiration().before(new Date());
    }

    /**
     * Get the currently authenticated user from SecurityContext.
     *
     * @return the current user principal
     * @throws IllegalStateException if no authenticated user found
     */
    public JwtUserPrincipal getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof JwtUserPrincipal) {
            return (JwtUserPrincipal) authentication.getPrincipal();
        }
        throw new IllegalStateException("No authenticated user found");
    }

    /**
     * Get the currently authenticated user ID.
     *
     * @return the user ID
     * @throws IllegalStateException if no authenticated user found
     */
    public Long getCurrentUserId() {
        return getCurrentUser().getId();
    }

    /**
     * Get the currently authenticated user's email.
     *
     * @return the email
     * @throws IllegalStateException if no authenticated user found
     */
    public String getCurrentEmail() {
        return getCurrentUser().getUsername();
    }

    /**
     * Get the currently authenticated user's role.
     *
     * @return the role
     * @throws IllegalStateException if no authenticated user found
     */
    public String getCurrentRole() {
        return getCurrentUser().getRole();
    }
}
