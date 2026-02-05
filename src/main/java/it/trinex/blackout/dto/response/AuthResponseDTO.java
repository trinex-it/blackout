package it.trinex.blackout.dto.response;

import lombok.Builder;

@Builder
public record AuthResponseDTO (
    Boolean needOTP,
    String access_token,
    String refresh_token,
    Long access_token_expiration,
    Long refresh_token_expiration
) {}
