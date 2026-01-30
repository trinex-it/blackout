package it.trinex.nnh.service;

import it.trinex.nnh.AuthAccountRepository;
import it.trinex.nnh.model.AuthAccount;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Slf4j
@RequiredArgsConstructor
@Service
public class CustomUserDetailsService implements UserDetailsService {

        private final AuthAccountRepository authAccountRepository;
        /*
         * overriding but we are actually loading by EMAIL, not username!
         */
        @Override
        public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
                AuthAccount authAccount = authAccountRepository.findByEmail(username)
                                .orElseThrow(() -> new UsernameNotFoundException("Utente non trovato: " + username));

                // Check if account is active
                if (!authAccount.isActive()) {
                        throw new AccountNotActiveException("AuthAccount with ID: " + authAccount.getId() +
                                        " (email: " + authAccount.getEmail() + ") is not active");
                }

                List<SimpleGrantedAuthority> authorities = List.of(
                                new SimpleGrantedAuthority("ROLE_" + authAccount.getType().name()));

                Optional<Long> ownerId = Optional.empty();

                String firstName = null;
                String lastName = null;

                // Refactoring the logic above to capture names
                if (Objects.requireNonNull(authAccount.getType()) == AuthAccountType.OWNER) {
                        Owner owner = ownerRepository.findByAuthAccount(authAccount);
                        if (owner != null) {
                                ownerId = Optional.of(owner.getId());
                                firstName = owner.getFirstName();
                                lastName = owner.getLastName();
                        } else {
                                log.warn("Owner record not found for authAccount ID: {} (email: {}). Proceeding with null ownerId.",
                                                authAccount.getId(), authAccount.getEmail());
                        }
                }

                return new JWTUserPrincipal(
                                authAccount.getId(),
                                authAccount.getEmail(),
                                authAccount.getPasswordHash(),
                                ownerId,
                                firstName,
                                lastName,
                                authorities);
        }
}