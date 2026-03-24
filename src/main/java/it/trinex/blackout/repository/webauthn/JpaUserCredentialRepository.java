package it.trinex.blackout.repository.webauthn;

import it.trinex.blackout.model.webauthn.WebAuthnCredentialRecord;
import lombok.RequiredArgsConstructor;
import org.springframework.security.web.webauthn.api.Bytes;
import org.springframework.security.web.webauthn.api.CredentialRecord;
import org.springframework.security.web.webauthn.api.ImmutableCredentialRecord;
import org.springframework.security.web.webauthn.api.ImmutablePublicKeyCose;
import org.springframework.security.web.webauthn.api.PublicKeyCredentialType;
import org.springframework.security.web.webauthn.management.UserCredentialRepository;

import java.util.List;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class JpaUserCredentialRepository implements UserCredentialRepository {

    private final WebAuthnCredentialRecordJpaRepo repo;

    @Override
    public void delete(Bytes credentialId) {
        repo.deleteById(credentialId.getBytes());
    }

    @Override
    public void save(CredentialRecord credentialRecord) {
        WebAuthnCredentialRecord entity = WebAuthnCredentialRecord.builder()
                .credentialId(credentialRecord.getCredentialId().getBytes())
                .userEntityUserId(credentialRecord.getUserEntityUserId().getBytes())
                .publicKey(credentialRecord.getPublicKey().getBytes())
                .signatureCount(credentialRecord.getSignatureCount())
                .uvInitialized(credentialRecord.isUvInitialized())
                .backupEligible(credentialRecord.isBackupEligible())
                .backupState(credentialRecord.isBackupState())
                .attestationObject(credentialRecord.getAttestationObject() != null ? credentialRecord.getAttestationObject().getBytes() : null)
                .attestationClientDataJSON(credentialRecord.getAttestationClientDataJSON() != null ? credentialRecord.getAttestationClientDataJSON().getBytes() : null)
                .transports(credentialRecord.getTransports())
                .credentialType(credentialRecord.getCredentialType().getValue())
                .build();
        repo.save(entity);
    }

    @Override
    public CredentialRecord findByCredentialId(Bytes credentialId) {
        return repo.findByCredentialId(credentialId.getBytes())
                .map(this::toRecord)
                .orElse(null);
    }

    @Override
    public List<CredentialRecord> findByUserId(Bytes userId) {
        return repo.findByUserEntityUserId(userId.getBytes()).stream()
                .map(this::toRecord)
                .collect(Collectors.toList());
    }

    private CredentialRecord toRecord(WebAuthnCredentialRecord entity) {
        return ImmutableCredentialRecord.builder()
                .credentialId(new Bytes(entity.getCredentialId()))
                .userEntityUserId(new Bytes(entity.getUserEntityUserId()))
                .publicKey(new ImmutablePublicKeyCose(entity.getPublicKey()))
                .signatureCount(entity.getSignatureCount())
                .uvInitialized(entity.isUvInitialized())
                .backupEligible(entity.isBackupEligible())
                .backupState(entity.isBackupState())
                .attestationObject(entity.getAttestationObject() != null ? new Bytes(entity.getAttestationObject()) : null)
                .attestationClientDataJSON(entity.getAttestationClientDataJSON() != null ? new Bytes(entity.getAttestationClientDataJSON()) : null)
                .transports(entity.getTransports())
                .credentialType(PublicKeyCredentialType.PUBLIC_KEY)
                .build();
    }
}
