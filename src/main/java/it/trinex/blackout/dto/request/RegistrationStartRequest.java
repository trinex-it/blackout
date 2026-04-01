package it.trinex.blackout.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RegistrationStartRequest {
    
    @NotBlank(message = "Display name is required")
    private String displayName;

}

