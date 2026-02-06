package it.trinex.blackout.service;

import it.trinex.blackout.exception.UnauthorizedException;
import it.trinex.blackout.model.AuthAccount;
import it.trinex.blackout.repository.AuthAccountRepo;
import it.trinex.blackout.security.BlackoutUserPrincipal;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnMissingBean(CurrentUserService.class)
@RequiredArgsConstructor
public class CurrentUserService<P extends BlackoutUserPrincipal> {

    private final AuthAccountRepo authAccountRepo;

    public P getCurrentPrincipal() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof BlackoutUserPrincipal userPrincipal)) {
            throw new UnauthorizedException("User is not authenticated");
        }

        return (P) userPrincipal;
    }

    public AuthAccount getAuthAccount() {
        UserDetails principal = getCurrentPrincipal();

        return authAccountRepo.findByUsername(principal.getUsername()).orElse(
                authAccountRepo.findByEmail(principal.getUsername())
                        .orElseThrow(() -> new UsernameNotFoundException("User with username " + principal.getUsername() + " not found."))
        );
    }

//    public U getCurrentUserReference() {
//        Long id =  getCurrentPrincipal().getUserId();
//        U reference = entityManager.getReference(U.class, id);
//    }

}
