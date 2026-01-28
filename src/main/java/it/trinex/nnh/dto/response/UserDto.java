package it.trinex.nnh.dto.response;

import it.trinex.nnh.security.jwt.JwtUserPrincipal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Optional;

/**
 * User DTO.
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
     * Owner profile ID (if applicable).
     */
    private Optional<Long> ownerId;

    /**
     * User's first name.
     */
    private String firstName;

    /**
     * User's last name.
     */
    private String lastName;

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
                .ownerId(principal.getOwnerId())
                .firstName(principal.getFirstName())
                .lastName(principal.getLastName())
                .build();
    }
}
