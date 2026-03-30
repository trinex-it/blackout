package it.trinex.blackout.autoconfig;

import it.trinex.blackout.controller.PasskeyController;
import it.trinex.blackout.properties.WebAuthnProperties;
import it.trinex.blackout.repository.PasskeyRepository;
import it.trinex.blackout.service.CookieService;
import it.trinex.blackout.service.CurrentUserService;
import it.trinex.blackout.service.JwtService;
import it.trinex.blackout.service.PasskeyService;
import it.trinex.blackout.service.redis.RedisService;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.core.userdetails.UserDetailsService;
import tools.jackson.databind.ObjectMapper;

@AutoConfiguration
@EnableConfigurationProperties(WebAuthnProperties.class)
@ConditionalOnProperty(prefix = "blackout.webauthn", name = "enabled", havingValue = "true")
public class WebAuthnAutoconfig {
    @Bean
    public PasskeyService passkeyService(PasskeyRepository passkeyRepository, CurrentUserService currentUserService, WebAuthnProperties webAuthnProperties, UserDetailsService userDetailsService, JwtService jwtService, ObjectMapper objectMapper, RedisTemplate<String, String> redisTemplate, CookieService cookieService) {
        return new PasskeyService(passkeyRepository, currentUserService, webAuthnProperties, userDetailsService, jwtService, objectMapper, cookieService);
    }

    @Bean
    @ConditionalOnProperty(prefix = "blackout.webauthn", name = "enabled", havingValue = "true", matchIfMissing = false)
    public PasskeyController passkeyController(PasskeyService passkeyService, CookieService cookieService, RedisService redisService) {
        return new PasskeyController(passkeyService, cookieService, redisService);
    }
}
