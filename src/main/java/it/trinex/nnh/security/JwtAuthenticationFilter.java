package it.trinex.nnh.security;

import it.trinex.nnh.model.AuthAccount;
import it.trinex.nnh.service.JWTService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@RequiredArgsConstructor
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JWTService jwtService;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        // Extract token from Authorization header
        String jwt = null;

        if (request.getCookies() != null) {
            for (jakarta.servlet.http.Cookie cookie : request.getCookies()) {
                if ("access_token".equals(cookie.getName())) {
                    jwt = cookie.getValue();
                    break;
                }
            }
        }

        if (jwt == null) {
            log.debug("No access_token cookie found in request to: {}", request.getRequestURI());
            filterChain.doFilter(request, response);
            return;
        }

        try {
            // Token is already extracted

            // Validate token
            if (!jwtService.isTokenValid(jwt)) {
                log.debug("Invalid or expired token for request to: {}", request.getRequestURI());
                filterChain.doFilter(request, response);
                return;
            }

            // Extract user principal from token
            AuthAccount userPrincipal = jwtService.extractUserPrincipal(jwt);

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
