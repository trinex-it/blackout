package it.trinex.blackout.repository.webauthn;

import it.trinex.blackout.model.webauthn.WebAuthnUserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface WebAuthnUserEntityJpaRepo extends JpaRepository<WebAuthnUserEntity, byte[]> {
    Optional<WebAuthnUserEntity> findByUsername(String username);
}
