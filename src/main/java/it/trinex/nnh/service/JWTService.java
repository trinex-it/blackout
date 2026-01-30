package it.trinex.nnh.service;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import it.trinex.nnh.model.AuthAccount;
import it.trinex.nnh.properties.JwtProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.security.Key;
import java.time.Instant;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class JWTService {

    private static final String TOKEN_TYPE_CLAIM = "tokenType";
    private static final String TOKEN_TYPE_ACCESS = "ACCESS";
    private static final String TOKEN_TYPE_REFRESH = "REFRESH";

    private static final String CLAIM_UID = "uid";
    private static final String CLAIM_ROLE = "role";
    private static final String CLAIM_OWNER_ID = "ownerId";

    private final JwtProperties jwtProperties;
    // ========================================
    // TOKEN GENERATION
    // ========================================

    /**
     * Generates an access token for the given user principal.
     * Token expiration is determined by the user's role (AuthAccountType).
     * The token is automatically tracked for the user in Redis for revocation
     * support.
     *
     * @param userPrincipal the authenticated user
     * @return JWT access token string
     */
    public String generateAccessToken(AuthAccount userPrincipal) {
        String role = extractRoleFromAuthorities(userPrincipal.getAuthorities());
        long expirationMs = jwtProperties.getAccessTokenExp();
        String token = buildToken(userPrincipal, expirationMs, TOKEN_TYPE_ACCESS);

        // Track token for user-level revocation (e.g., password change)
        String jti = extractJti(token);
        Date expiration = extractExpiration(token);
        return token;
    }

    /**
     * Generates a refresh token for the given user principal.
     * Refresh tokens have longer expiration times than access tokens.
     * The token is automatically tracked for the user in Redis for revocation
     * support.
     *
     * @param userPrincipal the authenticated user
     * @return JWT refresh token string
     */
    public String generateRefreshToken(AuthAccount userPrincipal) {
        String role = extractRoleFromAuthorities(userPrincipal.getAuthorities());
        long expirationMs = jwtProperties.getRefreshTokenExp();
        String token = buildToken(userPrincipal, expirationMs, TOKEN_TYPE_REFRESH);

        // Track token for user-level revocation (e.g., password change)
        String jti = extractJti(token);
        Date expiration = extractExpiration(token);
        return token;
    }

    /**
     * Builds a JWT token with all user principal information as claims.
     * Includes a unique JTI (JWT ID) for token revocation support.
     */
    private String buildToken(AuthAccount userPrincipal, long expirationMs, String tokenType) {
        Instant now = Instant.now();
        Instant expiration = now.plusMillis(expirationMs);
        var builder = Jwts.builder()
                .subject(userPrincipal.getSubject())
                .id(userPrincipal.getId().toString())
                .claim("token_type", tokenType)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiration))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256);

        userPrincipal.getClaims().forEach(builder::claim);

        return builder.compact();
    }

    // ========================================
    // TOKEN VALIDATION
    // ========================================

    /**
     * Validates if the token is a valid access token (not expired, valid
     * signature).
     *
     * @param token the JWT token to validate
     * @return true if token is valid, false otherwise
     */
    public boolean isTokenValid(String token) {
        try {
            Claims claims = extractAllClaims(token);
            String tokenType = claims.get(TOKEN_TYPE_CLAIM, String.class);
            return TOKEN_TYPE_ACCESS.equals(tokenType) && !isTokenExpired(claims);
        } catch (ExpiredJwtException e) {
            log.debug("Token expired: {}", e.getMessage());
            return false;
        } catch (SignatureException | MalformedJwtException | UnsupportedJwtException | IllegalArgumentException e) {
            log.warn("Invalid token: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Validates if the token is a valid refresh token (not expired, valid
     * signature).
     *
     * @param token the JWT token to validate
     * @return true if refresh token is valid, false otherwise
     */
    public boolean isRefreshTokenValid(String token) {
        try {
            Claims claims = extractAllClaims(token);
            String tokenType = claims.get(TOKEN_TYPE_CLAIM, String.class);
            return TOKEN_TYPE_REFRESH.equals(tokenType) && !isTokenExpired(claims);
        } catch (ExpiredJwtException e) {
            log.debug("Refresh token expired: {}", e.getMessage());
            return false;
        } catch (SignatureException | MalformedJwtException | UnsupportedJwtException | IllegalArgumentException e) {
            log.warn("Invalid refresh token: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Checks if a token is expired based on its claims.
     */
    private boolean isTokenExpired(Claims claims) {
        Date expiration = claims.getExpiration();
        return expiration.before(new Date());
    }

    // ========================================
    // EXTRACT USER PRINCIPAL
    // ========================================

    /**
     * Extracts the full JWTUserPrincipal from a JWT token.
     * This reconstructs the user principal from all claims in the token.
     * Use this method to get user information from a token on the client side
     * or when you need to validate a token and get the user information.
     *
     * @param token the JWT token
     * @return JWTUserPrincipal containing all user information
     * @throws RuntimeException if token is invalid or expired
     */
    public AuthAccount extractUserPrincipal(String token) {
        Claims claims = extractAllClaims(token);

        String subject = claims.getSubject();
        Long id = extractLongClaim(claims, CLAIM_UID);
        String role = claims.get(CLAIM_ROLE, String.class);
        String firstName = claims.get("firstName", String.class);
        String lastName = claims.get("lastName", String.class);

        // Provide both the ROLE_ prefixed authority (used by hasRole checks) and the
        // plain
        // authority (used by hasAuthority checks in this codebase). This ensures
        // compatibility
        // with both kinds of SpEL checks.
        List<SimpleGrantedAuthority> authorities = List.of(
                new SimpleGrantedAuthority("ROLE_" + role),
                new SimpleGrantedAuthority(role));

       return new AuthAccount(
               id,
               role,
               subject,
               null,
               true,
               null,
               null,
               null
       ) {
           @Override
           public Map<String, Object> getClaims() {
               return Map.of();
           }
       };

    }



    /**
     * Extracts the user's role (AuthAccountType) from a token.
     */
    public String extractRole(String token) {
        return extractAllClaims(token).get(CLAIM_ROLE, String.class);
    }

    /**
     * Extracts the expiration date from a token.
     */
    public Date extractExpiration(String token) {
        return extractAllClaims(token).getExpiration();
    }

    /**
     * Extracts the JTI (JWT ID) from a token for revocation purposes.
     */
    public String extractJti(String token) {
        return extractAllClaims(token).getId();
    }

    // ========================================
    // HELPER METHODS
    // ========================================

    /**
     * Extracts all claims from a JWT token.
     */
    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith((SecretKey) getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * Extracts a Long value from claims, handling both Number and String types.
     */
    private Long extractLongClaim(Claims claims, String claimName) {
        Object value = claims.get(claimName);
        switch (value) {
            case null -> {
                return null;
            }
            case Number number -> {
                return number.longValue();
            }
            case String string -> {
                try {
                    return Long.parseLong(string);
                } catch (NumberFormatException e) {
                    log.warn("Could not parse claim {} as Long: {}", claimName, value);
                    return null;
                }
            }
            default -> {
            }
        }
        return null;
    }

    /**
     * Extracts AuthAccountType from Spring Security authorities.
     * Authorities are expected to contain a ROLE_ prefixed authority (e.g.
     * "ROLE_ADMIN").
     */
    private String extractRoleFromAuthorities(Collection<? extends GrantedAuthority> authorities) {
        String authority = authorities.stream()
                .map(GrantedAuthority::getAuthority)
                .filter(auth -> auth.startsWith("ROLE_"))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No valid role found in authorities"));

        // Remove "ROLE_" prefix
        return authority.substring(5);
    }

    /**
     * Gets the signing key for JWT token signing and verification.
     */
    private Key getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(jwtProperties.getSecret());
        return Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * Calculates the expiration instant for an access token based on role.
     */
    public Instant calculateAccessTokenExpiration() {
        long expirationMs = jwtProperties.getAccessTokenExp();
        return Instant.now().plusMillis(expirationMs);
    }

    /**
     * Calculates the expiration instant for a refresh token based on role.
     */
    public Instant calculateRefreshTokenExpiration() {
        long expirationMs = jwtProperties.getRefreshTokenExp();
        return Instant.now().plusMillis(expirationMs);
    }
}
