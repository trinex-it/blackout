package it.trinex.blackout.security;

import io.jsonwebtoken.Claims;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;

public interface BlackoutPrincipalFactory<T extends UserDetails> {
    T fromClaims(Claims claims, Collection<? extends GrantedAuthority> authorities);
}
