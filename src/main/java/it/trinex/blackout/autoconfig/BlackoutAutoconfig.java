package it.trinex.blackout.autoconfig;

import it.trinex.blackout.controller.AuthenticationController;
import it.trinex.blackout.controller.SignupController;
import it.trinex.blackout.exception.BlackoutExceptionHandler;
import it.trinex.blackout.properties.JwtProperties;
import it.trinex.blackout.repository.AuthAccountRepo;
import it.trinex.blackout.security.BlackoutPrincipalFactory;
import it.trinex.blackout.security.JwtAuthenticationFilter;
import it.trinex.blackout.service.AuthService;
import it.trinex.blackout.service.BlackoutUserDetailService;
import it.trinex.blackout.service.JwtService;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;

@AutoConfiguration
public class BlackoutAutoconfig {

    @Bean
    @ConditionalOnMissingBean(name = "authController")
    public AuthenticationController authenticationController(AuthService authService) {
        return new AuthenticationController(authService);
    }

    @Bean
    @ConditionalOnMissingBean(UserDetailsService.class)
    public BlackoutUserDetailService blackoutUserDetailService(AuthAccountRepo authAccountRepo) {
        return new BlackoutUserDetailService(authAccountRepo);
    }

    @Bean
    @ConditionalOnProperty(name = "blackout.signup.enabled", havingValue = "true", matchIfMissing = false)
    public SignupController signupController(AuthService authService, PasswordEncoder passwordEncoder) {
        return new SignupController(passwordEncoder, authService);
    }

    @Bean
    public BlackoutExceptionHandler blackoutExceptionHandler() {
        return new BlackoutExceptionHandler();
    }

    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter(JwtService jwtService) {
        return new JwtAuthenticationFilter(jwtService);
    }

    @Bean
    public JwtService jwtService(JwtProperties jwtProperties, BlackoutPrincipalFactory blackoutPrincipalFactory) {
        return new JwtService(jwtProperties, blackoutPrincipalFactory);
    }

    @Bean
    public AuthService authService(AuthenticationManager authenticationManager, JwtService jWTService, AuthAccountRepo authAccountRepo, JwtProperties jwtProperties) {
        return new AuthService(authenticationManager, jWTService, authAccountRepo, jwtProperties);
    }

}
