package it.trinex.nnh.security.jwt;

import it.trinex.nnh.model.AuthAccount;
import it.trinex.nnh.model.AuthAccountType;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/**
 * JWT User Principal implementing Spring Security's UserDetails.
 *
 * <p>This class represents an authenticated user in the system with JWT-based authentication.
 * It contains only the fundamental authentication properties required by Spring Security.</p>
 *
 * <p><b>Design Note:</b> This class contains only authentication data (id, username, password, role).
 * Application-specific data (firstName, lastName, profileId, etc.) should be accessed through
 * your custom profile entities using the auth account id.</p>
 *
 * <p>The role field maps to Spring Security authorities with the "ROLE_" prefix automatically added.</p>
 */
@Getter
public class JwtUserPrincipal implements UserDetails {

    private final Long id;
    private final String username;
    private final String password;
    private final String role;
    private final Collection<? extends GrantedAuthority> authorities;

    /**
     * Constructor for JwtUserPrincipal.
     *
     * @param id the auth account ID
     * @param username the username (email)
     * @param password the password hash
     * @param role the role (USER, ADMIN, or custom)
     * @param authorities the granted authorities
     */
    public JwtUserPrincipal(
            Long id,
            String username,
            String password,
            String role,
            Collection<? extends GrantedAuthority> authorities
    ) {
        this.id = id;
        this.username = username;
        this.password = password;
        this.role = role;
        this.authorities = authorities;
    }

    /**
     * Create JwtUserPrincipal from AuthAccount entity.
     *
     * <p>This is the primary factory method for creating a principal from an AuthAccount.
     * Application-specific profile data should be loaded separately using the auth account id.</p>
     *
     * @param authAccount the auth account
     * @return JwtUserPrincipal instance
     */
    public static JwtUserPrincipal create(AuthAccount authAccount) {
        List<GrantedAuthority> authorities = List.of(
                new SimpleGrantedAuthority("ROLE_" + authAccount.getType().name())
        );

        return new JwtUserPrincipal(
                authAccount.getId(),
                authAccount.getEmail(),
                authAccount.getPasswordHash(),
                authAccount.getType().name(),
                authorities
        );
    }

    /**
     * Create JwtUserPrincipal from JWT claims.
     *
     * @param claims the JWT claims
     * @return JwtUserPrincipal instance
     */
    public static JwtUserPrincipal fromClaims(java.util.Map<String, Object> claims) {
        Long id = ((Number) claims.get("uid")).longValue();
        String username = (String) claims.get("sub");
        String role = (String) claims.get("role");

        List<GrantedAuthority> authorities = List.of(
                new SimpleGrantedAuthority("ROLE_" + role)
        );

        return new JwtUserPrincipal(
                id,
                username,
                null, // Password not available from claims
                role,
                authorities
        );
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true; // Checked before creating principal
    }

    /**
     * Get the role as an enum.
     *
     * @return the AuthAccountType
     */
    public AuthAccountType getRoleType() {
        return AuthAccountType.valueOf(role);
    }

    /**
     * Check if user has a specific role.
     *
     * @param role the role to check
     * @return true if user has the role
     */
    public boolean hasRole(String role) {
        return this.role.equals(role);
    }

    /**
     * Check if user has a specific authority.
     *
     * @param authority the authority to check
     * @return true if user has the authority
     */
    public boolean hasAuthority(String authority) {
        return authorities.stream()
                .anyMatch(a -> a.getAuthority().equals(authority));
    }
}
