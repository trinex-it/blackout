package it.trinex.blackout.repository;

import it.trinex.blackout.model.AuthAccount;
import it.trinex.blackout.model.Passkey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

public interface PasskeyRepository extends JpaRepository<Passkey, Long> {
    
    Optional<Passkey> findByCredentialId(String credentialId);
    
    List<Passkey> findByAuthAccount(AuthAccount authAccount);

    boolean existsByAuthAccount(AuthAccount authAccount);

    boolean existsByCredentialId(String credentialId);
}

