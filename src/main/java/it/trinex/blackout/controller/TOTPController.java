package it.trinex.blackout.controller;

import dev.samstevens.totp.exceptions.QrGenerationException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import it.trinex.blackout.dto.request.Disable2FA;
import it.trinex.blackout.dto.request.Disable2FAWithRecoveryRequest;
import it.trinex.blackout.dto.request.Enable2FARequest;
import it.trinex.blackout.dto.response.TFAEnabledResponse;
import it.trinex.blackout.dto.response.TOTPRegistrationResponse;
import it.trinex.blackout.repository.AuthAccountRepo;
import it.trinex.blackout.service.CurrentUserService;
import it.trinex.blackout.service.TOTPService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("${blackout.baseurl:/api}" + "/2fa")
@Tag(name = "Two-Factor Authentication", description = "Endpoints for TOTP-based two-factor authentication setup and management")
public class TOTPController {

    private final TOTPService totpService;

    @PostMapping
    @Operation(summary = "Enable two-factor authentication", description = """
        Enables TOTP-based two-factor authentication for the current user.

        This endpoint validates the TOTP code provided by the user along with
        the secret key. Once enabled, the user will be required to provide a
        valid TOTP code during login in addition to their password.

        The user should first call GET /api/2fa to obtain the secret key and
        QR code, then use an authenticator app (e.g., Google Authenticator,
        Authy) to generate TOTP codes.
        """)
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "2FA enabled successfully",
            content = @Content(schema = @Schema(implementation = TFAEnabledResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid TOTP code or secret"),
        @ApiResponse(responseCode = "401", description = "User not authenticated")
    })
    public ResponseEntity<TFAEnabledResponse> enable2FA(@RequestBody @Valid Enable2FARequest request) throws QrGenerationException {
        log.debug("2FA enable attempt");
        TFAEnabledResponse response = totpService.enable2FA(request.getSecret(), request.getTotp());
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @GetMapping
    @Operation(summary = "Generate TOTP secret and QR code", description = """
        Generates a new TOTP secret key and QR code for setting up two-factor authentication.

        This endpoint initializes the 2FA setup process by generating a unique secret key
        for the current user and a corresponding QR code that can be scanned by
        authenticator apps (Google Authenticator, Authy, etc.).

        After calling this endpoint, the user should:
        1. Scan the QR code with their authenticator app
        2. Call POST /api/2fa with the secret and a generated TOTP code to complete setup

        Note: This endpoint does not enable 2FA by itself. The user must confirm by
        providing a valid TOTP code via the POST endpoint.
        """)
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "TOTP secret and QR code generated successfully",
            content = @Content(schema = @Schema(implementation = TOTPRegistrationResponse.class))),
        @ApiResponse(responseCode = "400", description = "User not authenticated"),
        @ApiResponse(responseCode = "409", description = "2FA is already enabled for this user")
    })
    public ResponseEntity<TOTPRegistrationResponse> request2FA() throws QrGenerationException {
        log.debug("TOTP generation request");
        TOTPRegistrationResponse response = totpService.generateTOTP();
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @PostMapping("/disable")
    @Operation(summary = "Disable two-factor authentication", description = """
        Disables TOTP-based two-factor authentication for the current user.

        This endpoint validates the TOTP code provided by the user to ensure
        they have access to their authenticator app before disabling 2FA.

        Once disabled, the user will only need their email and password to log in.
        """)
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "2FA disabled successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid TOTP code"),
        @ApiResponse(responseCode = "401", description = "User not authenticated")
    })
    public ResponseEntity<Void> disable2FA(@RequestBody @Valid Disable2FA request) {
        log.debug("2FA disable attempt");
        totpService.disable2FA(request.getCode());
        return ResponseEntity.status(HttpStatus.OK).build();
    }

    @PostMapping("/disable-recovery")
    @Operation(summary = "Disable 2FA using recovery code", description = """
        Disables two-factor authentication using a recovery code.

        This endpoint is designed for users who have lost access to their
        authenticator app. It validates the user's credentials (email/username
        and password) along with a valid recovery code to disable 2FA.

        Once used, the recovery code is invalidated and cannot be reused.
        """)
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "2FA disabled successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid credentials or recovery code"),
        @ApiResponse(responseCode = "401", description = "Authentication failed")
    })
    public ResponseEntity<Void> disable2FAWithRecovery(@RequestBody @Valid Disable2FAWithRecoveryRequest request) {
        log.debug("2FA disable with recovery code attempt");
        totpService.disable2FAWithRecoveryCode(request);
        return ResponseEntity.status(HttpStatus.OK).build();
    }

}
