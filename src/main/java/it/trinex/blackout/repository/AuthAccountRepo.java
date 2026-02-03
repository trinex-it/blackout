package it.trinex.blackout.repository;

import it.trinex.blackout.model.AuthAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AuthAccountRepo extends JpaRepository<AuthAccount, Long> {
    public Optional<AuthAccount> findByUsername(String username);
}
