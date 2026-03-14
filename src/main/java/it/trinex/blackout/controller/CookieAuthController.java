package it.trinex.blackout.controller;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import it.trinex.blackout.dto.request.LoginRequestDTO;
import it.trinex.blackout.dto.request.RefreshRequestDTO;
import it.trinex.blackout.dto.response.AuthResponseDTO;
import it.trinex.blackout.dto.response.AuthStatusResponseDTO;
import it.trinex.blackout.exception.ExceptionResponseDTO;
import it.trinex.blackout.exception.InvalidTokenException;
import it.trinex.blackout.exception.UnauthorizedException;
import it.trinex.blackout.service.AuthService;
import it.trinex.blackout.service.JwtService;
import jakarta.annotation.PostConstruct;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.annotation.*;
import tools.jackson.databind.ObjectMapper;

import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping(value = "/auth", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Authentication", description = "Endpoints for user authentication and token management")
public class CookieAuthController {

    @PostConstruct
    public void init() {
        System.out.println("porcodios");
    }

    private final AuthService authService;
    private final JwtService jwtService;

    private static final String ACCESS_COOKIE_NAME = "access_token";
    private static final String REFRESH_COOKIE_NAME = "refresh_token";

    @PostMapping("/login")
    @Operation(summary = "Login user", description = "Authenticate user with email and password, returns JWT tokens")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Login successful",
            content = @Content(schema = @Schema(implementation = AuthResponseDTO.class))),
        @ApiResponse(responseCode = "202", description = "Login successful, TOTP code required",
            content = @Content(schema = @Schema(implementation = AuthResponseDTO.class))),
        @ApiResponse(responseCode = "401", description = "Invalid credentials", content =  @Content(schema = @Schema(implementation = ExceptionResponseDTO.class))),
    })
    public ResponseEntity<AuthResponseDTO> login(@Valid @RequestBody LoginRequestDTO request) {
        AuthResponseDTO response = authService.login(
            request.getSubject(),
            request.getPassword(),
            request.getRememberMe(),
            request.getTotpCode()
        );

        if (response.needOTP()) {
            return ResponseEntity.accepted().body(response);
        }

        ResponseCookie accessCookie = ResponseCookie.from(ACCESS_COOKIE_NAME, response.access_token())
            .httpOnly(true)
            .secure(true)
            .path("/")
            .maxAge(response.access_token_expiration())
            .sameSite("Lax")
            .build();
        ResponseCookie refreshCookie = ResponseCookie.from(REFRESH_COOKIE_NAME, response.refresh_token())
            .httpOnly(true)
            .secure(true)
            .path("/")
            .maxAge(response.refresh_token_expiration())
            .sameSite("Lax")
            .build();

        return ResponseEntity.ok()
            .headers(headers -> {
                headers.add(HttpHeaders.SET_COOKIE, accessCookie.toString());
                headers.add(HttpHeaders.SET_COOKIE, refreshCookie.toString());
            })
                .body(response);
    }

    /**
     * Refreshes access token using a valid refresh token.
     * Optionally rotates the refresh token for enhanced security.
     *
     * @throws InvalidTokenException if refresh token is invalid or expired
     */
    @Operation(summary = "Refresh access token", description = """
        Generates a new access token using a valid refresh token.
        
        For security, this endpoint also rotates the refresh token,
        returning a new refresh token that should be stored by the client.
        
        Use this endpoint when the access token expires to obtain a new one
        without requiring the user to log in again.
        
        """)
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Successfully refreshed tokens", content = @Content(mediaType = "application/json", schema = @Schema(implementation = AuthResponseDTO.class))),
        @ApiResponse(responseCode = "401", description = "Missing refresh_token cookie or invalid/expired refresh token", content = @Content(mediaType = "application/json", schema = @Schema(implementation = UnauthorizedException.class)))
    })
    @PostMapping("/refresh")
    public ResponseEntity<Void> refresh(@Parameter(hidden = true) @CookieValue(name = "refresh_token") String refreshToken) {

        log.debug("Token refresh attempt");

        AuthResponseDTO response = authService.refreshToken(refreshToken);

        ResponseCookie accessCookie = ResponseCookie.from(ACCESS_COOKIE_NAME, response.access_token())
            .httpOnly(true)
            .secure(true)
            .path("/")
            .maxAge(response.access_token_expiration())
            .sameSite("Lax")
            .build();

        return ResponseEntity.ok()
            .headers(headers -> {
                headers.add(HttpHeaders.SET_COOKIE, accessCookie.toString());
            })
            .build();

    }

    @PostMapping("/logout")
    @Operation(summary = "Logout user", description = """
        Clears the authentication cookies to log out the user.

        This endpoint expires both the access_token and refresh_token cookies,
        effectively ending the user's session on the client side.

        Note: Since JWTs are stateless, the tokens remain valid until their
        natural expiration time. For immediate token revocation, consider
        implementing a token blacklist or similar mechanism.
        """)
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Logout successful, cookies cleared"),
        @ApiResponse(responseCode = "401", description = "Not authenticated")
    })
    public ResponseEntity<Void> logout() {

        ResponseCookie accessCookie = ResponseCookie.from(ACCESS_COOKIE_NAME, "")
            .httpOnly(true)
            .secure(true)
            .path("/")
            .maxAge(0L)
            .sameSite("Lax")
            .build();
        ResponseCookie refreshCookie = ResponseCookie.from(REFRESH_COOKIE_NAME, "")
            .httpOnly(true)
            .secure(true)
            .path("/")
            .maxAge(0L)
            .sameSite("Lax")
            .build();

        return ResponseEntity.ok()
            .headers(headers -> {
                headers.add(HttpHeaders.SET_COOKIE, accessCookie.toString());
                headers.add(HttpHeaders.SET_COOKIE, refreshCookie.toString());
            })
            .build();
    }

    @Operation(summary = "Get authentication status", description = "Checks if the user is authenticated and returns user details.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "User status retrieved", content = @Content(mediaType = "application/json", schema = @Schema(implementation = AuthResponseDTO.class))),
        @ApiResponse(responseCode = "401", description = "Not authenticated", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/status")
    public ResponseEntity<String> getAuthStatus(@Parameter(hidden = true) @CookieValue(name = "access_token") String token) {

        String claims = jwtService.extractAllClaims(token).toString();

        return ResponseEntity.ok(claims);
    }
}