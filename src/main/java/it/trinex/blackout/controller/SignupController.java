package it.trinex.blackout.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import it.trinex.blackout.exception.BlackoutException;
import it.trinex.blackout.model.AuthAccount;
import it.trinex.blackout.properties.SignupProperties;
import it.trinex.blackout.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;

@RestController
@RequestMapping("/api/signup")
@ConditionalOnProperty(name = "blackout.signup.enabled", havingValue = "true", matchIfMissing = false)
@RequiredArgsConstructor
@Tag(name = "Signup", description = "Endpoints for default user registration")
class SignupController {

    private final PasswordEncoder passwordEncoder;
    private final AuthService authService;
    private final SignupProperties signupProperties;

    @PostMapping()
    @Operation(summary = "Register user", description = "Create a new account with email and password")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Account created successfully",
                    content = @Content(schema = @Schema(implementation = AuthAccount.class))),
            @ApiResponse(responseCode = "400", description = "Validation failed or passwords don't match")
    })
    public ResponseEntity<AuthAccount> signup(@Valid @RequestBody SignupRequestDTO request) {
        // Validate password confirmation
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new BlackoutException(HttpStatus.BAD_REQUEST, "PASSWORDS_DO_NOT_MATCH", "Passwords do not match");
        }

        // Create new AuthAccount
        AuthAccount authAccount = new AuthAccount();
        authAccount.setUsername(request.getEmail());
        authAccount.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        authAccount.setRole(signupProperties.getDefaultRole());
        authAccount.setFirstName("");
        authAccount.setLastName("");
        authAccount.setActive(true);

        AuthAccount saved = authService.registerAuthAccount(authAccount);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }
}
