package it.trinex.blackout.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AuthStatusResponseDTO {
    private Long id;
    private String username;
    private String role;
    private String firstName;
    private String lastName;
}
