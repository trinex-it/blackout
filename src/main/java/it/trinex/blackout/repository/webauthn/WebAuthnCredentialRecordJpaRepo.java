package it.trinex.blackout.repository.webauthn;

import it.trinex.blackout.model.webauthn.WebAuthnCredentialRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WebAuthnCredentialRecordJpaRepo extends JpaRepository<WebAuthnCredentialRecord, byte[]> {
    Optional<WebAuthnCredentialRecord> findByCredentialId(byte[] credentialId);
    List<WebAuthnCredentialRecord> findByUserEntityUserId(byte[] userEntityUserId);
}
