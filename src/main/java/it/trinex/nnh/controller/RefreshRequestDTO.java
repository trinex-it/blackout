package it.trinex.nnh.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

@Getter
@Valid
public class RefreshRequestDTO {
    @NotNull(message = "Refresh token is required")
    private String refreshToken;
}
