package it.trinex.nnh;

import it.trinex.nnh.model.AuthAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AuthAccountRepo extends JpaRepository<AuthAccount, Long> {
    public Optional<AuthAccount> findByUsername(String username);
}
