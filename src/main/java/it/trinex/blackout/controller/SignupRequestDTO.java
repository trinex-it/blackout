package it.trinex.blackout.controller;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "Signup request with email and password credentials")
public class SignupRequestDTO {

    @Schema(description = "User's email address", example = "user@example.com", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    private String email;

    @Schema(description = "User's password", example = "SecurePassword123!", requiredMode = Schema.RequiredMode.REQUIRED, format = "password")
    @NotBlank(message = "Password is required")
    private String password;

    @Schema(description = "Password confirmation", example = "SecurePassword123!", requiredMode = Schema.RequiredMode.REQUIRED, format = "password")
    @NotBlank(message = "Password confirmation is required")
    private String confirmPassword;
}