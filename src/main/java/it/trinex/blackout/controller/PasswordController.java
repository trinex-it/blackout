package it.trinex.blackout.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import it.trinex.blackout.dto.request.ResetPasswordRequest;
import it.trinex.blackout.exception.ExceptionResponseDTO;
import it.trinex.blackout.service.PasswordService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping(value = "/password", produces =  MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Password Management", description = "Endpoints for password reset and management")
public class PasswordController {
    private final PasswordService passwordService;

    @PostMapping(value = "/reset")
    @Operation(summary = "Reset password", description = """
        Resets the password for the currently authenticated user.

        This endpoint allows users to change their password by providing the current
        password along with the new password. Both passwords must be provided and
        the new password must be confirmed by repeating it.

        The user must be authenticated to use this endpoint. The old password is
        validated against the stored password hash before allowing the change.

        Security considerations:
        - The old password must match the current password
        - The new password must be provided twice to prevent typos
        - All passwords are validated using Jakarta Bean Validation (@NotNull)
        - Passwords are hashed using the configured PasswordEncoder before storage
        """)
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Password reset successfully"),
        @ApiResponse(responseCode = "400", description = """
            Invalid request due to:
            - Old password does not match the current password (error code: INVALID_PASSWORD)
            - New password and repeat new password do not match
            - Missing required fields (validation error)
            """,
            content = @Content(schema = @Schema(implementation = ExceptionResponseDTO.class))),
        @ApiResponse(responseCode = "401", description = "User not authenticated or invalid credentials",
            content = @Content(schema = @Schema(implementation = ExceptionResponseDTO.class))),
        @ApiResponse(responseCode = "404", description = "User account not found",
            content = @Content(schema = @Schema(implementation = ExceptionResponseDTO.class)))
    })
    public ResponseEntity<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest resetPasswordRequest) {
        passwordService.resetPasswordWithoutOTP(resetPasswordRequest);
        return ResponseEntity.ok().build();
    }

    @PostMapping(value = "/enable-passwordless")
    @Operation(summary = "Reset password", description = """
        Enable passwordless login:
        - User needs to create at least one passkey to enable this feature
        """)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Passwordless login enabled successfully"),
            @ApiResponse(responseCode = "403", description = "User didnt create any passkeys"),
            @ApiResponse(responseCode = "401", description = "User not authenticated or invalid credentials",
                    content = @Content(schema = @Schema(implementation = ExceptionResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "User account not found",
                    content = @Content(schema = @Schema(implementation = ExceptionResponseDTO.class)))
    })
    public ResponseEntity<Void> enablePasswordless() {
        passwordService.enablePasswordlessLogin();
        return ResponseEntity.ok().build();
    }


}
