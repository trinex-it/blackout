package it.trinex.nnh.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import it.trinex.queuerbe.annotation.RateLimit;
import it.trinex.queuerbe.dto.request.LoginRequestDTO;
import it.trinex.queuerbe.dto.request.OwnerSignUpRequestDTO;
import it.trinex.queuerbe.dto.response.AuthResponseDTO;
import it.trinex.queuerbe.dto.response.AuthStatusResponseDTO;
import it.trinex.queuerbe.dto.response.ErrorResponse;
import it.trinex.queuerbe.dto.response.OwnerSignUpResponseDTO;
import it.trinex.queuerbe.exception.InvalidTokenException;
import it.trinex.queuerbe.model.AuthAccountType;
import it.trinex.queuerbe.security.JWTUserPrincipal;
import it.trinex.queuerbe.service.JWTService;
import it.trinex.queuerbe.service.OwnerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Endpoints for user authentication and token management")
public class AuthenticationController {

  private final AuthenticationManager authenticationManager;
  private final JWTService jwtService;
  private final OwnerService ownerService;

  /**
   * Owner Sign Up endpoint. Creates an inactive OWNER account and profile.
   */
  @Operation(summary = "Owner sign up", description = "Registers a new Owner account. The account will be inactive until approved.")
  @ApiResponses({
      @ApiResponse(responseCode = "201", description = "Owner account created", content = @Content(mediaType = "application/json", schema = @Schema(implementation = OwnerSignUpResponseDTO.class), examples = @ExampleObject(value = """
          {
            "ownerId": 1,
            "authAccountId": 10,
            "email": "owner@example.com",
            "firstName": "Mario",
            "lastName": "Rossi",
            "active": false,
            "message": "Owner account created. Awaiting activation by admin."
          }
          """))),
      @ApiResponse(responseCode = "400", description = "Validation error", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
      @ApiResponse(responseCode = "409", description = "Conflict (email, fiscal code, or phone already used)", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
  })
  @SecurityRequirements
  @RateLimit(capacity = 5, refillTokens = 1, refillPeriod = 60, refillUnit = ChronoUnit.SECONDS, keyPrefix = "signup-owner")
  @PostMapping("/signup")
  public ResponseEntity<OwnerSignUpResponseDTO> signUpOwner(@Valid @RequestBody OwnerSignUpRequestDTO request) {
    OwnerSignUpResponseDTO response = ownerService.signUpOwner(request);
    return ResponseEntity.status(201).body(response);
  }

  /**
   * Authenticates user with email and password.
   * Returns access and refresh tokens on successful authentication.
   *
   * @param loginRequestDTO login credentials
   * @return AuthResponse containing access token, refresh token, and expiration
   */
  @Operation(summary = "User login")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Successfully authenticated", content = @Content(mediaType = "application/json", schema = @Schema(implementation = AuthResponseDTO.class), examples = @ExampleObject(value = """
          {
            "expiresIn": 604800000
          }
          """))),
      @ApiResponse(responseCode = "400", description = "Invalid request body or validation errors", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class), examples = @ExampleObject(value = """
          {
            "timestamp": "2025-11-13T10:00:00Z",
            "status": 400,
            "category": "VALIDATION_ERROR",
            "message": "Validation failed",
            "path": "/api/auth/login",
            "details": {
              "email": "Email must be valid",
              "password": "Password is required"
            }
          }
          """))),
      @ApiResponse(responseCode = "401", description = "Invalid credentials", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class), examples = @ExampleObject(value = """
          {
            "timestamp": "2025-11-13T10:00:00Z",
            "status": 401,
            "category": "AUTHENTICATION",
            "message": "Invalid email or password",
            "path": "/api/auth/login"
          }
          """))),
      @ApiResponse(responseCode = "429", description = "Rate limit exceeded - too many login attempts", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class), examples = @ExampleObject(value = """
          {
            "timestamp": "2025-11-13T10:00:00Z",
            "status": 429,
            "category": "RATE_LIMIT_EXCEEDED",
            "message": "Too many login attempts. Please try again later.",
            "path": "/api/auth/login",
            "details": {
              "retryAfterSeconds": 30
            }
          }
          """)))
  })
  @SecurityRequirements
  @RateLimit(capacity = 5, refillTokens = 1, refillPeriod = 30, refillUnit = ChronoUnit.SECONDS, keyPrefix = "login")
  @PostMapping("/login")
  public ResponseEntity<AuthResponseDTO> login(@Valid @RequestBody LoginRequestDTO loginRequestDTO) {
    log.info("Login attempt for user: {}", loginRequestDTO.getEmail());

    // Authenticate user with Spring Security
    Authentication authentication = authenticationManager.authenticate(
        new UsernamePasswordAuthenticationToken(
            loginRequestDTO.getEmail(),
            loginRequestDTO.getPassword()));

    // Extract authenticated user principal
    JWTUserPrincipal userPrincipal = (JWTUserPrincipal) authentication.getPrincipal();

    // Generate tokens
    String accessToken = jwtService.generateAccessToken(userPrincipal);
    String refreshToken = jwtService.generateRefreshToken(userPrincipal);

    // Calculate expiration time for client
    AuthAccountType role = jwtService.extractRole(accessToken);
    long accessTokenExpirationMs = jwtService.calculateAccessTokenExpiration(role).toEpochMilli()
        - System.currentTimeMillis();
    long refreshTokenExpirationMs = jwtService.calculateRefreshTokenExpiration(role).toEpochMilli()
        - System.currentTimeMillis();

    log.info("User '{}' logged in successfully", userPrincipal.getUsername());

    ResponseCookie accessCookie = ResponseCookie.from("access_token", accessToken)
        .httpOnly(true)
        .secure(true)
        .path("/")
        .maxAge(Duration.ofMillis(accessTokenExpirationMs))
        .sameSite("None")
        .build();

    // Determine if we should set the refresh token
    boolean rememberMe = Boolean.TRUE.equals(loginRequestDTO.getRememberMe());
    long refreshTokenMaxAge = rememberMe ? Duration.ofMillis(refreshTokenExpirationMs).toSeconds() : 0;

    ResponseCookie refreshCookie = ResponseCookie.from("refresh_token", refreshToken)
        .httpOnly(true)
        .secure(true)
        .path("/")
        .maxAge(refreshTokenMaxAge)
        .sameSite("None")
        .build();

    AuthResponseDTO response = new AuthResponseDTO(accessTokenExpirationMs);

    return ResponseEntity.ok()
        .header(HttpHeaders.SET_COOKIE, accessCookie.toString())
        .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
        .body(response);
  }

  /**
   * Refreshes access token using a valid refresh token.
   * Optionally rotates the refresh token for enhanced security.
   *
   * @param refreshToken token request
   * @throws InvalidTokenException if refresh token is invalid or expired
   */
  @Operation(summary = "Refresh access token", description = """
      Generates a new access token using a valid refresh token.

      For security, this endpoint also rotates the refresh token,
      returning a new refresh token that should be stored by the client.

      Use this endpoint when the access token expires to obtain a new one
      without requiring the user to log in again.

      **Rate Limit:** 10 requests per minute per IP address
      """)
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Successfully refreshed tokens", content = @Content(mediaType = "application/json", schema = @Schema(implementation = AuthResponseDTO.class), examples = @ExampleObject(value = """
          {
            "expiresIn": 604800000
          }
          """))),
      @ApiResponse(responseCode = "401", description = "Missing refresh_token cookie or invalid/expired refresh token", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class), examples = {
          @ExampleObject(name = "Missing Cookie", value = """
              {
                "timestamp": "2025-11-13T10:00:00Z",
                "status": 401,
                "category": "AUTHENTICATION",
                "message": "Missing required cookie: refresh_token",
                "path": "/api/auth/refresh"
              }
              """),
          @ExampleObject(name = "Invalid Token", value = """
              {
                "timestamp": "2025-11-13T10:00:00Z",
                "status": 401,
                "category": "INVALID_TOKEN",
                "message": "Refresh token is invalid or expired",
                "path": "/api/auth/refresh"
              }
              """)
      })),
      @ApiResponse(responseCode = "429", description = "Rate limit exceeded", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
  })
  @SecurityRequirements
  @RateLimit(capacity = 10, refillTokens = 10, refillPeriod = 60, refillUnit = ChronoUnit.SECONDS, keyPrefix = "refresh")
  @PostMapping("/refresh")
  public ResponseEntity<AuthResponseDTO> refresh(@CookieValue(name = "refresh_token") String refreshToken) {

    log.debug("Token refresh attempt");

    // Validate refresh token
    if (!jwtService.isRefreshTokenValid(refreshToken)) {
      log.warn("Invalid or expired refresh token");
      throw new InvalidTokenException("Refresh token is invalid or expired");
    }

    // Extract user from refresh token (no database call needed!)
    JWTUserPrincipal userPrincipal = jwtService.extractUserPrincipal(refreshToken);

    // Generate new access token
    String newAccessToken = jwtService.generateAccessToken(userPrincipal);

    // Calculate expiration time for client
    AuthAccountType role = jwtService.extractRole(newAccessToken);
    long accessTokenExpirationMs = jwtService.calculateAccessTokenExpiration(role).toEpochMilli()
        - System.currentTimeMillis();
    long refreshTokenExpirationMs = jwtService.calculateRefreshTokenExpiration(role).toEpochMilli()
        - System.currentTimeMillis();

    log.info("Token refreshed successfully for user: {}", userPrincipal.getUsername());

    ResponseCookie accessCookie = ResponseCookie.from("access_token", newAccessToken)
        .httpOnly(true)
        .secure(true)
        .path("/")
        .maxAge(Duration.ofMillis(accessTokenExpirationMs))
        .sameSite("None")
        .build();

    ResponseCookie refreshCookie = ResponseCookie.from("refresh_token", refreshToken)
        .httpOnly(true)
        .secure(true)
        .path("/")
        .maxAge(Duration.ofMillis(refreshTokenExpirationMs))
        .sameSite("None")
        .build();

    AuthResponseDTO response = new AuthResponseDTO(accessTokenExpirationMs);

    return ResponseEntity.ok()
        .header(HttpHeaders.SET_COOKIE, accessCookie.toString())
        .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
        .body(response);
  }

  @Operation(summary = "Get authentication status", description = "Checks if the user is authenticated and returns user details.")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "User status retrieved", content = @Content(mediaType = "application/json", schema = @Schema(implementation = AuthStatusResponseDTO.class))),
      @ApiResponse(responseCode = "401", description = "Not authenticated", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
  })
  @GetMapping("/status")
  public ResponseEntity<AuthStatusResponseDTO> getAuthStatus(Authentication authentication) {
    if (authentication == null || !(authentication.getPrincipal() instanceof JWTUserPrincipal)) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(AuthStatusResponseDTO.builder()
          .authenticated(false)
          .build());
    }

    JWTUserPrincipal userPrincipal = (JWTUserPrincipal) authentication.getPrincipal();

    AuthStatusResponseDTO response = AuthStatusResponseDTO.builder()
        .authenticated(true)
        .userId(userPrincipal.id())
        .email(userPrincipal.getUsername())
        .role(userPrincipal.getAuthorities().stream().findFirst().map(Object::toString)
            .orElse("UNKNOWN"))
        .ownerId(userPrincipal.getOwnerId().orElse(null))
        .firstName(userPrincipal.firstName())
        .lastName(userPrincipal.lastName())
        .build();

    return ResponseEntity.ok(response);
  }

  /**
   * Logs out the user by clearing authentication cookies.
   * 
   * @return ResponseEntity with no content
   */
  @Operation(summary = "Logout", description = "Logs out the user by clearing authentication cookies.")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Successfully logged out"),
      @ApiResponse(responseCode = "401", description = "Not authenticated")
  })
  @PostMapping("/logout")
  public ResponseEntity<Void> logout() {
    log.info("User logged out");

    // Create cookies with maxAge(0) to delete them
    ResponseCookie accessCookie = ResponseCookie.from("access_token", "")
        .httpOnly(true)
        .secure(true)
        .path("/")
        .maxAge(0)
        .sameSite("None")
        .build();

    ResponseCookie refreshCookie = ResponseCookie.from("refresh_token", "")
        .httpOnly(true)
        .secure(true)
        .path("/")
        .maxAge(0)
        .sameSite("None")
        .build();

    return ResponseEntity.ok()
        .header(HttpHeaders.SET_COOKIE, accessCookie.toString())
        .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
        .build();
  }
}
