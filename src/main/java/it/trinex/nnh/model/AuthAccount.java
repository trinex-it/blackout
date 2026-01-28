package it.trinex.nnh.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Base authentication account entity.
 *
 * <p>This entity represents a user's authentication credentials and account status.
 * It can be extended with profile-specific entities like {@link Owner}.</p>
 *
 * <p>Key features:</p>
 * <ul>
 *   <li>Unique email constraint</li>
 *   <li>Account type (OWNER, ADMIN)</li>
 *   <li>Active status for account activation/deactivation</li>
 *   <li>Audit timestamps</li>
 * </ul>
 */
@Entity
@Table(name = "auth_accounts")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Account type (OWNER, ADMIN, etc.).
     * Determines the user's role and permissions.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AuthAccountType type;

    /**
     * User's email address (used as username).
     * Must be unique across all accounts.
     */
    @Column(unique = true, nullable = false, length = 255)
    private String email;

    /**
     * Password hash (BCrypt).
     * Never store plain text passwords.
     */
    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    /**
     * TOTP secret for two-factor authentication (future feature).
     */
    @Column(name = "totp_secret")
    private String totpSecret;

    /**
     * Account active status.
     * Inactive accounts cannot authenticate.
     */
    @Column(nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    /**
     * Account creation timestamp.
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Account last update timestamp.
     */
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /**
     * Password last changed timestamp.
     * Useful for password expiration policies.
     */
    @Column(name = "password_changed_at")
    private LocalDateTime passwordChangedAt;

    /**
     * Last login timestamp.
     */
    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    /**
     * Update the last login timestamp.
     */
    public void updateLastLogin() {
        this.lastLoginAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Update the password hash.
     *
     * @param passwordHash the new password hash
     */
    public void updatePassword(String passwordHash) {
        this.passwordHash = passwordHash;
        this.passwordChangedAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Deactivate the account.
     */
    public void deactivate() {
        this.isActive = false;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Activate the account.
     */
    public void activate() {
        this.isActive = true;
        this.updatedAt = LocalDateTime.now();
    }
}
