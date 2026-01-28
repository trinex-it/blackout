package it.trinex.nnh.model;

/**
 * Enumeration of authentication account types.
 *
 * <p>Each account type can have different permissions and token expiration times.</p>
 *
 * <p>These map to Spring Security authorities as:</p>
 * <ul>
 *   <li>OWNER → ROLE_OWNER</li>
 *   <li>ADMIN → ROLE_ADMIN</li>
 * </ul>
 */
public enum AuthAccountType {
    /**
     * Regular owner account.
     * Typically used for end-users of the application.
     */
    OWNER,

    /**
     * Administrator account.
     * Typically used for system administrators with elevated permissions.
     */
    ADMIN
}
