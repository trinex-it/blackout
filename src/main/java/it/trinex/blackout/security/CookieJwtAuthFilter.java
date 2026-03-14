package it.trinex.blackout.security;

import it.trinex.blackout.dto.response.AuthResponseDTO;
import it.trinex.blackout.service.AuthService;
import it.trinex.blackout.service.CookieService;
import it.trinex.blackout.service.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@RequiredArgsConstructor
public class CookieJwtAuthFilter extends OncePerRequestFilter implements JwtAuthenticationFilter {

    private final JwtService jwtService;
    private final AuthService authService;
    private final CookieService cookieService;
    @Value("${blackout.cookie.auto-refresh}")
    private final boolean autoRefresh;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        // Extract access and refresh tokens from cookies
        String accessToken = null;
        String refreshToken = null;

        if (request.getCookies() != null) {
            for (jakarta.servlet.http.Cookie cookie : request.getCookies()) {
                if (CookieService.ACCESS_COOKIE_NAME.equals(cookie.getName())) {
                    accessToken = cookie.getValue();
                } else if (CookieService.REFRESH_COOKIE_NAME.equals(cookie.getName())) {
                    refreshToken = cookie.getValue();
                }
            }
        }

        if(autoRefresh) {
            String newToken = null;

            if (accessToken == null || !jwtService.isTokenValid(accessToken)) {
                if (refreshToken != null && jwtService.isRefreshTokenValid(refreshToken)) {
                    //Access token is invalid and refresh token is valid
                    log.debug("Access token missing, attempting refresh for: {}", request.getRequestURI());
                    AuthResponseDTO authResponse = authService.refreshToken(refreshToken);
                    newToken = authResponse.access_token();
                } else {
                    //Both tokens are invalid
                    log.debug("No {} cookie found for request to: {}", CookieService.REFRESH_COOKIE_NAME, request.getRequestURI());
                    filterChain.doFilter(request, response);
                    return;
                }
            }

            // Set new access token cookie if we performed a refresh
            if (newToken != null) {
                ResponseCookie accessCookie = cookieService.generateAccessCookie(newToken);
                response.addHeader(HttpHeaders.SET_COOKIE, accessCookie.toString());
                accessToken = newToken;
            }
        }

        try {
            // Validate token
            if (!jwtService.isTokenValid(accessToken)) {
                log.debug("Invalid or expired token for request to: {}", request.getRequestURI());
                filterChain.doFilter(request, response);
                return;
            }

            // Extract user principal from token
            UserDetails userPrincipal = jwtService.extractUserPrincipal(accessToken);

            // Check if user is not already authenticated
            if (SecurityContextHolder.getContext().getAuthentication() == null) {
                // Create authentication token
                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        userPrincipal,
                        null,
                        userPrincipal.getAuthorities());

                // Set request details
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                // Set authentication in SecurityContext
                SecurityContextHolder.getContext().setAuthentication(authToken);

                log.debug("User '{}' authenticated successfully for: {}",
                        userPrincipal.getUsername(),
                        request.getRequestURI());

                // Log authorities for troubleshooting authorization issues
                log.debug("Assigned authorities for user '{}': {}",
                        userPrincipal.getUsername(), userPrincipal.getAuthorities());
            }

        } catch (Exception e) {
            log.error("Cannot set user authentication: {}", e.getMessage());
        }

        filterChain.doFilter(request, response);
    }
}
