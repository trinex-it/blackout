package it.trinex.blackout.service;

import it.trinex.blackout.exception.UnauthorizedException;
import it.trinex.blackout.model.BlackoutUserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnMissingBean(CurrentUserService.class)
@RequiredArgsConstructor
public class CurrentUserService<P extends BlackoutUserPrincipal> {

    public P getCurrentPrincipal() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof BlackoutUserPrincipal userPrincipal)) {
            throw new UnauthorizedException("User is not authenticated");
        }

        return (P) userPrincipal;
    }

}
