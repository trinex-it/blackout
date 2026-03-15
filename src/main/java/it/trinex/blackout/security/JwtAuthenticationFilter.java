package it.trinex.blackout.security;

import it.trinex.blackout.dto.response.AuthResponseDTO;
import it.trinex.blackout.service.AuthService;
import it.trinex.blackout.service.CookieService;
import it.trinex.blackout.service.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * JWT authentication filter that supports both Authorization header and Cookies.
 * If configured, it can automatically refresh the access token using a refresh token from cookies.
 */
@Slf4j
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final AuthService authService;
    private final CookieService cookieService;
    private final boolean autoRefresh;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        String accessToken = null;
        String refreshToken = null;
        boolean fromCookie = false;

        // 1. Try to extract token from Authorization header
        String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            accessToken = authHeader.substring(7);
        }

        // 2. If not found in header, try to extract from cookies
        if (accessToken == null && request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if (CookieService.ACCESS_COOKIE_NAME.equals(cookie.getName())) {
                    accessToken = cookie.getValue();
                    fromCookie = true;
                } else if (CookieService.REFRESH_COOKIE_NAME.equals(cookie.getName())) {
                    refreshToken = cookie.getValue();
                    fromCookie = true;
                }
            }
        }

        if (accessToken == null && refreshToken == null) {
            filterChain.doFilter(request, response);
            return;
        }

        // 3. Handle Auto-Refresh (only if from cookie and configured)
        if (fromCookie && autoRefresh && authService != null && cookieService != null) {
            if (accessToken == null || !jwtService.isTokenValid(accessToken)) {
                if (refreshToken != null && jwtService.isRefreshTokenValid(refreshToken)) {
                    try {
                        log.debug("Access token invalid, attempting refresh for: {}", request.getRequestURI());
                        AuthResponseDTO authResponse = authService.refreshToken(refreshToken);
                        String newAccessToken = authResponse.access_token();
                        
                        ResponseCookie accessCookie = cookieService.generateAccessCookie(newAccessToken);
                        response.addHeader(HttpHeaders.SET_COOKIE, accessCookie.toString());
                        accessToken = newAccessToken;
                    } catch (Exception e) {
                        log.debug("Token refresh failed: {}", e.getMessage());
                    }
                }
            }
        }

        // 4. Validate and Set Authentication
        if (accessToken != null) {
            try {
                if (jwtService.isTokenValid(accessToken)) {
                    UserDetails userPrincipal = jwtService.extractUserPrincipal(accessToken);

                    if (SecurityContextHolder.getContext().getAuthentication() == null) {
                        UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                                userPrincipal,
                                null,
                                userPrincipal.getAuthorities());

                        authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                        SecurityContextHolder.getContext().setAuthentication(authToken);

                        log.debug("User '{}' authenticated successfully for: {}",
                                userPrincipal.getUsername(),
                                request.getRequestURI());
                    }
                } else {
                    log.debug("Invalid or expired token for request to: {}", request.getRequestURI());
                }
            } catch (Exception e) {
                log.error("Cannot set user authentication: {}", e.getMessage());
            }
        }

        filterChain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return request.getRequestURI().startsWith("/api/auth");
    }
}
