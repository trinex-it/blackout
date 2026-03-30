package it.trinex.blackout.controller;


import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import it.trinex.blackout.dto.request.*;
import it.trinex.blackout.dto.response.AuthResponseDTO;
import it.trinex.blackout.dto.response.AuthenticationStartResponse;
import it.trinex.blackout.dto.response.ReauthenticationFinishResponse;
import it.trinex.blackout.dto.response.RegistrationStartResponse;
import it.trinex.blackout.exception.ExceptionResponseDTO;
import it.trinex.blackout.service.CookieService;
import it.trinex.blackout.service.PasskeyService;
import it.trinex.blackout.service.redis.RedisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(value = "/passkey", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Passkey Authentication", description = "Endpoints for WebAuthn/Passkey registration and authentication")
public class PasskeyController {

    private final PasskeyService passkeyService;
    private final CookieService cookieService;
    private final RedisService redisService;

    @Operation(
        summary = "Start passkey registration",
        description = """
            Initiates the WebAuthn registration process for the currently authenticated user.

            Generates a cryptographic challenge and returns registration options that
            the client uses to create a new passkey credential.

            The client should call navigator.credentials.create() with these options,
            then send the result to /register/finish.
            """
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Registration started successfully, returns challenge and options",
            content = @Content(schema = @Schema(implementation = RegistrationStartResponse.class))),
        @ApiResponse(responseCode = "401", description = "User not authenticated",
            content = @Content(schema = @Schema(implementation = ExceptionResponseDTO.class))),
    })
    @PostMapping("/register/start")
    public ResponseEntity<RegistrationStartResponse> startRegistration() {

        RegistrationStartResponse response = passkeyService.startRegistration();

        return ResponseEntity.ok(response);
    }

    @Operation(
        summary = "Finish passkey registration",
        description = """
            Completes the WebAuthn registration process by validating the credential created by the client.

            Expects the attestation response from navigator.credentials.create().
            Validates the signature, stores the credential public key, and associates
            it with the authenticated user.
            """
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Passkey registered successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request or registration not started",
            content = @Content(schema = @Schema(implementation = ExceptionResponseDTO.class))),
        @ApiResponse(responseCode = "401", description = "User not authenticated",
            content = @Content(schema = @Schema(implementation = ExceptionResponseDTO.class))),
        @ApiResponse(responseCode = "500", description = "Registration validation failed",
            content = @Content(schema = @Schema(implementation = ExceptionResponseDTO.class))),
    })
    @PostMapping("/register/finish")
    public ResponseEntity<Void> finishRegistration(
            @RequestBody RegistrationFinishRequest request) {

        passkeyService.finishRegistration(request);

        return ResponseEntity.ok().build();
    }

    @Operation(
        summary = "Start passkey authentication",
        description = """
            Initiates the WebAuthn authentication process.

            Generates a cryptographic challenge for the client to sign with their passkey.
            Returns the challenge along with allowed credentials (if known).

            The client should call navigator.credentials.get() with these options,
            then send the result to /authenticate/finish.

            Sets a passkey_session cookie to track the challenge state.
            """
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Authentication started successfully, returns challenge and options",
            content = @Content(schema = @Schema(implementation = AuthenticationStartResponse.class))),
    })
    @PostMapping("/authenticate/start")
    public ResponseEntity<AuthenticationStartResponse> startAuthentication() {

        AuthenticationStartResponse response = passkeyService.startAuthentication();

        ResponseCookie sessionCookie = ResponseCookie.from("passkey_session", response.getSessionId())
                    .httpOnly(true)
                    .secure(true)
                    .path("/")
                    .maxAge(60)
                    .sameSite("Lax")
                    .build();

        return ResponseEntity.ok()
                .headers(headers -> {
                    headers.add(HttpHeaders.SET_COOKIE, sessionCookie.toString());
                })
                .body(response);
    }

    @Operation(
        summary = "Finish passkey authentication",
        description = """
            Completes the WebAuthn authentication process by validating the signature from the client.

            Expects the assertion response from navigator.credentials.get().
            Validates the signature and user presence, then generates JWT tokens
            if authentication succeeds.

            Sets access_token and refresh_token cookies upon success.
            """
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Authentication successful, returns JWT tokens",
            content = @Content(schema = @Schema(implementation = AuthResponseDTO.class))),
        @ApiResponse(responseCode = "400", description = "Invalid authentication response or challenge not found",
            content = @Content(schema = @Schema(implementation = ExceptionResponseDTO.class))),
        @ApiResponse(responseCode = "401", description = "Passkey not found or authentication failed",
            content = @Content(schema = @Schema(implementation = ExceptionResponseDTO.class))),
        @ApiResponse(responseCode = "500", description = "Authentication validation failed",
            content = @Content(schema = @Schema(implementation = ExceptionResponseDTO.class))),
    })
    @PostMapping("/authenticate/finish")
    public ResponseEntity<AuthResponseDTO> finishAuthentication(
            @RequestBody AuthenticationFinishRequest request,
            @Parameter(hidden = true) @CookieValue(name = "passkey_session") String sessionId) {

        log.info("Finishing authentication, session: {}", sessionId);

        AuthResponseDTO response = passkeyService.finishAuthentication(request, sessionId);

        ResponseCookie accessCookie = cookieService.generateAccessCookie(response.access_token());
        ResponseCookie refreshCookie = cookieService.generateRefreshCookie(response.refresh_token());

        return ResponseEntity.ok()
                .headers(headers -> {
                    headers.add(HttpHeaders.SET_COOKIE, accessCookie.toString());
                    headers.add(HttpHeaders.SET_COOKIE, refreshCookie.toString());
                })
                .body(response);
    }

    @Operation(
        summary = "Start passkey reauthentication",
        description = """
            Initiates WebAuthn authentication for reauthentication purposes.

            Similar to /authenticate/start but returns the user's registered passkeys
            in the allowCredentials list so the authenticator can filter available credentials.

            Used when a user needs to reauthenticate for sensitive operations,
            such as changing security settings or performing critical actions.

            Sets a passkey_session cookie to track the challenge state.
            """
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Reauthentication started successfully, returns challenge and options",
            content = @Content(schema = @Schema(implementation = AuthenticationStartResponse.class))),
        @ApiResponse(responseCode = "401", description = "User not authenticated",
            content = @Content(schema = @Schema(implementation = ExceptionResponseDTO.class))),
    })
    @PostMapping("/reauthenticate/start")
    public ResponseEntity<AuthenticationStartResponse> startReauthentication() {

        AuthenticationStartResponse response = passkeyService.startReauthentication();

        ResponseCookie sessionCookie = ResponseCookie.from("passkey_session", response.getSessionId())
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(60)
                .sameSite("Lax")
                .build();

        log.info("Stored reauth challenge in session: {}", response.getSessionId());

        return ResponseEntity.ok()
                .headers(headers -> {
                    headers.add(HttpHeaders.SET_COOKIE, sessionCookie.toString());
                })
                .body(response);
    }

    @Operation(
        summary = "Finish passkey reauthentication",
        description = """
            Completes the WebAuthn reauthentication process.

            Validates that the passkey used belongs to the currently authenticated user,
            preventing credential swapping attacks.

            Upon successful validation, generates a reauthentication token stored in
            a cookie that can be used to verify recent user authentication for sensitive operations.
            """
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Reauthentication successful, sets reauthentication cookie"),
        @ApiResponse(responseCode = "400", description = "Invalid authentication response or challenge not found",
            content = @Content(schema = @Schema(implementation = ExceptionResponseDTO.class))),
        @ApiResponse(responseCode = "401", description = "Passkey not found or does not belong to current user",
            content = @Content(schema = @Schema(implementation = ExceptionResponseDTO.class))),
        @ApiResponse(responseCode = "500", description = "Reauthentication validation failed",
            content = @Content(schema = @Schema(implementation = ExceptionResponseDTO.class))),
    })
    @PostMapping("/reauthenticate/finish")
    public ResponseEntity<ReauthenticationFinishResponse> finishReauthentication(
            @RequestBody AuthenticationFinishRequest request,
            @Parameter(hidden = true) @CookieValue(name = "passkey_session") String sessionId) {

        log.info("Finishing reauthentication, session: {}", sessionId);

        ReauthenticationFinishResponse response = passkeyService.finishReauthentication(request, sessionId);

        return ResponseEntity.ok()
                .headers(headers -> {
                    headers.add(HttpHeaders.SET_COOKIE, response.getTokenCookie().toString());
                })
                .body(response);
    }

    @Operation(
            summary = "Obtain reauthentication token with password"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Reauthentication successful, sets reauthentication token"),
            @ApiResponse(responseCode = "401", description = "Password was invalid or user was not authenticate",
                    content = @Content(schema = @Schema(implementation = ExceptionResponseDTO.class))),
            @ApiResponse(responseCode = "500", description = "Reauthentication validation failed",
                    content = @Content(schema = @Schema(implementation = ExceptionResponseDTO.class))),
    })
    @PostMapping("/reauthenticate/password")
    public ResponseEntity<ReauthenticationFinishResponse> passwordReauthentication(
            @RequestBody PasswordReauthenticationRequest request) {

        ReauthenticationFinishResponse response = passkeyService.passwordReauthentication(request);

        return ResponseEntity.ok()
                .headers(headers -> {
                    headers.add(HttpHeaders.SET_COOKIE, response.getTokenCookie().toString());
                })
                .body(response);
    }



}

