package it.trinex.blackout.service.redis;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class RealRedisService implements RedisService {

    private static final String REVOKED_REFRESH_KEY_PREFIX = "revoked:refresh:";
    private static final String REVOKED_ACCESS_KEY_PREFIX = "revoked:access:";
    private static final String USER_TOKENS_KEY_PREFIX = "user_tokens:";

    private final RedisTemplate<String, String> redisTemplate;

    @PostConstruct
    public void init() {
        log.info("Redis configuration found. Initialized Redis Service");
    }

    /**
     * Revokes a refresh token by storing its JTI in Redis blacklist.
     * Token will auto-expire from Redis when the token itself expires.
     *
     * @param jti token ID from JWT
     * @param expiresAt token expiration date
     */
    public void revokeRefreshToken(String jti, Date expiresAt) {
        try {
            String key = REVOKED_REFRESH_KEY_PREFIX + jti;
            long ttlSeconds = calculateTTL(expiresAt);

            if (ttlSeconds > 0) {
                redisTemplate.opsForValue().set(key, "revoked", Duration.ofSeconds(ttlSeconds));
                log.debug("Revoked refresh token: {} (TTL: {}s)", jti, ttlSeconds);
            } else {
                log.debug("Token already expired, skipping revocation: {}", jti);
            }
        } catch (Exception e) {
            log.warn("Failed to revoke refresh token in Redis (graceful degradation): {}", e.getMessage());
        }
    }

    /**
     * Revokes an access token (for critical scenarios like compromised accounts).
     *
     * @param jti token ID from JWT
     * @param expiresAt token expiration date
     */
    public void revokeAccessToken(String jti, Date expiresAt) {
        try {
            String key = REVOKED_ACCESS_KEY_PREFIX + jti;
            long ttlSeconds = calculateTTL(expiresAt);

            if (ttlSeconds > 0) {
                redisTemplate.opsForValue().set(key, "revoked", Duration.ofSeconds(ttlSeconds));
                log.debug("Revoked access token: {} (TTL: {}s)", jti, ttlSeconds);
            }
        } catch (Exception e) {
            log.warn("Failed to revoke access token in Redis: {}", e.getMessage());
        }
    }

    /**
     * Checks if a refresh token is revoked.
     *
     * @param jti token ID from JWT
     * @return true if token is revoked, false otherwise
     */
    public boolean isRefreshTokenRevoked(String jti) {
        try {
            String key = REVOKED_REFRESH_KEY_PREFIX + jti;
            return Boolean.TRUE.equals(redisTemplate.hasKey(key));
        } catch (Exception e) {
            // CRITICAL: Redis failure means revoked tokens will be accepted!
            // This is a FAIL-OPEN security vulnerability that should trigger alerts.
            log.error("[CRITICAL_ALERT] Redis connection failure in token revocation check - REVOKED TOKENS MAY BE ACCEPTED! " +
                    "jti={}, error={}, failOpen=true", jti, e.getMessage());
            // TODO: Consider fail-closed approach or circuit breaker pattern
            return false; // Fail open: allow request if Redis is down (SECURITY RISK)
        }
    }

    /**
     * Checks if an access token is revoked.
     *
     * @param jti token ID from JWT
     * @return true if token is revoked, false otherwise
     */
    public boolean isAccessTokenRevoked(String jti) {
        try {
            String key = REVOKED_ACCESS_KEY_PREFIX + jti;
            return Boolean.TRUE.equals(redisTemplate.hasKey(key));
        } catch (Exception e) {
            // CRITICAL: Redis failure means revoked tokens will be accepted!
            // This is a FAIL-OPEN security vulnerability that should trigger alerts.
            log.error("[CRITICAL_ALERT] Redis connection failure in access token revocation check - REVOKED TOKENS MAY BE ACCEPTED! " +
                    "jti={}, error={}, failOpen=true", jti, e.getMessage());
            // TODO: Consider fail-closed approach or circuit breaker pattern
            return false; // Fail open: allow request if Redis is down (SECURITY RISK)
        }
    }

    /**
     * Tracks a token for a user in Redis Set.
     * This allows bulk revocation of all user tokens (e.g., on password change).
     *
     * @param userId user's unique identifier
     * @param jti token ID from JWT
     * @param expiresAt token expiration date
     * @param tokenType "access" or "refresh"
     */
    public void trackUserToken(Long userId, String jti, Date expiresAt, String tokenType) {
        try {
            String key = USER_TOKENS_KEY_PREFIX + userId;
            String tokenData = jti + ":" + tokenType + ":" + expiresAt.getTime();

            long ttlSeconds = calculateTTL(expiresAt);
            if (ttlSeconds > 0) {
                redisTemplate.opsForSet().add(key, tokenData);
                // Set TTL on the set (will be extended if more tokens are added)
                redisTemplate.expire(key, Duration.ofSeconds(ttlSeconds));
                log.debug("Tracked {} token for user {}: {} (TTL: {}s)", tokenType, userId, jti, ttlSeconds);
            }
        } catch (Exception e) {
            log.warn("Failed to track token in Redis (graceful degradation): {}", e.getMessage());
        }
    }

    /**
     * Removes a specific token from user's tracking set.
     * Called when token is individually revoked or expires.
     *
     * @param userId user's unique identifier
     * @param jti token ID to remove
     * @param tokenType "access" or "refresh"
     */
    public void removeUserToken(Long userId, String jti, String tokenType) {
        try {
            String key = USER_TOKENS_KEY_PREFIX + userId;
            Set<String> tokens = redisTemplate.opsForSet().members(key);

            if (tokens != null) {
                // Find and remove the token entry
                for (String tokenData : tokens) {
                    if (tokenData.startsWith(jti + ":" + tokenType + ":")) {
                        redisTemplate.opsForSet().remove(key, tokenData);
                        log.debug("Removed {} token from user {}: {}", tokenType, userId, jti);
                        break;
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Failed to remove token from user tracking in Redis: {}", e.getMessage());
        }
    }

    /**
     * Retrieves all active tokens for a user.
     *
     * @param userId user's unique identifier
     * @return set of token data strings in format "jti:type:expiration"
     */
    public Set<String> getUserTokens(Long userId) {
        try {
            String key = USER_TOKENS_KEY_PREFIX + userId;
            Set<String> tokens = redisTemplate.opsForSet().members(key);
            return tokens != null ? tokens : new HashSet<>();
        } catch (Exception e) {
            log.error("Failed to retrieve user tokens from Redis: {}", e.getMessage());
            return new HashSet<>();
        }
    }

    /**
     * Revokes all active tokens for a user.
     * Used for force logout scenarios (password change, account compromise, etc.).
     *
     * @param userId user's unique identifier
     * @return number of tokens revoked
     */
    public int revokeAllUserTokens(Long userId) {
        try {
            String key = USER_TOKENS_KEY_PREFIX + userId;
            Set<String> tokens = redisTemplate.opsForSet().members(key);

            if (tokens == null || tokens.isEmpty()) {
                log.info("No active tokens found for user {}", userId);
                return 0;
            }

            int revokedCount = 0;
            for (String tokenData : tokens) {
                try {
                    // Parse token data: "jti:type:expiration"
                    String[] parts = tokenData.split(":");
                    if (parts.length != 3) {
                        log.warn("Invalid token data format: {}", tokenData);
                        continue;
                    }

                    String jti = parts[0];
                    String type = parts[1];
                    long expirationMs = Long.parseLong(parts[2]);
                    Date expiresAt = new Date(expirationMs);

                    // Revoke the token
                    if ("access".equals(type)) {
                        revokeAccessToken(jti, expiresAt);
                    } else if ("refresh".equals(type)) {
                        revokeRefreshToken(jti, expiresAt);
                    }

                    revokedCount++;
                } catch (Exception e) {
                    log.warn("Failed to revoke individual token {}: {}", tokenData, e.getMessage());
                }
            }

            // Clear the user's token tracking set
            redisTemplate.delete(key);

            log.info("Revoked {} tokens for user {}", revokedCount, userId);
            return revokedCount;
        } catch (Exception e) {
            log.error("Failed to revoke all user tokens in Redis: {}", e.getMessage());
            return 0;
        }
    }

    /**
     * Calculates TTL in seconds until token expires.
     */
    private long calculateTTL(Date expiresAt) {
        long now = System.currentTimeMillis();
        long expiry = expiresAt.getTime();
        return (expiry - now) / 1000;
    }

}
