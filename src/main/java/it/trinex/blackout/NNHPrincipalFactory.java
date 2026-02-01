package it.trinex.blackout;

import io.jsonwebtoken.Claims;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;

public interface NNHPrincipalFactory<T extends UserDetails> {
    T fromClaims(Claims claims, Collection<? extends GrantedAuthority> authorities);
}
