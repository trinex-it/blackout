package it.trinex.nnh.service;

import it.trinex.nnh.AuthAccountRepo;
import it.trinex.nnh.exception.UnauthorizedException;
import it.trinex.nnh.model.AuthAccount;
import it.trinex.nnh.model.NNHUserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnMissingBean(CurrentUserService.class)
@RequiredArgsConstructor
public class CurrentUserService<P extends NNHUserPrincipal> {


//    public AuthAccount getCurrentAccount() {
//        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
//        if (authentication == null || !(authentication.getPrincipal() instanceof NNHUserPrincipal userPrincipal)) {
//            throw new UnauthorizedException("User is not authenticated");
//        }
//
//        return authAccountRepo.findByUsername(userPrincipal.getUsername()).orElseThrow(
//                () -> new UsernameNotFoundException("User " + userPrincipal.getUsername() + " not found")
//        );
//    }
    public P getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof NNHUserPrincipal userPrincipal)) {
            throw new UnauthorizedException("User is not authenticated");
        }

        return (P) userPrincipal;
    }

}
