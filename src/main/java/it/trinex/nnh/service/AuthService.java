package it.trinex.nnh.service;

import it.trinex.nnh.controller.AuthResponseDTO;
import it.trinex.nnh.properties.CorsProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
    public AuthResponseDTO login(String subject, String password) {
        log.info("Login attempt for user: '{}' ", subject);

        // Authenticate user with Spring Security
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        subject,
                        password));

        // Extract authenticated user principal
        JWTUserPrincipal userPrincipal = (JWTUserPrincipal) authentication.getPrincipal();

        // Generate tokens
        String accessToken = jwtService.generateAccessToken(userPrincipal);
        String refreshToken = jwtService.generateRefreshToken(userPrincipal);

        // Calculate expiration time for client
        AuthAccountType role = jwtService.extractRole(accessToken);
        long accessTokenExpirationMs = jwtService.calculateAccessTokenExpiration(role).toEpochMilli()
                - System.currentTimeMillis();
        long refreshTokenExpirationMs = jwtService.calculateRefreshTokenExpiration(role).toEpochMilli()
                - System.currentTimeMillis();

        log.info("User '{}' logged in successfully", subject);

        // Determine if we should set the refresh token
        boolean rememberMe = Boolean.TRUE.equals(loginRequestDTO.getRememberMe());
        long refreshTokenMaxAge = rememberMe ? Duration.ofMillis(refreshTokenExpirationMs).toSeconds() : 0;

        ResponseCookie refreshCookie = ResponseCookie.from("refresh_token", refreshToken)
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(refreshTokenMaxAge)
                .sameSite("None")
                .build();

        AuthResponseDTO response = new AuthResponseDTO(accessTokenExpirationMs);

    }
}
