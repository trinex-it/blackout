package it.trinex.nnh.security.jwt;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * JWT Authentication Filter that extracts and validates JWT from cookies.
 *
 * <p>This filter runs once per request and:</p>
 * <ol>
 *   <li>Extracts the access_token from cookies</li>
 *   <li>Validates the token using JwtService</li>
 *   <li>Loads user details from UserDetailsService</li>
 *   <li>Sets authentication in SecurityContext</li>
 * </ol>
 *
 * <p>The filter only processes requests that have a valid access_token cookie.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        final String accessToken = extractAccessToken(request);

        if (accessToken != null && jwtService.isTokenValid(accessToken)) {
            String username = jwtService.extractUsername(accessToken);

            if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails userDetails = userDetailsService.loadUserByUsername(username);

                if (userDetails instanceof JwtUserPrincipal) {
                    JwtUserPrincipal principal = (JwtUserPrincipal) userDetails;
                    UsernamePasswordAuthenticationToken authToken =
                            new UsernamePasswordAuthenticationToken(
                                    principal,
                                    null,
                                    principal.getAuthorities()
                            );
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);

                    log.debug("Set authentication for user: {}", username);
                }
            }
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Extract access token from the request cookies.
     *
     * @param request the HTTP request
     * @return the access token, or null if not found
     */
    private String extractAccessToken(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("access_token".equals(cookie.getName())) {
                    log.debug("Found access_token cookie");
                    return cookie.getValue();
                }
            }
        }
        return null;
    }
}
