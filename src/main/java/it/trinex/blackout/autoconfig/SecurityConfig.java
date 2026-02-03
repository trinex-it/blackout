package it.trinex.blackout.autoconfig;

import it.trinex.blackout.properties.*;
import it.trinex.blackout.security.JwtAuthenticationFilter;
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
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer;
import org.springframework.security.config.Customizer;
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

@AutoConfiguration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
@EnableConfigurationProperties({CorsProperties.class, JwtProperties.class, FilterChainProperties.class, SignupProperties.class, BlackoutProperties.class})
@ConditionalOnBean({JwtAuthenticationFilter.class, UserDetailsService.class})
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final UserDetailsService userDetailsService;
    private final CorsProperties corsProperties;
    private final FilterChainProperties filterChainProperties;
    private final BlackoutProperties blackoutProperties;
    //Even if no bean is defined Spring should inject its own
    private final ObjectProvider<Customizer<AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry>> authorizeHttpRequestsCustomizer;
    private final SignupProperties signupProperties;

    /**
     * Configures the security filter chain with JWT authentication.
     */
    @Bean
    @ConditionalOnMissingBean(SecurityFilterChain.class)
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // Disable CSRF (not needed for JWT stateless authentication)
                .csrf(AbstractHttpConfigurer::disable)

                // Disable HTTP Basic (using JWT only)
                .httpBasic(HttpBasicConfigurer::disable)

                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                // Configure authorization rules
                .authorizeHttpRequests(auth -> {
                    authorizeHttpRequestsCustomizer.ifAvailable(c -> c.customize(auth));
                    // Error page - must be accessible to all
                    auth.requestMatchers("/error").permitAll();
                    // Authentication endpoints - no authentication required
                    auth.requestMatchers(blackoutProperties.getBaseUrl() + "/auth/**").permitAll();
                    // Swagger/OpenAPI endpoints - no authentication required (dev only)
                    auth.requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/swagger-ui.html").permitAll();

                    if(signupProperties.isEnabled()){
                        auth.requestMatchers(blackoutProperties.getBaseUrl() + "/signup").permitAll();
                    }

                    // Custom allowed endpoints from properties
                    if (filterChainProperties.getAllowed() != null) {
                        filterChainProperties.getAllowed().forEach(
                            pattern -> auth.requestMatchers(blackoutProperties.getBaseUrl() + pattern).permitAll()
                        );
                    }

                    // All other API endpoints require authentication
                    auth.requestMatchers(blackoutProperties.getBaseUrl() + "/**").authenticated();

                    // Custom authenticated endpoints from properties
                    if (filterChainProperties.getAuthenticated() != null) {
                        filterChainProperties.getAuthenticated().forEach(
                            pattern -> auth.requestMatchers(pattern).authenticated()
                        );
                    }
                })

                // Stateless session management (no server-side sessions)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // Set authentication provider
                .authenticationProvider(authenticationProvider())

                // Add JWT filter before Spring Security's authentication filter
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * Authentication provider using DaoAuthenticationProvider.
     * Loads users via CustomUserDetailsService and validates passwords with BCrypt.
     */
    @Bean
    @ConditionalOnMissingBean(AuthenticationProvider.class)
    public AuthenticationProvider authenticationProvider() {
        // todo: fix deprecation
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    /**
     * BCrypt password encoder for secure password hashing.
     * Uses default strength (10 rounds).
     */
    @Bean
    @ConditionalOnMissingBean(PasswordEncoder.class)
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Exposes Spring Security's AuthenticationManager as a bean.
     * Required for manual authentication in AuthenticationController.
     */
    @Bean
    @ConditionalOnMissingBean(AuthenticationManager.class)
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    @ConditionalOnMissingBean(CorsConfigurationSource.class)
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // When allowCredentials is true, cannot use wildcard for origins
        if (corsProperties.isAllowCredentials()) {
            // Validate that origins are not wildcards when credentials are enabled
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
