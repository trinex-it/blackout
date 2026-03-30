package it.trinex.blackout.controller;

import com.sun.net.httpserver.Headers;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import it.trinex.blackout.dto.request.ResetPasswordOTPRequest;
import it.trinex.blackout.dto.request.ValidateOTPRequest;
import it.trinex.blackout.exception.ExceptionResponseDTO;
import it.trinex.blackout.exception.InvalidResetOTPException;
import it.trinex.blackout.service.CookieService;
import it.trinex.blackout.service.PasswordService;
import jakarta.mail.MessagingException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.UnsupportedEncodingException;

@RestController
@RequiredArgsConstructor
@RequestMapping(value = "/password-otp", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Password OTP Management", description = "Endpoints for password reset via email OTP")
@ConditionalOnProperty(prefix = "blackout.mail", name = "enabled", havingValue = "true")
public class PasswordOtpController {

    private final PasswordService passwordService;
    private final CookieService cookieService;

    private static final String RESET_COOKIE_NAME = "reset_key";

    @PostMapping("/request-reset/{subject}")
    @Operation(summary = "Request password reset with OTP", description = """
        Initiates the password reset process by sending a one-time password (OTP) to the user's email.

        This endpoint allows users to request a password reset without being authenticated.
        The user can provide either their username or email address. A 6-digit OTP will be
        generated and sent to the email address associated with the account.

        The OTP is stored in Redis with a 5-minute (300 seconds) expiration time.

        Flow:
        1. User calls this endpoint with username or email
        2. System generates a 6-digit OTP
        3. OTP is sent to the user's email
        4. User calls /password/validate-otp to verify the code
        5. User calls /password/reset-with-otp to complete the reset
        """)
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "OTP sent successfully to user's email"),
        @ApiResponse(responseCode = "500", description = "Failed to send email (SMTP error)",
            content = @Content(schema = @Schema(implementation = ExceptionResponseDTO.class)))
    })
    public ResponseEntity<Void> requestResetPasswordWithOTP(
            @PathVariable @NotBlank(message = "Subject is required.") String subject) throws MessagingException, UnsupportedEncodingException {
        passwordService.sendResetPasswordEmail(subject);
        ResponseCookie resetCookie= cookieService.generateGenericCookie(RESET_COOKIE_NAME, subject, 300L);
        return ResponseEntity.ok()
                .headers(headers -> {
                    headers.add(HttpHeaders.SET_COOKIE, resetCookie.toString());
                })
                .build();
    }

    @PostMapping("/validate-otp")
    @Operation(summary = "Validate OTP code", description = """
        Validates a one-time password (OTP) for password reset.

        This endpoint checks if the provided OTP matches the one stored in Redis
        for the given user. The OTP must have been previously generated via
        /password/request-reset/{subject}.

        This endpoint does not consume the OTP - it only validates it.
        Use /password/reset-with-otp to actually reset the password.
        """)
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "OTP is valid"),
        @ApiResponse(responseCode = "400", description = """
            Invalid request due to:
            - OTP does not match (error code: INVALID_RESET_OTP)
            - OTP has expired
            - Missing required fields (validation error)
            """,
            content = @Content(schema = @Schema(implementation = ExceptionResponseDTO.class)))
    })
    public ResponseEntity<Void> validateOTP(@RequestBody @Valid ValidateOTPRequest request, @Parameter(hidden = true) @CookieValue(name = RESET_COOKIE_NAME, required = false) String resetKey) {
        if (resetKey == null) {
            throw new InvalidResetOTPException("OTP is not valid or expired");
        }
        if (passwordService.checkResetOTP(resetKey, request.getOtp())) {
            return ResponseEntity.ok().build();
        }
        throw new InvalidResetOTPException("OTP is not valid or expired");
    }

    @PostMapping("/reset-with-otp")
    @Operation(summary = "Reset password with OTP", description = """
        Resets the user's password using a valid OTP code.

        This endpoint allows users to reset their password without being authenticated,
        provided they have a valid OTP. The OTP is verified against the stored value
        in Redis and consumed upon successful password reset.

        Security considerations:
        - The OTP must match the one stored in Redis
        - OTP expires after 5 minutes
        - The new password must meet the application's password requirements
        - All existing JWT tokens for the user are revoked after successful reset
        """)
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Password reset successfully"),
        @ApiResponse(responseCode = "400", description = """
            Invalid request due to:
            - OTP does not match or has expired (error code: INVALID_RESET_OTP)
            - Missing required fields (validation error)
            """,
            content = @Content(schema = @Schema(implementation = ExceptionResponseDTO.class))),
        @ApiResponse(responseCode = "404", description = "User account not found",
            content = @Content(schema = @Schema(implementation = ExceptionResponseDTO.class)))
    })
    public ResponseEntity<Void> resetOTP(@RequestBody @Valid ResetPasswordOTPRequest request, @Parameter(hidden = true) @CookieValue(name = RESET_COOKIE_NAME, required = false) String resetKey) {
        if (resetKey == null) {
            throw new InvalidResetOTPException("OTP is not valid or expired");
        }
        passwordService.resetPasswordWithOTP(request, resetKey);
        return ResponseEntity.ok().build();
    }
}
