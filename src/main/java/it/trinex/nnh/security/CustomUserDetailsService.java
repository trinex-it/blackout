package it.trinex.nnh.security;

import it.trinex.nnh.exception.AuthenticationException;
import it.trinex.nnh.model.AuthAccount;
import it.trinex.nnh.repository.AuthAccountRepository;
import it.trinex.nnh.security.jwt.JwtUserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * Custom UserDetailsService that loads users from JPA repositories.
 *
 * <p>This service:</p>
 * <ul>
 *   <li>Loads users by email (not username!)</li>
 *   <li>Checks account active status</li>
 *   <li>Creates JwtUserPrincipal with authorities</li>
 * </ul>
 *
 * <p><b>Note:</b> Although the method is {@code loadUserByUsername}, it actually loads by <b>email</b>.</p>
 *
 * <p><b>Extension:</b> Developers can extend this service to load custom profile entities.
 * Profile-specific data (firstName, lastName, etc.) should be fetched separately using
 * the auth account id.</p>
 *
 * <p>This service is conditionally loaded when:</p>
 * <ul>
 *   <li>JPA repositories are available</li>
 *   <li>No other UserDetailsService bean is defined</li>
 *   <li>nnh.security.use-jpa is true (default)</li>
 * </ul>
 */
@Service
@ConditionalOnBean(AuthAccountRepository.class)
@ConditionalOnMissingBean(UserDetailsService.class)
@ConditionalOnProperty(prefix = "nnh.security", name = "use-jpa", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final AuthAccountRepository authAccountRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        // Load auth account by email
        AuthAccount authAccount = authAccountRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + email));

        // Check if account is active
        if (!authAccount.getIsActive()) {
            throw new AuthenticationException("Account is not active");
        }

        // Create and return JwtUserPrincipal
        // Profile-specific data should be loaded separately by the application
        return JwtUserPrincipal.create(authAccount);
    }
}