package it.trinex.nnh.service;

import it.trinex.nnh.AuthAccountRepo;
import it.trinex.nnh.exception.AccountNotActiveException;
import it.trinex.nnh.model.AuthAccount;
import it.trinex.nnh.model.NNHUserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@ConditionalOnMissingBean(UserDetailsService.class)
@RequiredArgsConstructor
public class NNHUserDetailService implements UserDetailsService {

    protected final AuthAccountRepo authAccountRepo;


    @Override
    public NNHUserPrincipal loadUserByUsername(String username) throws UsernameNotFoundException {
        AuthAccount authAccount = authAccountRepo.findByUsername(username).orElseThrow(
                () -> new UsernameNotFoundException(username));

        // Check if account is active
        if (!authAccount.isActive()) {
            throw new AccountNotActiveException("AuthAccount with ID: " + authAccount.getId() +
                    " (email: " + authAccount.getUsername() + ") is not active");
        }

        List<SimpleGrantedAuthority> authorities = List.of(
                new SimpleGrantedAuthority("ROLE_" + authAccount.getRole()));

        String firstName = authAccount.getFirstName();
        String lastName = authAccount.getLastName();

        return NNHUserPrincipal.builder()
                .id(authAccount.getId())
                .userId(null)
                .authorities(authorities)
                .username(username)
                .password(authAccount.getPasswordHash())
                .firstName(firstName)
                .lastName(lastName)
                .build();
    }
}
