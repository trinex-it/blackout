package it.trinex.blackout.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

@Getter
@Valid
public class RefreshRequestDTO {
    @Schema(description = "Refresh token", example = "eyJhbGciOiJI...", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "Refresh token is required")
    private String refreshToken;
}
