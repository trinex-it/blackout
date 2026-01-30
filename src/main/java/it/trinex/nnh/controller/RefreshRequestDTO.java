package it.trinex.nnh.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

@Getter
@Valid
public class RefreshRequestDTO {
    @NotNull
    private String refreshToken;
}
