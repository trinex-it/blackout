package it.trinex.nnh;


import it.trinex.nnh.model.AuthAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AuthAccountRepository extends JpaRepository<AuthAccount, Long> {

    Optional<AuthAccount> findByEmail(String email);

    Optional<AuthAccount> findFirstByType(AuthAccountType type);
}
