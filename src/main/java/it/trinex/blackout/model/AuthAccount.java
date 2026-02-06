package it.trinex.blackout.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Builder
@Table(name = "auth-account")
public class AuthAccount {
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Id
    private Long id;

    @Column(nullable = false)
    private String firstName;
    @Column(nullable = false)
    private String lastName;

    // Credentials
    @Column(unique = true)
    private String username;
    @Column(unique = true)
    private String email;
    @Column(nullable = false)
    private String passwordHash;
    @Column(nullable = false)
    private boolean isActive;

    @Column(unique = true, nullable = true)
    private String totpSecret;

    @ElementCollection
    @CollectionTable(name = "recovery_codes", joinColumns = @JoinColumn(name = "auth_account_id"))
    private List<String> recoveryCodes;

    // Audit fields
    private Instant createdAt;
    private Instant updatedAt;
    private Instant deletedAt;
    
    

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }

    public void setDeleted() {
        deletedAt = Instant.now();
    }
}
