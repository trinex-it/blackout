package it.trinex.nnh.service;

import it.trinex.nnh.AuthAccountRepo;
import it.trinex.nnh.controller.AuthResponseDTO;
import it.trinex.nnh.controller.AuthStatusResponseDTO;
import it.trinex.nnh.exception.InvalidTokenException;
import it.trinex.nnh.exception.UnauthorizedException;
import it.trinex.nnh.model.AuthAccount;
import it.trinex.nnh.model.NNHUserPrincipal;
import it.trinex.nnh.properties.JwtProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@Slf4j
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JWTService jwtService;
    private final AuthAccountRepo authAccountRepo;
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

    public AuthResponseDTO refreshToken(String refreshToken) {
        // Validate refresh token
        if (!jwtService.isRefreshTokenValid(refreshToken)) {
            log.warn("Invalid or expired refresh token");
            throw new InvalidTokenException("Refresh token is invalid or expired");
        }

        // Extract user from refresh token (no database call needed!)
        NNHUserPrincipal userPrincipal = (NNHUserPrincipal) jwtService.extractUserPrincipal(refreshToken);

        // Generate new access token
        String newAccessToken = jwtService.generateAccessToken(userPrincipal);

        // Calculate expiration time for client
        String role = jwtService.extractRole(newAccessToken);
        long accessTokenExpirationMs = jwtService.calculateAccessTokenExpiration().toEpochMilli()
                - System.currentTimeMillis();
        long refreshTokenExpirationMs = jwtService.calculateRefreshTokenExpiration().toEpochMilli()
                - System.currentTimeMillis();

        log.info("Token refreshed successfully for user: {}", userPrincipal.getUsername());

        return AuthResponseDTO.builder()
                .access_token(newAccessToken)
                .refresh_token(refreshToken)
                .access_token_expiration(accessTokenExpirationMs)
                .refresh_token_expiration(refreshTokenExpirationMs)
                .build();
    }

    public AuthStatusResponseDTO getStatus() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof NNHUserPrincipal userPrincipal)) {
            throw new UnauthorizedException("User is not authenticated");
        }

        return AuthStatusResponseDTO.builder()
                .authenticated(true)
                .id(userPrincipal.getId())
                .username(userPrincipal.getUsername())
                .role(userPrincipal.getAuthorities().stream().findFirst().map(Object::toString)
                        .orElse("UNKNOWN"))
                .firstName(userPrincipal.getFirstName())
                .lastName(userPrincipal.getLastName())
                .build();
    }

    public AuthAccount registerAuthAccount(AuthAccount authAccount) {
        return authAccountRepo.save(authAccount);
    }
}
