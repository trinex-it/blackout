package it.trinex.nnh.repository;

import it.trinex.nnh.model.AuthAccount;
import it.trinex.nnh.model.Owner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * JPA Repository for Owner entities.
 *
 * <p>This repository is conditionally loaded based on the property {@code nnh.security.use-jpa}.
 * Set the property to {@code false} to use a custom UserDetailsService instead.</p>
 */
@Repository
@ConditionalOnProperty(prefix = "nnh.security", name = "use-jpa", havingValue = "true", matchIfMissing = true)
public interface OwnerRepository extends JpaRepository<Owner, Long> {

    /**
     * Find an owner by their associated auth account.
     *
     * @param authAccount the auth account
     * @return the owner, if found
     */
    Optional<Owner> findByAuthAccount(AuthAccount authAccount);

    /**
     * Find an owner by the auth account's email.
     *
     * @param email the email address
     * @return the owner, if found
     */
    Optional<Owner> findByAuthAccount_Email(String email);

    /**
     * Check if an owner exists by fiscal code.
     *
     * @param fiscalCode the fiscal code
     * @return true if exists, false otherwise
     */
    boolean existsByFiscalCode(String fiscalCode);

    /**
     * Check if an owner exists by phone number.
     *
     * @param phoneNumber the phone number
     * @return true if exists, false otherwise
     */
    boolean existsByPhoneNumber(String phoneNumber);
}
