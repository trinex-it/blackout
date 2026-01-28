package it.trinex.nnh.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Owner profile entity.
 *
 * <p>This entity demonstrates the pattern of extending AuthAccount with profile-specific information.
 * Users can create similar entities for different user types (e.g., Customer, Employee, etc.).</p>
 *
 * <p>This entity has a one-to-one relationship with AuthAccount.</p>
 */
@Entity
@Table(name = "owners")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Owner {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Associated authentication account.
     * One-to-one relationship with cascade operations.
     */
    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "auth_account_id", nullable = false, unique = true)
    private AuthAccount authAccount;

    /**
     * Owner's first name.
     */
    @NotBlank
    @Size(max = 100)
    @Column(nullable = false, length = 100)
    private String firstName;

    /**
     * Owner's last name.
     */
    @NotBlank
    @Size(max = 100)
    @Column(nullable = false, length = 100)
    private String lastName;

    /**
     * Italian fiscal code (codice fiscale).
     * Must follow the pattern: 6 letters, 2 digits, 1 letter, 2 digits, 1 letter, 2 digits, 3 letters, 1 letter.
     * Example: RSSMRA80A01H501U
     */
    @NotBlank
    @Column(unique = true, nullable = false, length = 16)
    private String fiscalCode;

    /**
     * Phone number.
     * Must be unique across all owners.
     */
    @NotBlank
    @Column(unique = true, nullable = false, length = 20)
    private String phoneNumber;

    /**
     * Profile creation timestamp.
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Profile last update timestamp.
     */
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

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
     * Get the owner's full name.
     *
     * @return the full name
     */
    public String getFullName() {
        return firstName + " " + lastName;
    }

    /**
     * Get the email from the associated auth account.
     *
     * @return the email
     */
    public String getEmail() {
        return authAccount != null ? authAccount.getEmail() : null;
    }
}
