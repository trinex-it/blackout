package it.trinex.blackout.security;

import it.trinex.blackout.dto.response.AuthResponseDTO;
import it.trinex.blackout.service.WebAuthnHelperService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;

@Slf4j
@RequiredArgsConstructor
public class WebAuthnJwtSuccessHandler implements AuthenticationSuccessHandler {

    private final WebAuthnHelperService webAuthnHelperService;
    private final ObjectMapper objectMapper;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        log.info("WebAuthn authentication successful for user: {}", authentication.getName());
        
        BlackoutUserPrincipal userPrincipal = (BlackoutUserPrincipal) authentication.getPrincipal();
        
        AuthResponseDTO authResponseDTO = webAuthnHelperService.generateTokensForPrincipal(userPrincipal, false);
        
        response.setStatus(HttpStatus.OK.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), authResponseDTO);
    }
}
