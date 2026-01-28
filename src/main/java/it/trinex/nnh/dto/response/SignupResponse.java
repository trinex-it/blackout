package it.trinex.nnh.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Signup response DTO.
 *
 * <p><b>Note:</b> This response does NOT include the actual JWT tokens.
 * Tokens are sent via HTTP-only cookies for security.</p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SignupResponse {

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
     * Success message.
     */
    private String message;
}
