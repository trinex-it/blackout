package it.trinex.nnh.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import it.trinex.nnh.exception.InvalidTokenException;
import it.trinex.nnh.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.annotation.*;


@Slf4j
@RestController
@RequestMapping("/api/jwt")
@RequiredArgsConstructor
@Tag(name = "JwtController", description = "Endpoints for token management")
public class JwtController {

    private final AuthService authService;

    /**
     * Refreshes access token using a valid refresh token.
     * Optionally rotates the refresh token for enhanced security.
     *
     * @param request token request
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
            @ApiResponse(responseCode = "401", description = "Missing refresh_token cookie or invalid/expired refresh token", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/refresh")
    public ResponseEntity<AuthResponseDTO> refresh(@RequestBody @Valid RefreshRequestDTO request) {

        log.debug("Token refresh attempt");

        AuthResponseDTO response = authService.refreshToken(request.getRefreshToken());

        return ResponseEntity.ok()
                .body(response);
    }

    @Operation(summary = "Get authentication status", description = "Checks if the user is authenticated and returns user details.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User status retrieved", content = @Content(mediaType = "application/json", schema = @Schema(implementation = AuthStatusResponseDTO.class))),
            @ApiResponse(responseCode = "401", description = "Not authenticated", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/status")
    public ResponseEntity<AuthStatusResponseDTO> getAuthStatus() {

        AuthStatusResponseDTO response = authService.getStatus();

        return ResponseEntity.ok(response);
    }
}
