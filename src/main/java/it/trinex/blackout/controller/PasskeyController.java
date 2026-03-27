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

    /**
     * Finish passkey authentication
     */
    @PostMapping("/reauthenticate/finish")
    public ResponseEntity<Void> finishReauthentication(
            @RequestBody AuthenticationFinishRequest request,
            @Parameter(hidden = true) @CookieValue(name = "passkey_session") String sessionId) {

        log.info("Finishing reauthentication, session: {}", sessionId);

        ResponseCookie cookie = passkeyService.finishReauthentication(request, sessionId);

        return ResponseEntity.ok()
                .headers(headers -> {
                    headers.add(HttpHeaders.SET_COOKIE, cookie.toString());
                })
                .build();
    }



}

