package it.trinex.nnh.dto.response;

import it.trinex.nnh.security.jwt.JwtUserPrincipal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Optional;

/**
 * Authentication response DTO.
 *
 * <p><b>Note:</b> This response does NOT include the actual JWT tokens.
 * Tokens are sent via HTTP-only cookies for security.</p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthResponse {

    /**
     * User's auth account ID.
     */
    private Long userId;

    /**
     * User's email address.
     */
    private String email;

    /**
     * User's role.
     */
    private String role;

    /**
     * Access token expiration time in milliseconds.
     */
    private Long expiresIn;

    /**
     * Owner profile ID (if applicable).
     */
    private Optional<Long> ownerId = Optional.empty();

    /**
     * User's first name.
     */
    private String firstName;

    /**
     * User's last name.
     */
    private String lastName;

    /**
     * Create AuthResponse from a JwtUserPrincipal.
     *
     * @param principal the JWT user principal
     * @param expiresIn access token expiration in milliseconds
     * @return the AuthResponse
     */
    public static AuthResponse fromPrincipal(JwtUserPrincipal principal, long expiresIn) {
        return AuthResponse.builder()
                .userId(principal.getId())
                .email(principal.getUsername())
                .role(principal.getRole())
                .expiresIn(expiresIn)
                .ownerId(principal.getOwnerId())
                .firstName(principal.getFirstName())
                .lastName(principal.getLastName())
                .build();
    }
}
