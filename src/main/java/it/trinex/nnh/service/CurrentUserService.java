package it.trinex.nnh.service;

import it.trinex.nnh.security.jwt.JwtService;
import it.trinex.nnh.security.jwt.JwtUserPrincipal;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Service layer access to the current authenticated user.
 *
 * <p>This service provides convenient methods for accessing the current user's information
 * from the SecurityContext. It should be used instead of directly accessing SecurityContextHolder
 * in service layer code.</p>
 *
 * <p><b>Important:</b> Do NOT pass authAccountId as parameters when referring to the
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
 *         String email = currentUserService.getCurrentEmail();
 *         String role = currentUserService.getCurrentRole();
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
        return getJwtService().getCurrentUserId();
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
        return getJwtService().getCurrentEmail();
    }

    /**
     * Get the current user's role.
     *
     * @return the role
     * @throws IllegalStateException if no authenticated user found
     */
    public String getCurrentRole() {
        return getJwtService().getCurrentRole();
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