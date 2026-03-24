package it.trinex.blackout.autoconfig;

import it.trinex.blackout.properties.*;
import it.trinex.blackout.security.JwtAuthenticationFilter;
import it.trinex.blackout.security.WebAuthnJwtSuccessHandler;
import it.trinex.blackout.service.WebAuthnHelperService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer;
import org.springframework.security.config.annotation.web.configurers.HttpBasicConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import tools.jackson.databind.ObjectMapper;

@AutoConfiguration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
@EnableConfigurationProperties({CorsProperties.class, JwtProperties.class, FilterChainProperties.class, SignupProperties.class, BlackoutProperties.class, WebAuthProperties.class})
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final UserDetailsService userDetailsService;
    private final CorsProperties corsProperties;
    private final FilterChainProperties filterChainProperties;
    private final BlackoutProperties blackoutProperties;
    private final WebAuthProperties webAuthProperties;
    private final ObjectProvider<Customizer<AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry>> authorizeHttpRequestsCustomizer;
    private final SignupProperties signupProperties;
    private final ObjectProvider<WebAuthnHelperService> webAuthnHelperServiceProvider;

    @Bean
    @ConditionalOnMissingBean(SecurityFilterChain.class)
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .httpBasic(HttpBasicConfigurer::disable)
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, authException) ->
                                response.sendError(HttpServletResponse.SC_UNAUTHORIZED)
                        )
                )
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .authorizeHttpRequests(auth -> {
                    authorizeHttpRequestsCustomizer.ifAvailable(c -> c.customize(auth));
                    auth.requestMatchers("/error").permitAll();
                    auth.requestMatchers(blackoutProperties.getBaseUrl() + "/auth/**").permitAll();
                    auth.requestMatchers(blackoutProperties.getBaseUrl() + "/password/**").permitAll();
                    auth.requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/swagger-ui.html").permitAll();
                    auth.requestMatchers(blackoutProperties.getBaseUrl() + "/2fa/disable-recovery").permitAll();

                    if (webAuthProperties.isEnabled()) {
                        auth.requestMatchers("/webauthn/**").permitAll();
                        auth.requestMatchers("/login/webauthn").permitAll();
                    }

                    if(signupProperties.isEnabled()){
                        auth.requestMatchers(blackoutProperties.getBaseUrl() + "/signup").permitAll();
                    }

                    if (filterChainProperties.getAllowed() != null) {
                        filterChainProperties.getAllowed().forEach(
                            pattern -> auth.requestMatchers( pattern).permitAll()
                        );
                    }

                    auth.requestMatchers( "/**").authenticated();

                    if (filterChainProperties.getAuthenticated() != null) {
                        filterChainProperties.getAuthenticated().forEach(
                            pattern -> auth.requestMatchers(pattern).authenticated()
                        );
                    }
                })
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authenticationProvider(authenticationProvider());

                if(webAuthProperties.isEnabled()) {
                    http.webAuthn(webAuthn -> {
                        if (webAuthProperties.getRpId() != null) {
                            webAuthn.rpId(webAuthProperties.getRpId());
                        }
                        if (webAuthProperties.getAllowedOrigins() != null && !webAuthProperties.getAllowedOrigins().isEmpty()) {
                            webAuthn.allowedOrigins(webAuthProperties.getAllowedOrigins());
                        }
                        if (webAuthProperties.getRpName() != null) {
                            webAuthn.rpName(webAuthProperties.getRpName());
                        }
                    });
                }

                http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    @ConditionalOnMissingBean(AuthenticationProvider.class)
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    @ConditionalOnMissingBean(PasswordEncoder.class)
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    @ConditionalOnMissingBean(AuthenticationManager.class)
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    @ConditionalOnMissingBean(CorsConfigurationSource.class)
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        if (corsProperties.isAllowCredentials()) {
            if (corsProperties.getAllowedOrigins() != null &&
                    corsProperties.getAllowedOrigins().contains("*")) {
                throw new IllegalStateException(
                        "CORS configuration error: When allowCredentials is true, " +
                        "allowedOrigins cannot be '*'. Please specify explicit origins in blackout.cors.allowedOrigins");
            }
            configuration.setAllowCredentials(true);
        }

        configuration.setAllowedOrigins(corsProperties.getAllowedOrigins());
        configuration.setAllowedMethods(corsProperties.getAllowedMethods());
        configuration.setAllowedHeaders(corsProperties.getAllowedHeaders());
        configuration.setMaxAge(corsProperties.getMaxAge());

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);

        return source;
    }
}
