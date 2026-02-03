package it.trinex.blackout.service;

import it.trinex.blackout.repository.AuthAccountRepo;
import it.trinex.blackout.exception.AccountNotActiveException;
import it.trinex.blackout.model.AuthAccount;
import it.trinex.blackout.security.BlackoutUserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class BlackoutUserDetailService implements UserDetailsService {

    protected final AuthAccountRepo authAccountRepo;


    @Override
    public BlackoutUserPrincipal loadUserByUsername(String username) throws UsernameNotFoundException {
        AuthAccount authAccount = authAccountRepo.findByUsername(username).orElseThrow(
                () -> new UsernameNotFoundException(username));

        // Check if account is active
        if (!authAccount.isActive()) {
            throw new AccountNotActiveException("AuthAccount with ID: " + authAccount.getId() +
                    " (email: " + authAccount.getUsername() + ") is not active");
        }

        return BlackoutUserPrincipal.builder()
                .id(authAccount.getId())
                .userId(null)
                .username(username)
                .password(authAccount.getPasswordHash())
                .build();
    }
}
