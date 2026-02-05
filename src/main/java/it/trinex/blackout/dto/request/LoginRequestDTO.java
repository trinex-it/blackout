package it.trinex.blackout.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "Login request with email and password credentials")
public class LoginRequestDTO {

    @Schema(description = "Email or username", example = "andrew")
    @NotBlank(message = "Either email or username is required")
    private String subject;

    @Schema(description = "User's password", example = "odioinegri", requiredMode = Schema.RequiredMode.REQUIRED, format = "password")
    @NotBlank(message = "Password is required")
    private String password;

    @Schema(description = "Remember me flag", example = "true", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private Boolean rememberMe = false;

}
