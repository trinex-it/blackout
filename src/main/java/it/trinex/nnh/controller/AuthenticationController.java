package it.trinex.nnh.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import it.trinex.nnh.exception.NNHException;
import it.trinex.nnh.model.AuthAccount;
import it.trinex.nnh.service.AuthService;
import jakarta.annotation.PostConstruct;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@ConditionalOnMissingBean(name = "authController")
@Tag(name = "Authentication", description = "Endpoints for user authentication and token management")
public class AuthenticationController {

    private final AuthService authService;
    private final PasswordEncoder passwordEncoder;

    @Value("${nnh.default.signup.role:USER}")
    private String defaultSignupRole;

    @PostMapping("/login")
    @Operation(summary = "Login user", description = "Authenticate user with email and password, returns JWT tokens")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Login successful",
            content = @Content(schema = @Schema(implementation = AuthResponseDTO.class))),
        @ApiResponse(responseCode = "401", description = "Invalid credentials")
    })
    public ResponseEntity<AuthResponseDTO> login(@Valid @RequestBody LoginRequestDTO request) {
        AuthResponseDTO response = authService.login(
                request.getEmail(),
                request.getPassword(),
                request.getRememberMe()
        );
        return ResponseEntity.ok(response);
    }

    @PostMapping("/signup")
    @Operation(summary = "Register user", description = "Create a new account with email and password")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Account created successfully",
            content = @Content(schema = @Schema(implementation = AuthAccount.class))),
        @ApiResponse(responseCode = "400", description = "Validation failed or passwords don't match")
    })
    public ResponseEntity<AuthAccount> signup(@Valid @RequestBody SignupRequestDTO request) {
        // Validate password confirmation
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new NNHException(HttpStatus.BAD_REQUEST, "PASSWORDS_DO_NOT_MATCH", "Passwords do not match");
        }

        // Create new AuthAccount
        AuthAccount authAccount = new AuthAccount();
        authAccount.setUsername(request.getEmail());
        authAccount.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        authAccount.setRole(defaultSignupRole);
        authAccount.setFirstName("");
        authAccount.setLastName("");
        authAccount.setActive(true);

        AuthAccount saved = authService.registerAuthAccount(authAccount);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }
}