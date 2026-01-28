package it.trinex.nnh.controller;

import it.trinex.nnh.dto.request.LoginRequest;
import it.trinex.nnh.dto.request.SignupRequest;
import it.trinex.nnh.dto.response.AuthResponse;
import it.trinex.nnh.dto.response.SignupResponse;
import it.trinex.nnh.dto.response.UserDto;
import it.trinex.nnh.model.AuthAccount;
import it.trinex.nnh.model.AuthAccountType;
import it.trinex.nnh.security.jwt.JwtService;
import it.trinex.nnh.security.jwt.JwtUserPrincipal;
import it.trinex.nnh.service.AuthenticationService;
import it.trinex.nnh.service.UserService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * Authentication controller.
 *
 * <p>This controller provides authentication endpoints:</p>
 * <ul>
 *   <li>POST /api/auth/signup - Create a new user account</li>
 *   <li>POST /api/auth/login - Login with email and password</li>
 *   <li>POST /api/auth/refresh - Refresh access token using refresh token cookie</li>
 *   <li>POST /api/auth/logout - Logout by clearing cookies</li>
 *   <li>GET /api/auth/status - Get current authentication status</li>
 * </ul>
 *
 * <p><b>Note:</b> This controller is only loaded when {@code nnh.security.expose-controller=true}.</p>
 * <p>To use a custom controller, set {@code nnh.security.expose-controller=false} and inject
 * {@link AuthenticationService} into your own controller.</p>
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Validated
@ConditionalOnProperty(prefix = "nnh.security", name = "expose-controller", havingValue = "true", matchIfMissing = true)
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final AuthenticationService authenticationService;
    private final UserService userService;

    /**
     * Signup endpoint.
     *
     * <p>Creates a new user account, generates JWT tokens, and sets them as HTTP-only cookies.
     * The user is automatically logged in after signup.</p>
     *
     * @param request the signup request
     * @param response the HTTP response
     * @return signup response with user info (tokens are in cookies)
     */
    @PostMapping("/signup")
    public ResponseEntity<SignupResponse> signup(
            @Valid @RequestBody SignupRequest request,
            HttpServletResponse response
    ) {
        // Determine account type
        AuthAccountType accountType = AuthAccountType.USER;
        if (request.getAccountType() != null && !request.getAccountType().isEmpty()) {
            try {
                accountType = AuthAccountType.valueOf(request.getAccountType().toUpperCase());
            } catch (IllegalArgumentException e) {
                // Default to USER if invalid type
                accountType = AuthAccountType.USER;
            }
        }

        // Create user
        AuthAccount authAccount = userService.createUser(
                request.getEmail(),
                request.getPassword(),
                accountType
        );

        // Create user principal
        JwtUserPrincipal userPrincipal = JwtUserPrincipal.create(authAccount);

        // Generate tokens
        String accessToken = jwtService.generateAccessToken(userPrincipal);
        String refreshToken = jwtService.generateRefreshToken(userPrincipal);

        // Get access token expiration
        long accessExpiration = jwtService.extractExpiration(accessToken).getTime() - System.currentTimeMillis();

        // Build response
        SignupResponse signupResponse = SignupResponse.builder()
                .userId(authAccount.getId())
                .email(authAccount.getEmail())
                .role(authAccount.getType().name())
                .expiresIn(accessExpiration)
                .message("User created successfully")
                .build();

        // Set cookies
        response.addHeader(HttpHeaders.SET_COOKIE,
                createAccessTokenCookie(accessToken, accessExpiration).toString());
        response.addHeader(HttpHeaders.SET_COOKIE,
                createRefreshTokenCookie(refreshToken, false).toString());

        return ResponseEntity.status(201).body(signupResponse);
    }

    /**
     * Login endpoint.
     *
     * <p>Authenticates user with email and password, generates JWT tokens, and sets them as HTTP-only cookies.</p>
     *
     * @param request the login request
     * @param response the HTTP response
     * @return authentication response with user info (tokens are in cookies)
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletResponse response
    ) {
        // Authenticate user
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        JwtUserPrincipal userPrincipal = (JwtUserPrincipal) authentication.getPrincipal();

        // Generate tokens
        String accessToken = jwtService.generateAccessToken(userPrincipal);
        String refreshToken = jwtService.generateRefreshToken(userPrincipal);

        // Get access token expiration
        long accessExpiration = jwtService.extractExpiration(accessToken).getTime() - System.currentTimeMillis();

        // Build response
        AuthResponse authResponse = AuthResponse.fromPrincipal(userPrincipal, accessExpiration);

        // Set cookies
        response.addHeader(HttpHeaders.SET_COOKIE,
                createAccessTokenCookie(accessToken, accessExpiration).toString());
        response.addHeader(HttpHeaders.SET_COOKIE,
                createRefreshTokenCookie(refreshToken, request.isRememberMe()).toString());

        return ResponseEntity.ok(authResponse);
    }

    /**
     * Refresh token endpoint.
     *
     * <p>Refreshes the access token using the refresh token from cookies.</p>
     *
     * @param refreshToken the refresh token from cookies
     * @param response the HTTP response
     * @return authentication response with user info
     */
    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refreshToken(
            @CookieValue(value = "refresh_token", required = false) String refreshToken,
            HttpServletResponse response
    ) {
        if (refreshToken == null) {
            return ResponseEntity.status(401).build();
        }

        // Validate refresh token and extract user principal
        if (!jwtService.isRefreshTokenValid(refreshToken)) {
            return ResponseEntity.status(401).build();
        }

        JwtUserPrincipal userPrincipal = jwtService.extractUserPrincipal(refreshToken);

        // Generate new access token
        String accessToken = jwtService.generateAccessToken(userPrincipal);
        long accessExpiration = jwtService.extractExpiration(accessToken).getTime() - System.currentTimeMillis();

        // Build response
        AuthResponse authResponse = AuthResponse.fromPrincipal(userPrincipal, accessExpiration);

        // Set new access token cookie
        response.addHeader(HttpHeaders.SET_COOKIE,
                createAccessTokenCookie(accessToken, accessExpiration).toString());

        return ResponseEntity.ok(authResponse);
    }

    /**
     * Logout endpoint.
     *
     * <p>Clears the authentication cookies.</p>
     *
     * @param response the HTTP response
     * @return empty response with cleared cookies
     */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletResponse response) {
        authenticationService.clearTokenCookies(response);
        return ResponseEntity.ok().build();
    }

    /**
     * Get current authentication status.
     *
     * <p>Returns the current authenticated user's information.</p>
     *
     * @param userPrincipal the authenticated user principal
     * @return user information
     */
    @GetMapping("/status")
    public ResponseEntity<UserDto> getCurrentUser(@AuthenticationPrincipal JwtUserPrincipal userPrincipal) {
        if (userPrincipal == null) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(UserDto.fromPrincipal(userPrincipal));
    }

    private org.springframework.http.ResponseCookie createAccessTokenCookie(String token, long maxAgeMs) {
        return org.springframework.http.ResponseCookie.from("access_token", token)
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(java.time.Duration.ofMillis(maxAgeMs))
                .sameSite("None")
                .build();
    }

    private org.springframework.http.ResponseCookie createRefreshTokenCookie(String token, boolean rememberMe) {
        org.springframework.http.ResponseCookie.ResponseCookieBuilder builder =
                org.springframework.http.ResponseCookie.from("refresh_token", token)
                        .httpOnly(true)
                        .secure(true)
                        .path("/")
                        .sameSite("None");

        if (rememberMe) {
            builder.maxAge(java.time.Duration.ofDays(90));
        }
        // else: session cookie (no maxAge)

        return builder.build();
    }
}
