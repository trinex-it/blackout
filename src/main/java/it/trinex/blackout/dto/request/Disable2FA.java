package it.trinex.blackout.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class Disable2FA {
    @NotBlank(message = "2FA code is required")
    private String code;
}
