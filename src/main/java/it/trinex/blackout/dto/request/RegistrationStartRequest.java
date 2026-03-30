package it.trinex.blackout.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RegistrationStartRequest {
    
    @NotBlank(message = "Username is required")
    private String username;
    
    @NotBlank(message = "Display name is required")
    private String displayName;
    
    @NotBlank(message = "Email is required")
    @Email(message = "Email should be valid")
    private String email;
}

