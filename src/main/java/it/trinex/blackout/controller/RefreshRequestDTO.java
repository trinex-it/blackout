package it.trinex.blackout.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

@Getter
@Valid
public class RefreshRequestDTO {
    @NotNull(message = "Refresh token is required")
    private String refreshToken;
}
