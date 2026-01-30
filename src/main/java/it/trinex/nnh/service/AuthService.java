package it.trinex.nnh.service;

import it.trinex.nnh.controller.AuthResponseDTO;
import it.trinex.nnh.model.NNHUserPrincipal;
import it.trinex.nnh.properties.CorsProperties;
import it.trinex.nnh.properties.JwtProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@Slf4j
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JWTService jwtService;
    private final JwtProperties jwtProperties;
    public AuthResponseDTO login(String subject, String password, Boolean rememberMe) {
        log.info("Login attempt for user: '{}' ", subject);

        // Authenticate user with Spring Security
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        subject,
                        password));

        // Extract authenticated user principal
        NNHUserPrincipal userPrincipal = (NNHUserPrincipal) authentication.getPrincipal();

        // Generate tokens
        String accessToken = jwtService.generateAccessToken(userPrincipal);
        String refreshToken = jwtService.generateRefreshToken(userPrincipal);

        // Calculate expiration time for client
        long accessTokenExpirationMs = jwtService.calculateAccessTokenExpiration().toEpochMilli()
                - System.currentTimeMillis();
        long refreshTokenExpirationMs = jwtService.calculateRefreshTokenExpiration().toEpochMilli()
                - System.currentTimeMillis();

        log.info("User '{}' logged in successfully", subject);

        // Determine if we should set the refresh token
        long refreshTokenMaxAge = rememberMe ? Duration.ofMillis(refreshTokenExpirationMs).toSeconds() : jwtProperties.getDefaultRefreshExpirationNoRemember();

        return AuthResponseDTO.builder()
            .access_token(accessToken)
            .refresh_token(refreshToken)
            .access_token_expiration(accessTokenExpirationMs)
            .refresh_token_expiration(refreshTokenMaxAge)
            .build();
    }
}
