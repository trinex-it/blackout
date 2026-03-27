package it.trinex.blackout.service;

import it.trinex.blackout.properties.JwtProperties;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CookieService {
    public static final String ACCESS_COOKIE_NAME = "access_token";
    public static final String REFRESH_COOKIE_NAME = "refresh_token";

    private final JwtProperties jwtProperties;

    public ResponseCookie generateAccessCookie(String token) {
        return ResponseCookie.from(ACCESS_COOKIE_NAME, token)
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(token == null ? 0L : jwtProperties.getAccessTokenExp() / 1000)
                .sameSite("Lax")
                .build();
    }

    public ResponseCookie generateRefreshCookie(String token) {
        return ResponseCookie.from(REFRESH_COOKIE_NAME, token)
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(token == null ? 0L : jwtProperties.getRefreshTokenExp() / 1000)
                .sameSite("Lax")
                .build();
    }

    public ResponseCookie generateGenericCookie(String cookieName, String value, Long maxAgeSeconds) {
        return ResponseCookie.from(cookieName, value)
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(maxAgeSeconds)
                .sameSite("Lax")
                .build();
    }
}
