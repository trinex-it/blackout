package it.trinex.blackout.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Getter
public class Disable2FAWithRecoveryRequest {
    @NotBlank(message = "Subject cannot be blank")
    private String subject;
    @NotBlank(message = "Password cannot be blank")
    private String password;
    @NotBlank(message = "Recovery code cannot be blank")
    private String recoveryCode;
}
