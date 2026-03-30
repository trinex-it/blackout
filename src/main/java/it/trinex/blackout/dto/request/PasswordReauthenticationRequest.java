package it.trinex.blackout.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Getter
public class PasswordReauthenticationRequest {
    @NotBlank(message = "Password cannot be blank")
    private String password;
}
