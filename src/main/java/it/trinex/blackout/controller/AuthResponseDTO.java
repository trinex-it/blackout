package it.trinex.blackout.controller;

import lombok.Builder;

@Builder
public record AuthResponseDTO (
    String access_token,
    String refresh_token,
    Long access_token_expiration,
    Long refresh_token_expiration
) {}
