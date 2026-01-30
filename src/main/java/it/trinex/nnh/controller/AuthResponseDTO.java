package it.trinex.nnh.controller;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public record AuthResponseDTO {
    private static String access_token;
    private static String refresh_token;
}
