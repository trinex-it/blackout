package it.trinex.blackout.service;

import it.trinex.blackout.dto.response.AuthResponseDTO;
import it.trinex.blackout.properties.JwtProperties;
import it.trinex.blackout.security.BlackoutUserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class WebAuthnHelperService {

    private final JwtService jwtService;
    private final JwtProperties jwtProperties;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public AuthResponseDTO generateTokensForPrincipal(BlackoutUserPrincipal userPrincipal, Boolean rememberMe) {
        String accessToken = jwtService.generateAccessToken(userPrincipal);
        String refreshToken = jwtService.generateRefreshToken(userPrincipal);

        long accessTokenExpirationMs = jwtService.calculateAccessTokenExpiration().toEpochMilli()
                - System.currentTimeMillis();
        long refreshTokenExpirationMs = jwtService.calculateRefreshTokenExpiration().toEpochMilli()
                - System.currentTimeMillis();

        long refreshTokenMaxAge = (rememberMe != null && rememberMe) ? Duration.ofMillis(refreshTokenExpirationMs).toSeconds() : jwtProperties.getRefreshTokenExpNoRemember();

        String userJson = null;
        try {
            userJson = objectMapper.writeValueAsString(jwtService.extractAllClaims(accessToken));
        } catch (Exception e) {
            throw new RuntimeException("Could not serialize user principal", e);
        }

        return AuthResponseDTO.builder()
            .needOTP(false)
            .access_token(accessToken)
            .refresh_token(refreshToken)
            .access_token_expiration(accessTokenExpirationMs)
            .refresh_token_expiration(refreshTokenMaxAge)
            .userJson(userJson)
            .build();
    }
}
