package it.trinex.blackout.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Getter
public class Enable2FARequest {
    @NotBlank(message = "Secret cannot be blank")
    private String secret;
    @NotBlank(message = "TOTP cannot be blank")
    private String totp;
}
