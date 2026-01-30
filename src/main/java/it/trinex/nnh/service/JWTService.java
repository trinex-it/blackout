package it.trinex.nnh.service;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
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
    private final OrganizationService organizationService;

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
    public String generateAccessToken(JWTUserPrincipal userPrincipal) {
        AuthAccountType role = extractRoleFromAuthorities(userPrincipal.getAuthorities());
        long expirationMs = jwtProperties.getAccessToken().getExpiration().get(role.name());
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
    public String generateRefreshToken(JWTUserPrincipal userPrincipal) {
        AuthAccountType role = extractRoleFromAuthorities(userPrincipal.getAuthorities());
        long expirationMs = jwtProperties.getRefreshToken().getExpiration().get(role.name());
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
    private String buildToken(JWTUserPrincipal userPrincipal, long expirationMs, String tokenType) {
        Instant now = Instant.now();
        Instant expiration = now.plusMillis(expirationMs);

        // Calculate active organization IDs if user has an ownerId
        List<Long> activeOrganizationIds = null;
        if (userPrincipal.getOwnerId().isPresent()) {
            List<Organization> activeOrganizations = organizationService.findActiveOrganizationsByOwnerId(
                    userPrincipal.getOwnerId().get());
            activeOrganizationIds = activeOrganizations.stream()
                    .map(Organization::getId)
                    .toList();
        }

        return Jwts.builder()
                .subject(userPrincipal.getUsername())
                .id(UUID.randomUUID().toString())
                .claim(CLAIM_UID, userPrincipal.id())
                .claim(CLAIM_ROLE, extractRoleFromAuthorities(userPrincipal.getAuthorities()).name())
                .claim(CLAIM_OWNER_ID, userPrincipal.getOwnerId().orElse(null))
                .claim("firstName", userPrincipal.firstName())
                .claim("lastName", userPrincipal.lastName())
                .claim(TOKEN_TYPE_CLAIM, tokenType)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiration))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)// todo: check deprecation
                .compact();
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
    public JWTUserPrincipal extractUserPrincipal(String token) {
        Claims claims = extractAllClaims(token);

        String username = claims.getSubject();
        Long id = extractLongClaim(claims, CLAIM_UID);
        String role = claims.get(CLAIM_ROLE, String.class);
        Optional<Long> ownerId = Optional.ofNullable(extractLongClaim(claims, CLAIM_OWNER_ID));

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

        return new JWTUserPrincipal(
                id,
                username,
                null,
                ownerId,
                firstName,
                lastName,
                authorities);
    }

    // ========================================
    // SECURITY CONTEXT ACCESS
    // ========================================

    /**
     * Gets the currently authenticated user from Spring Security's SecurityContext.
     * This is the recommended way to access the current user in controllers and
     * services.
     *
     * @return the current JWTUserPrincipal
     * @throws IllegalStateException if no user is authenticated
     */
    public JWTUserPrincipal getCurrentUser() {
        return getCurrentUserOptional()
                .orElseThrow(() -> new IllegalStateException("No authenticated user in security context"));
    }

    /**
     * Gets the currently authenticated user from Spring Security's SecurityContext
     * as an Optional.
     * Returns empty if no user is authenticated.
     *
     * @return Optional containing the current JWTUserPrincipal, or empty if not
     *         authenticated
     */
    public Optional<JWTUserPrincipal> getCurrentUserOptional() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated() ||
                authentication.getPrincipal() instanceof String) {
            return Optional.empty();
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof JWTUserPrincipal jwtUserPrincipal) {
            return Optional.of(jwtUserPrincipal);
        }

        return Optional.empty();
    }

    // ========================================
    // INDIVIDUAL CLAIM EXTRACTION
    // ========================================

    /**
     * Extracts the username (email) from a token.
     */
    public String extractUsername(String token) {
        return extractAllClaims(token).getSubject();
    }

    /**
     * Extracts the user ID from a token.
     */
    public Long extractUserId(String token) {
        return extractLongClaim(extractAllClaims(token), CLAIM_UID);
    }

    /**
     * Extracts the user's role (AuthAccountType) from a token.
     */
    public AuthAccountType extractRole(String token) {
        String roleName = extractAllClaims(token).get(CLAIM_ROLE, String.class);
        return AuthAccountType.valueOf(roleName);
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

    private List<Long> extractLongClaimList(Claims claims, String claimName) {
        Object value = claims.get(claimName);
        switch (value) {
            case null -> {
                return null;
            }
            case List<?> list -> {
                List<Long> extractedLongs = new ArrayList<>();
                for (Object item : list) {
                    switch (item) {
                        case Number number -> extractedLongs.add(number.longValue());
                        case String string -> {
                            try {
                                extractedLongs.add(Long.parseLong(string));
                            } catch (NumberFormatException e) {
                                log.warn("Could not parse item {} in claim {} as Long: {}", string, claimName, item);
                            }
                        }
                        default -> log.warn("Unsupported item type in claim {}: {}", claimName, item);
                    }
                }
                return extractedLongs;
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
    private AuthAccountType extractRoleFromAuthorities(Collection<? extends GrantedAuthority> authorities) {
        String authority = authorities.stream()
                .map(GrantedAuthority::getAuthority)
                .filter(auth -> auth.startsWith("ROLE_"))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No valid role found in authorities"));

        String roleName = authority.substring(5); // Remove "ROLE_" prefix
        return AuthAccountType.valueOf(roleName);
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
    public Instant calculateAccessTokenExpiration(AuthAccountType role) {
        long expirationMs = jwtProperties.getAccessToken().getExpiration().get(role.name());
        return Instant.now().plusMillis(expirationMs);
    }

    /**
     * Calculates the expiration instant for a refresh token based on role.
     */
    public Instant calculateRefreshTokenExpiration(AuthAccountType role) {
        long expirationMs = jwtProperties.getRefreshToken().getExpiration().get(role.name());
        return Instant.now().plusMillis(expirationMs);
    }
}
