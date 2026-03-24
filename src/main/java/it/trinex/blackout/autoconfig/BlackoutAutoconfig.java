package it.trinex.blackout.autoconfig;

import dev.samstevens.totp.code.CodeVerifier;
import dev.samstevens.totp.qr.QrGenerator;
import dev.samstevens.totp.recovery.RecoveryCodeGenerator;
import dev.samstevens.totp.secret.SecretGenerator;
import it.trinex.blackout.controller.*;
import it.trinex.blackout.exception.BlackoutExceptionHandler;
import it.trinex.blackout.properties.*;
import it.trinex.blackout.repository.AuthAccountRepo;
import it.trinex.blackout.security.BlackoutPrincipalFactory;
import it.trinex.blackout.security.BlackoutUserPrincipal;
import it.trinex.blackout.security.JwtAuthenticationFilter;
import it.trinex.blackout.service.*;
import it.trinex.blackout.service.redis.RedisService;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.spring6.SpringTemplateEngine;

@AutoConfiguration
@EnableConfigurationProperties({CookieProperties.class})
public class BlackoutAutoconfig {

    @Bean
    @ConditionalOnMissingBean(name = "authController")
    @ConditionalOnProperty(prefix = "blackout.cookie", name = "enabled", havingValue = "false",  matchIfMissing = true)
    public BodyAuthController bodyAuthController(AuthService authService) {
        return new BodyAuthController(authService);
    }

    @Bean
    @ConditionalOnMissingBean(name = "authController")
    @ConditionalOnProperty(prefix = "blackout.cookie", name = "enabled", havingValue = "true")
    public CookieAuthController cookieAuthController(AuthService authService, JwtService jwtService, CookieService cookieService) {
        return new CookieAuthController(authService, jwtService, cookieService);
    }

    @Bean
    public PasswordController passwordController(PasswordService passwordService) {
        return new PasswordController(passwordService);
    }

    @Bean
    @ConditionalOnProperty(prefix = "blackout.mail", name = "enabled", havingValue = "true")
    public PasswordOtpController passwordOtpController(PasswordService passwordService) {
        return new PasswordOtpController(passwordService);
    }

    @Bean
    public PasswordService passwordService(AuthAccountRepo authAccountRepo, PasswordEncoder passwordEncoder, RedisTemplate<String, String> redisTemplate, CurrentUserService currentUserService, RedisService redisService, ObjectProvider<MailService> mailService) {
        return new PasswordService(authAccountRepo, passwordEncoder, redisTemplate, currentUserService, redisService, mailService.getIfAvailable());
    }

    @Bean
    @ConditionalOnProperty(prefix = "blackout.mail", name = "enabled", havingValue = "true")
    public MailService mailService(JavaMailSender javaMailSender, SpringTemplateEngine templateEngine, MailProperties mailProperties) {
        return new MailService(javaMailSender, templateEngine, mailProperties);
    }

    @Bean
    @ConditionalOnProperty(prefix = "blackout.cookie", name = "enabled", havingValue = "true")
    public CookieService cookieService(JwtProperties jwtProperties){
        return new CookieService(jwtProperties);
    }

    @Bean
    @ConditionalOnMissingBean(name = "totpController")
    public TOTPController totpController(TOTPService totpService) {
        return new TOTPController(totpService);
    }

    @Bean
    @ConditionalOnMissingBean(CurrentUserService.class)
    public CurrentUserService<BlackoutUserPrincipal> currentUserService(AuthAccountRepo authAccountRepo) {
        return new CurrentUserService<>(authAccountRepo);
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
    @ConditionalOnMissingBean(JwtAuthenticationFilter.class)
    public JwtAuthenticationFilter jwtAuthFilter(
            JwtService jwtService,
            ObjectProvider<AuthService> authService,
            ObjectProvider<CookieService> cookieService,
            CookieProperties cookieProperties) {
        return new JwtAuthenticationFilter(
                jwtService,
                authService.getIfAvailable(),
                cookieService.getIfAvailable(),
                cookieProperties.isAutoRefresh());
    }

    @Bean
    public TOTPService totpService(TOTPProperties tOTPProperties, SecretGenerator secretGenerator, AuthAccountRepo authAccountRepo, QrGenerator qrGenerator, CodeVerifier codeVerifier, CurrentUserService currentUserService, RecoveryCodeGenerator recoveryCodeGenerator, AuthenticationManager authenticationManager) {
        return new TOTPService(tOTPProperties, secretGenerator, authAccountRepo, qrGenerator, codeVerifier, currentUserService, recoveryCodeGenerator, authenticationManager);
    }

    @Bean
    public JwtService jwtService(JwtProperties jwtProperties, BlackoutPrincipalFactory blackoutPrincipalFactory, RedisService redisService) {
        return new JwtService(jwtProperties, blackoutPrincipalFactory, redisService);
    }

    @Bean
    public AuthService authService(@Lazy AuthenticationManager authenticationManager, JwtService jWTService, AuthAccountRepo authAccountRepo, JwtProperties jwtProperties, UserDetailsService userDetailsService, @Lazy TOTPService totpService, CurrentUserService currentUserService, RedisService redisService) {
        return new AuthService(authenticationManager, jWTService, authAccountRepo, jwtProperties, userDetailsService, totpService, currentUserService, redisService);
    }

}
