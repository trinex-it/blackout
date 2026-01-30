package it.trinex.nnh.controller;

import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Builder
public record AuthResponseDTO (
    String access_token,
    String refresh_token,
    Long access_token_expiration,
    Long refresh_token_expiration
) {}
