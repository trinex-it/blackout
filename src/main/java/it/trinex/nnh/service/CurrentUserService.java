package it.trinex.nnh.service;

import it.trinex.nnh.security.jwt.JwtService;
import it.trinex.nnh.security.jwt.JwtUserPrincipal;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Optional;

/**
 * Service layer access to the current authenticated user.
 *
 * <p>This service provides convenient methods for accessing the current user's information
 * from the SecurityContext. It should be used instead of directly accessing SecurityContextHolder
 * in service layer code.</p>
 *
 * <p><b>Important:</b> Do NOT pass userId or ownerId as parameters when referring to the
 * currently logged-in user. Always use this service instead.</p>
 *
 * <p>Example usage:</p>
 * <pre>
 * &#64;Service
 * &#64;RequiredArgsConstructor
 * public class MyService {
 *     private final CurrentUserService currentUserService;
 *
 *     public void doSomething() {
 *         Long authAccountId = currentUserService.getCurrentAuthAccountId();
 *         Optional&lt;Long&gt; ownerId = currentUserService.getCurrentOwnerId();
 *     }
 * }
 * </pre>
 */
public class CurrentUserService {

    private JwtService jwtService;

    @Autowired(required = false)
    public void setJwtService(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    private JwtService getJwtService() {
        if (jwtService == null) {
            throw new IllegalStateException("JwtService is not available");
        }
        return jwtService;
    }

    /**
     * Get the current user's auth account ID.
     *
     * @return the auth account ID
     * @throws IllegalStateException if no authenticated user found
     */
    public Long getCurrentAuthAccountId() {
        return getJwtService().getCurrentUser().getId();
    }

    /**
     * Get the current user's owner ID (if applicable).
     *
     * @return optional owner ID
     * @throws IllegalStateException if no authenticated user found
     */
    public Optional<Long> getCurrentOwnerId() {
        return getJwtService().getCurrentUser().getOwnerId();
    }

    /**
     * Get the current user principal.
     *
     * @return the JWT user principal
     * @throws IllegalStateException if no authenticated user found
     */
    public JwtUserPrincipal getCurrentUserPrincipal() {
        return getJwtService().getCurrentUser();
    }

    /**
     * Get the current user's email (username).
     *
     * @return the email
     * @throws IllegalStateException if no authenticated user found
     */
    public String getCurrentEmail() {
        return getJwtService().getCurrentUser().getUsername();
    }

    /**
     * Get the current user's role.
     *
     * @return the role
     * @throws IllegalStateException if no authenticated user found
     */
    public String getCurrentRole() {
        return getJwtService().getCurrentUser().getRole();
    }

    /**
     * Get the current user's first name.
     *
     * @return the first name
     * @throws IllegalStateException if no authenticated user found
     */
    public String getCurrentFirstName() {
        return getJwtService().getCurrentUser().getFirstName();
    }

    /**
     * Get the current user's last name.
     *
     * @return the last name
     * @throws IllegalStateException if no authenticated user found
     */
    public String getCurrentLastName() {
        return getJwtService().getCurrentUser().getLastName();
    }

    /**
     * Check if the current user has a specific role.
     *
     * @param role the role to check
     * @return true if the user has the role
     * @throws IllegalStateException if no authenticated user found
     */
    public boolean hasRole(String role) {
        return getJwtService().getCurrentUser().hasRole(role);
    }
}
