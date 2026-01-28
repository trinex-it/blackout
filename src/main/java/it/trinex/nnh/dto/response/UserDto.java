package it.trinex.nnh.dto.response;

import it.trinex.nnh.security.jwt.JwtUserPrincipal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * User DTO for authentication responses.
 *
 * <p>This DTO contains only authentication-related data from the JWT principal.
 * Application-specific profile data (firstName, lastName, etc.) should be fetched
 * separately using the auth account id and your custom profile entities.</p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserDto {

    /**
     * User's auth account ID.
     */
    private Long id;

    /**
     * User's email address.
     */
    private String email;

    /**
     * User's role.
     */
    private String role;

    /**
     * Create UserDto from a JwtUserPrincipal.
     *
     * @param principal the JWT user principal
     * @return the UserDto
     */
    public static UserDto fromPrincipal(JwtUserPrincipal principal) {
        return UserDto.builder()
                .id(principal.getId())
                .email(principal.getUsername())
                .role(principal.getRole())
                .build();
    }
}