package it.trinex.nnh.model;

/**
 * Enumeration of authentication account types.
 *
 * <p>Each account type can have different permissions and token expiration times.
 * Developers can extend this enum with custom types for their application (e.g., CUSTOMER, VENDOR, EMPLOYEE).</p>
 *
 * <p>These map to Spring Security authorities with the "ROLE_" prefix:</p>
 * <ul>
 *   <li>USER → ROLE_USER</li>
 *   <li>ADMIN → ROLE_ADMIN</li>
 *   <li>CUSTOM → ROLE_CUSTOM</li>
 * </ul>
 *
 * <p><b>Example custom types:</b></p>
 * <pre>
 * public enum AuthAccountType {
 *     USER,
 *     ADMIN,
 *     CUSTOMER,
 *     VENDOR,
 *     EMPLOYEE,
 *     MODERATOR
 * }
 * </pre>
 */
public enum AuthAccountType {
    /**
     * Regular user account.
     * Typically used for end-users of the application.
     */
    USER,

    /**
     * Administrator account.
     * Typically used for system administrators with elevated permissions.
     */
    ADMIN
}
