package it.trinex.blackout.service;

import it.trinex.blackout.exception.*;
import it.trinex.blackout.repository.AuthAccountRepo;
import it.trinex.blackout.dto.response.AuthResponseDTO;
import it.trinex.blackout.dto.response.AuthStatusResponseDTO;
import it.trinex.blackout.model.AuthAccount;
import it.trinex.blackout.security.BlackoutUserPrincipal;
import it.trinex.blackout.properties.JwtProperties;
import it.trinex.blackout.service.redis.RedisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.util.Date;

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
    private final CurrentUserService currentUserService;
    private final RedisService redisService;

    private ObjectMapper objectMapper = new ObjectMapper();

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
            if(e.getCause() instanceof AccountNotActiveException) {
                throw (AccountNotActiveException) e.getCause();
            }
            throw new UnauthorizedException("Invalid username or password");
        }

        AuthAccount authAccount = authAccountRepo.findByUsername(subject).orElse(
                authAccountRepo.findByEmail(subject).get()
        );

        if (authAccount.isPasswordless()) {
            throw new PasswordlessEnabledException("Passwordless login enabled, use a passkey to login");
        }

        if(authAccount.getTotpSecret() != null && !authAccount.getTotpSecret().isEmpty()) {
            if(totpCode != null && !totpCode.isEmpty()) {
                if(!totpService.verifyCode(totpCode, authAccount.getTotpSecret())) {
                    throw new InvalidTOTPCodeException("Invalid TOTP code");
                }
            } else {
                return new AuthResponseDTO(true, null, null, null, null, null);
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

        String userJson = objectMapper.writeValueAsString(jwtService.extractAllClaims(accessToken));

        return AuthResponseDTO.builder()
            .needOTP(false)
            .access_token(accessToken)
            .refresh_token(refreshToken)
            .access_token_expiration(accessTokenExpirationMs)
            .refresh_token_expiration(refreshTokenMaxAge)
            .userJson(userJson)
            .build();
    }

    public void disableUser(String subject) {
        AuthAccount authAccount = authAccountRepo.findByUsername(subject).orElse(
                authAccountRepo.findByEmail(subject).orElseThrow(
                        () -> new UsernameNotFoundException("Username not found: " + subject)
                )
        );

        BlackoutUserPrincipal operator = currentUserService.getCurrentPrincipal();
        if(operator.getAuthId().equals(authAccount.getId())) {
            throw new BlackoutException(
                    HttpStatus.BAD_REQUEST,
                    "INVALID_OPERATION",
                    "It is not allowed to self-disable an authAccount"
            );
        }

        redisService.revokeAllUserTokens(authAccount.getId());
        authAccount.setActive(false);
        authAccountRepo.save(authAccount);
    }

    public void enableUser(String subject) {
        AuthAccount authAccount = authAccountRepo.findByUsername(subject).orElse(
                authAccountRepo.findByEmail(subject).orElseThrow(
                        () -> new UsernameNotFoundException("Username not found: " + subject)
                )
        );

        authAccount.setActive(true);
        authAccountRepo.save(authAccount);
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
        String newRefreshToken = jwtService.generateRefreshToken(userPrincipal);

        // Calculate expiration time for client
        long accessTokenExpirationMs = jwtService.calculateAccessTokenExpiration().toEpochMilli()
                - System.currentTimeMillis();
        long refreshTokenExpirationMs = jwtService.calculateRefreshTokenExpiration().toEpochMilli()
                - System.currentTimeMillis();

        String oldRefreshJti = jwtService.extractJti(refreshToken);
        Date oldRefreshExp = jwtService.extractExpiration(refreshToken);

        redisService.revokeRefreshToken(oldRefreshJti, oldRefreshExp);

        log.info("Token refreshed successfully for user: {}", userPrincipal.getUsername());

        return AuthResponseDTO.builder()
                .access_token(newAccessToken)
                .refresh_token(newRefreshToken)
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
