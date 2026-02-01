package it.trinex.blackout.controller;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "Login request with email and password credentials")
public class LoginRequestDTO {
    @Schema(description = "User's email address", example = "hello@andrewfly.it", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    private String email;

    @Schema(description = "User's password", example = "odioinegri", requiredMode = Schema.RequiredMode.REQUIRED, format = "password")
    @NotBlank(message = "Password is required")
    private String password;

    @Schema(description = "Remember me flag", example = "true", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private Boolean rememberMe = false;
}
