package it.trinex.nnh.security;

import it.trinex.nnh.security.jwt.JwtUserPrincipal;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * In-memory UserDetailsService for testing purposes.
 *
 * <p>This service is only activated when the property {@code nnh.security.use-in-memory-auth}
 * is set to {@code true}. It provides pre-configured test users without requiring a database.</p>
 *
 * <p>Default test users:</p>
 * <ul>
 *   <li>Email: test@example.com, Password: password, Role: OWNER</li>
 *   <li>Email: admin@example.com, Password: password, Role: ADMIN</li>
 * </ul>
 *
 * <p><b>Warning:</b> This should only be used for testing and never in production!</p>
 */
@Service
@ConditionalOnProperty(prefix = "nnh.security", name = "use-in-memory-auth", havingValue = "true")
public class InMemoryUserDetailsService implements UserDetailsService {

    private final Map<String, JwtUserPrincipal> users = new HashMap<>();
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    /**
     * Initialize with default test users.
     */
    @jakarta.annotation.PostConstruct
    public void init() {
        // Default test users
        addUser("test@example.com", "password", "OWNER", 1L);
        addUser("admin@example.com", "password", "ADMIN", 2L);
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        JwtUserPrincipal user = users.get(email);
        if (user == null) {
            throw new UsernameNotFoundException("User not found: " + email);
        }
        return user;
    }

    /**
     * Add a test user.
     *
     * @param email the user's email
     * @param password the user's password
     * @param role the user's role
     * @param id the user's ID
     */
    public void addUser(String email, String password, String role, Long id) {
        List<SimpleGrantedAuthority> authorities = List.of(
                new SimpleGrantedAuthority("ROLE_" + role)
        );

        JwtUserPrincipal user = new JwtUserPrincipal(
                id,
                email,
                passwordEncoder.encode(password),
                role.equals("OWNER") ? java.util.Optional.of(id + 100) : java.util.Optional.empty(),
                "Test",
                "User",
                role,
                authorities
        );

        users.put(email, user);
    }

    /**
     * Remove a test user.
     *
     * @param email the user's email
     */
    public void removeUser(String email) {
        users.remove(email);
    }

    /**
     * Check if a user exists.
     *
     * @param email the user's email
     * @return true if exists, false otherwise
     */
    public boolean userExists(String email) {
        return users.containsKey(email);
    }

    /**
     * Get all user emails.
     *
     * @return list of user emails
     */
    public java.util.Collection<String> getAllUserEmails() {
        return users.keySet();
    }
}
