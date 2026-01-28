package it.trinex.nnh.repository;

import it.trinex.nnh.model.AuthAccount;
import it.trinex.nnh.model.AuthAccountType;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * JPA Repository for AuthAccount entities.
 *
 * <p>This repository is conditionally loaded based on the property {@code nnh.security.use-jpa}.
 * Set the property to {@code false} to use a custom UserDetailsService instead.</p>
 */
@Repository
@ConditionalOnProperty(prefix = "nnh.security", name = "use-jpa", havingValue = "true", matchIfMissing = true)
public interface AuthAccountRepository extends JpaRepository<AuthAccount, Long> {

    /**
     * Find an auth account by email.
     *
     * @param email the email address
     * @return the auth account, if found
     */
    Optional<AuthAccount> findByEmail(String email);

    /**
     * Check if an auth account exists by email.
     *
     * @param email the email address
     * @return true if exists, false otherwise
     */
    boolean existsByEmail(String email);

    /**
     * Find the first auth account by type.
     *
     * @param type the account type
     * @return the auth account, if found
     */
    Optional<AuthAccount> findFirstByType(AuthAccountType type);

    /**
     * Find an auth account by email and active status.
     *
     * @param email the email address
     * @param isActive the active status
     * @return the auth account, if found
     */
    Optional<AuthAccount> findByEmailAndIsActive(String email, Boolean isActive);
}
