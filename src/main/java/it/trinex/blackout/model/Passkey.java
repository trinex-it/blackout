package it.trinex.blackout.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "passkeys")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Passkey {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(unique = true, nullable = false, length = 500)
    private String credentialId; // Base64 URL encoded credential ID
    
    @Column(nullable = false, length = 2000)
    private String publicKey; // Base64 encoded public key
    
    @Column(nullable = false)
    private Long signCount;
    
    @Column(nullable = false)
    private String aaguid; // Authenticator AAGUID
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "authAccount_id", nullable = false)
    private AuthAccount authAccount;

    private String deviceName; // e.g., "MacBook Pro", "iPhone 14"
    
    @Column(nullable = false)
    private LocalDateTime createdAt;
    
    private LocalDateTime lastUsedAt;
    
    @Column(nullable = false)
    private boolean enabled = true;
    
    // Transports (e.g., usb, nfc, ble, internal)
    @Column(length = 500)
    private String transports;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (signCount == null) {
            signCount = 0L;
        }
    }
}

