package it.trinex.blackout.service.redis;

import java.util.Date;
import java.util.Set;

public interface RedisService {

    /**
     * Revokes a refresh token by storing its JTI in Redis blacklist.
     * Token will auto-expire from Redis when the token itself expires.
     *
     * @param jti token ID from JWT
     * @param expiresAt token expiration date
     */
    void revokeRefreshToken(String jti, Date expiresAt);

    /**
     * Revokes an access token (for critical scenarios like compromised accounts).
     *
     * @param jti token ID from JWT
     * @param expiresAt token expiration date
     */
    void revokeAccessToken(String jti, Date expiresAt);

    /**
     * Checks if a refresh token is revoked.
     *
     * @param jti token ID from JWT
     * @return true if token is revoked, false otherwise
     */
    boolean isRefreshTokenRevoked(String jti);

    /**
     * Checks if an access token is revoked.
     *
     * @param jti token ID from JWT
     * @return true if token is revoked, false otherwise
     */
    boolean isAccessTokenRevoked(String jti);

    /**
     * Tracks a token for a user in Redis Set.
     * This allows bulk revocation of all user tokens (e.g., on password change).
     *
     * @param authAccountId user's unique identifier
     * @param jti token ID from JWT
     * @param expiresAt token expiration date
     * @param tokenType "access" or "refresh"
     */
    void trackUserToken(Long authAccountId, String jti, Date expiresAt, String tokenType);

    void trackChallenge(String sessionId, String challenge);

    /**
     * Removes a specific token from user's tracking set.
     * Called when token is individually revoked or expires.
     *
     * @param authAccountId user's unique identifier
     * @param jti token ID to remove
     * @param tokenType "access" or "refresh"
     */
    void removeUserToken(Long authAccountId, String jti, String tokenType);

    /**
     * Retrieves all active tokens for a user.
     *
     * @param authAccountId user's unique identifier
     * @return set of token data strings in format "jti:type:expiration"
     */
    Set<String> getUserTokens(Long authAccountId);

    /**
     * Revokes all active tokens for a user.
     * Used for force logout scenarios (password change, account compromise, etc.).
     *
     * @param authAccountId user's AuthAccount identifier
     * @return number of tokens revoked
     */
    int revokeAllUserTokens(Long authAccountId);
}
