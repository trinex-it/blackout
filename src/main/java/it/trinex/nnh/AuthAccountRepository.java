package it.trinex.nnh;

import it.trinex.queuerbe.model.AuthAccount;
import it.trinex.queuerbe.model.AuthAccountType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AuthAccountRepository extends JpaRepository<AuthAccount, Long> {

    Optional<AuthAccount> findByEmail(String email);

    Optional<AuthAccount> findFirstByType(AuthAccountType type);
}
