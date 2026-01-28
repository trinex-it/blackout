package it.trinex.nnh.service;

import it.trinex.nnh.config.SecurityProperties;
import it.trinex.nnh.dto.response.AuthResponse;
import it.trinex.nnh.exception.AuthenticationException;
import it.trinex.nnh.security.jwt.JwtService;
import it.trinex.nnh.security.jwt.JwtUserPrincipal;
import it.trinex.nnh.util.CookieUtils;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;

/**
 * Implementation of AuthenticationService.
 */
@Service
@RequiredArgsConstructor
public class AuthenticationServiceImpl implements AuthenticationService {

    private final JwtService jwtService;
    private final CookieUtils cookieUtils;
    private final SecurityProperties securityProperties;

    @Override
    public AuthResponse createTokens(JwtUserPrincipal userPrincipal, boolean rememberMe) {
        String accessToken = jwtService.generateAccessToken(userPrincipal);
        String refreshToken = jwtService.generateRefreshToken(userPrincipal);

        // Get access token expiration for response
        long accessExpiration = securityProperties.getJwt().getAccessToken()
                .getExpirationForRole(userPrincipal.getRole());

        return AuthResponse.builder()
                .userId(userPrincipal.getId())
                .email(userPrincipal.getUsername())
                .role(userPrincipal.getRole())
                .ownerId(userPrincipal.getOwnerId())
                .firstName(userPrincipal.getFirstName())
                .lastName(userPrincipal.getLastName())
                .expiresIn(accessExpiration)
                .build();
    }

    @Override
    public void setTokenCookies(HttpServletResponse response, AuthResponse authResponse, boolean rememberMe) {
        // Note: Tokens need to be generated here since we don't store them in the response
        // This is a simplified version - in practice, you'd need the user principal
        // For the full implementation, the controller should handle this
    }

    @Override
    public void setAccessTokenCookie(HttpServletResponse response, AuthResponse authResponse) {
        // Similar to above - tokens need to be generated from user principal
    }

    @Override
    public void clearTokenCookies(HttpServletResponse response) {
        response.addHeader(HttpHeaders.SET_COOKIE, cookieUtils.clearAccessTokenCookie().toString());
        response.addHeader(HttpHeaders.SET_COOKIE, cookieUtils.clearRefreshTokenCookie().toString());
    }

    @Override
    public AuthResponse refreshAccessToken(String refreshToken) {
        if (!jwtService.isRefreshTokenValid(refreshToken)) {
            throw new AuthenticationException("Invalid refresh token");
        }

        JwtUserPrincipal userPrincipal = jwtService.extractUserPrincipal(refreshToken);

        // Get access token expiration for response
        long accessExpiration = securityProperties.getJwt().getAccessToken()
                .getExpirationForRole(userPrincipal.getRole());

        return AuthResponse.builder()
                .userId(userPrincipal.getId())
                .email(userPrincipal.getUsername())
                .role(userPrincipal.getRole())
                .ownerId(userPrincipal.getOwnerId())
                .firstName(userPrincipal.getFirstName())
                .lastName(userPrincipal.getLastName())
                .expiresIn(accessExpiration)
                .build();
    }

    @Override
    public JwtUserPrincipal getCurrentUser() {
        return jwtService.getCurrentUser();
    }
}
