package it.trinex.nnh.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Signup request DTO.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SignupRequest {

    /**
     * User's email address (will be used as username).
     */
    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    /**
     * User's password.
     */
    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters")
    private String password;

    /**
     * Account type (optional, defaults to USER).
     */
    private String accountType;
}
