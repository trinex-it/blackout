package it.trinex.blackout.service;

import it.trinex.blackout.exception.InvalidTOTPCodeException;
import it.trinex.blackout.exception.UserNotFoundException;
import it.trinex.blackout.repository.AuthAccountRepo;
import it.trinex.blackout.dto.response.AuthResponseDTO;
import it.trinex.blackout.dto.response.AuthStatusResponseDTO;
import it.trinex.blackout.exception.DuplicateKeyException;
import it.trinex.blackout.exception.InvalidTokenException;
import it.trinex.blackout.exception.UnauthorizedException;
import it.trinex.blackout.model.AuthAccount;
import it.trinex.blackout.security.BlackoutUserPrincipal;
import it.trinex.blackout.properties.JwtProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@Slf4j
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final AuthAccountRepo authAccountRepo;
    private final JwtProperties jwtProperties;
    private final UserDetailsService userDetailsService;
    private final TOTPService totpService;

    public AuthResponseDTO login(String subject, String password, Boolean rememberMe, String totpCode) {
        log.info("Login attempt for user: '{}' ", subject);

        // Authenticate user with Spring Security
        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            subject,
                            password));
        } catch (AuthenticationException e) {
            throw new UnauthorizedException("Invalid username or password");
        }

        AuthAccount authAccount = authAccountRepo.findByUsername(subject).orElse(
                authAccountRepo.findByEmail(subject).orElseThrow(() -> new UserNotFoundException("Invalid username or password"))
        );

        if(authAccount.getTotpSecret() != null && !authAccount.getTotpSecret().isEmpty()) {
            if(totpCode != null && !totpCode.isEmpty()) {
                if(!totpService.verifyCode(totpCode, authAccount.getTotpSecret())) {
                    throw new InvalidTOTPCodeException("Invalid TOTP code");
                }
            } else {
                return new AuthResponseDTO(true, null, null, null, null);
            }
        }

        // Extract authenticated user principal
        BlackoutUserPrincipal userPrincipal = (BlackoutUserPrincipal) authentication.getPrincipal();

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
        long refreshTokenMaxAge = rememberMe ? Duration.ofMillis(refreshTokenExpirationMs).toSeconds() : jwtProperties.getRefreshTokenExpNoRemember();

        return AuthResponseDTO.builder()
            .needOTP(false)
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
        String subject = jwtService.extractSubject(refreshToken);

        BlackoutUserPrincipal userPrincipal = (BlackoutUserPrincipal) userDetailsService.loadUserByUsername(subject);

        // Generate new access token
        String newAccessToken = jwtService.generateAccessToken(userPrincipal);

        // Calculate expiration time for client
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
        if (authentication == null || !(authentication.getPrincipal() instanceof BlackoutUserPrincipal userPrincipal)) {
            throw new UnauthorizedException("User is not authenticated");
        }

        return AuthStatusResponseDTO.builder()
                .id(userPrincipal.getAuthId())
                .username(userPrincipal.getUsername())
                .role(userPrincipal.getAuthorities().stream().findFirst().map(Object::toString)
                        .orElse("UNKNOWN"))
                .build();
    }

    public AuthAccount registerAuthAccount(AuthAccount authAccount) {
        // Verify that either username or email are populated
        if ((authAccount.getUsername() == null || authAccount.getUsername().trim().isEmpty()) &&
                (authAccount.getEmail() == null || authAccount.getEmail().trim().isEmpty())) {
            throw new IllegalArgumentException("Either username or email must be provided");
        }

        try {
            return authAccountRepo.save(authAccount);
        } catch (DataIntegrityViolationException e) {
            log.warn("Duplicate key violation during registration: {}", e.getMessage());

            // Check if the exception is related to username/email uniqueness
            if (e.getMessage() != null && e.getMessage().contains("username")) {
                throw new DuplicateKeyException("email", authAccount.getUsername());
            }

            // Generic duplicate key error
            throw new DuplicateKeyException("A record with these values already exists");
        }
    }
}
