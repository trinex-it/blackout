package it.trinex.blackout.controller;


import io.swagger.v3.oas.annotations.Parameter;
import it.trinex.blackout.dto.request.*;
import it.trinex.blackout.dto.response.AuthResponseDTO;
import it.trinex.blackout.dto.response.AuthenticationStartResponse;
import it.trinex.blackout.dto.response.RegistrationStartResponse;
import it.trinex.blackout.service.CookieService;
import it.trinex.blackout.service.PasskeyService;
import it.trinex.blackout.service.redis.RedisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/passkey")
@RequiredArgsConstructor
@Slf4j
public class PasskeyController {
    
    private final PasskeyService passkeyService;
    private final CookieService cookieService;
    private final RedisService redisService;

    /**
     * Start passkey registration
     */
    @PostMapping("/register/start")
    public ResponseEntity<RegistrationStartResponse> startRegistration() {

        RegistrationStartResponse response = passkeyService.startRegistration();

        return ResponseEntity.ok(response);
    }
    
    /**
     * Finish passkey registration
     */
    @PostMapping("/register/finish")
    public ResponseEntity<Void> finishRegistration(
            @RequestBody RegistrationFinishRequest request) {

        passkeyService.finishRegistration(request);

        return ResponseEntity.ok().build();
    }
    
    /**
     * Start passkey authentication
     */
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

        log.info("Stored challenge in session: {}", response.getSessionId());

        return ResponseEntity.ok()
                .headers(headers -> {
                    headers.add(HttpHeaders.SET_COOKIE, sessionCookie.toString());
                })
                .body(response);
    }
    
    /**
     * Finish passkey authentication
     */
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
    
//    /**
//     * Start reauthentication
//     */
//    @PostMapping("/reauthenticate/start")
//    public ResponseEntity<AuthenticationStartResponse> startReauthentication(
//            @RequestBody ReauthenticationStartRequest request,
//            HttpSession session) {
//
//        User user = (User) session.getAttribute("user");
//        if (user == null) {
//            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
//                    .body(ApiResponse.error("Not authenticated"));
//        }
//
//        log.info("Starting reauthentication for user: {}", user.getUsername());
//
//        AuthenticationStartResponse response = passkeyService.startReauthentication(user);
//
//        return ResponseEntity.ok(ApiResponse.success(response));
//
//    }
    
//    /**
//     * Finish reauthentication
//     */
//    @PostMapping("/reauthenticate/finish")
//    public ResponseEntity<ApiResponse<String>> finishReauthentication(
//            @RequestBody ReauthenticationFinishRequest request,
//            HttpSession session) {
//        try {
//            User user = (User) session.getAttribute("user");
//            if (user == null) {
//                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
//                        .body(ApiResponse.error("Not authenticated"));
//            }
//
//            log.info("Finishing reauthentication for user: {}", user.getUsername());
//
//            User reauthenticatedUser = passkeyService.finishReauthentication(
//                    (AuthenticationFinishRequest) convertToAuthenticationFinishRequest(request),
//                    user);
//
//            // Update session with reauthentication timestamp
//            session.setAttribute("reauthenticatedAt", System.currentTimeMillis());
//
//            return ResponseEntity.ok(
//                    ApiResponse.success("Reauthentication successful!",
//                            reauthenticatedUser.getUsername()));
//        } catch (IllegalArgumentException e) {
//            log.error("Reauthentication finish failed", e);
//            return ResponseEntity.badRequest()
//                    .body(ApiResponse.error(e.getMessage()));
//        } catch (Exception e) {
//            log.error("Unexpected error during reauthentication finish", e);
//            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
//                    .body(ApiResponse.error("Failed to complete reauthentication: " + e.getMessage()));
//        }
//    }
    
//    /**
//     * Check if reauthentication is required (helper method)
//     */
//    @GetMapping("/reauthenticate/required")
//    public ResponseEntity<ApiResponse<Boolean>> isReauthenticationRequired(
//            HttpSession session,
//            @RequestParam(defaultValue = "300000") long maxAge) { // Default: 5 minutes
//        User user = (User) session.getAttribute("user");
//        if (user == null) {
//            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
//                    .body(ApiResponse.error("Not authenticated"));
//        }
//
//        Long reauthenticatedAt = (Long) session.getAttribute("reauthenticatedAt");
//        if (reauthenticatedAt == null) {
//            // Never reauthenticated, require it
//            return ResponseEntity.ok(ApiResponse.success(true));
//        }
//
//        long timeSinceReauth = System.currentTimeMillis() - reauthenticatedAt;
//        boolean required = timeSinceReauth > maxAge;
//
//        return ResponseEntity.ok(ApiResponse.success(required));
//    }
    
//    /**
//     * Helper method to convert ReauthenticationFinishRequest to AuthenticationFinishRequest
//     */
//    private AuthenticationFinishRequest convertToAuthenticationFinishRequest(ReauthenticationFinishRequest request) {
//        AuthenticationFinishRequest authRequest = new AuthenticationFinishRequest();
//        authRequest.setId(request.getId());
//        authRequest.setRawId(request.getRawId());
//        authRequest.setType(request.getType());
//
//        AuthenticationFinishRequest.Response response = new AuthenticationFinishRequest.Response();
//        response.setClientDataJSON(request.getResponse().getClientDataJSON());
//        response.setAuthenticatorData(request.getResponse().getAuthenticatorData());
//        response.setSignature(request.getResponse().getSignature());
//        response.setUserHandle(request.getResponse().getUserHandle());
//
//        authRequest.setResponse(response);
//        return authRequest;
//    }
}

