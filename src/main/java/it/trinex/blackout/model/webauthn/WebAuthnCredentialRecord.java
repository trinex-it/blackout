package it.trinex.blackout.model.webauthn;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.security.web.webauthn.api.AuthenticatorTransport;

import java.util.Set;

@Entity
@Table(name = "webauthn_credential_record")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WebAuthnCredentialRecord {

    @Id
    @Column(columnDefinition = "VARBINARY(255)", updatable = false, nullable = false)
    private byte[] credentialId;

    @Column(columnDefinition = "BLOB", nullable = false)
    private byte[] userEntityUserId;

    @Column(columnDefinition = "BLOB", nullable = false)
    private byte[] publicKey;

    @Column(nullable = false)
    private long signatureCount;

    private boolean uvInitialized;

    private boolean backupEligible;

    private boolean backupState;

    @Column(columnDefinition = "BLOB")
    private byte[] attestationObject;

    @Column(columnDefinition = "BLOB")
    private byte[] attestationClientDataJSON;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "webauthn_credential_transports", joinColumns = @JoinColumn(name = "credential_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "transport")
    private Set<AuthenticatorTransport> transports;

    @Column(nullable = false)
    private String credentialType;
}
