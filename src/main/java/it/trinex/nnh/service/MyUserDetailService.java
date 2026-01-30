package it.trinex.nnh.service;

import it.trinex.nnh.AuthAccountRepo;
import it.trinex.nnh.exception.AccountNotActiveException;
import it.trinex.nnh.model.AuthAccount;
import it.trinex.nnh.model.NNHUserPrincipal;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MyUserDetailService implements UserDetailsService {

    private final AuthAccountRepo authAccountRepo;

    @PostConstruct
    void init() {
        System.out.println("userniger CONFIG LOADED");
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
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
                .authorities(authorities)
                .username(username)
                .password(authAccount.getPasswordHash())
                .firstName(firstName)
                .lastName(lastName)
                .build();
    }
}
