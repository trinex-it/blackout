package it.trinex.nnh.security.jwt;

import it.trinex.nnh.model.AuthAccount;
import it.trinex.nnh.model.AuthAccountType;
import it.trinex.nnh.model.Owner;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * JWT User Principal implementing Spring Security's UserDetails.
 *
 * <p>This record represents an authenticated user in the system with JWT-based authentication.
 * It contains all the information needed for authentication and authorization.</p>
 *
 * <p>The role field maps to Spring Security authorities with the "ROLE_" prefix automatically added.</p>
 */
@Getter
public class JwtUserPrincipal implements UserDetails {

    private final Long id;
    private final String username;
    private final String password;
    private final Optional<Long> ownerId;
    private final String firstName;
    private final String lastName;
    private final String role;
    private final Collection<? extends GrantedAuthority> authorities;

    /**
     * Constructor for JwtUserPrincipal.
     *
     * @param id the auth account ID
     * @param username the username (email)
     * @param password the password hash
     * @param ownerId optional owner profile ID
     * @param firstName the user's first name
     * @param lastName the user's last name
     * @param role the role (OWNER, ADMIN, etc.)
     * @param authorities the granted authorities
     */
    public JwtUserPrincipal(
            Long id,
            String username,
            String password,
            Optional<Long> ownerId,
            String firstName,
            String lastName,
            String role,
            Collection<? extends GrantedAuthority> authorities
    ) {
        this.id = id;
        this.username = username;
        this.password = password;
        this.ownerId = ownerId;
        this.firstName = firstName;
        this.lastName = lastName;
        this.role = role;
        this.authorities = authorities;
    }

    /**
     * Create JwtUserPrincipal from AuthAccount entity.
     *
     * @param authAccount the auth account
     * @param owner optional owner profile
     * @return JwtUserPrincipal instance
     */
    public static JwtUserPrincipal create(AuthAccount authAccount, Optional<Owner> owner) {
        List<GrantedAuthority> authorities = List.of(
                new SimpleGrantedAuthority("ROLE_" + authAccount.getType().name())
        );

        return new JwtUserPrincipal(
                authAccount.getId(),
                authAccount.getEmail(),
                authAccount.getPasswordHash(),
                owner.map(Owner::getId),
                owner.map(Owner::getFirstName).orElse(""),
                owner.map(Owner::getLastName).orElse(""),
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
        Long ownerId = claims.get("ownerId") != null ? ((Number) claims.get("ownerId")).longValue() : null;
        String firstName = (String) claims.get("firstName");
        String lastName = (String) claims.get("lastName");

        List<GrantedAuthority> authorities = List.of(
                new SimpleGrantedAuthority("ROLE_" + role)
        );

        return new JwtUserPrincipal(
                id,
                username,
                null, // Password not available from claims
                Optional.ofNullable(ownerId),
                firstName != null ? firstName : "",
                lastName != null ? lastName : "",
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
