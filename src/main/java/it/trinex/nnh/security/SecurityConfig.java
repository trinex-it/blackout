package it.trinex.nnh.security;

import it.trinex.nnh.config.SecurityProperties;
import it.trinex.nnh.security.jwt.JwtAuthenticationFilter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Core Spring Security configuration.
 *
 * <p>This configuration:</p>
 * <ul>
 *   <li>Disables CSRF (stateless JWT doesn't need it)</li>
 *   <li>Configures CORS</li>
 *   <li>Sets up public and protected endpoints</li>
 *   <li>Configures stateless session management</li>
 *   <li>Adds JWT authentication filter (if available)</li>
 * </ul>
 *
 * <p>All beans use {@code @ConditionalOnMissingBean} to allow users to override them.</p>
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@ConditionalOnProperty(
        prefix = "nnh.security",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class SecurityConfig {

    private final ObjectProvider<JwtAuthenticationFilter> jwtAuthenticationFilterProvider;
    private final ObjectProvider<UserDetailsService> userDetailsServiceProvider;
    private final ObjectProvider<SecurityProperties> securityPropertiesProvider;

    public SecurityConfig(
            ObjectProvider<JwtAuthenticationFilter> jwtAuthenticationFilterProvider,
            ObjectProvider<UserDetailsService> userDetailsServiceProvider,
            ObjectProvider<SecurityProperties> securityPropertiesProvider
    ) {
        this.jwtAuthenticationFilterProvider = jwtAuthenticationFilterProvider;
        this.userDetailsServiceProvider = userDetailsServiceProvider;
        this.securityPropertiesProvider = securityPropertiesProvider;
    }

    /**
     * Configure the security filter chain.
     *
     * @param http the HttpSecurity to configure
     * @return the configured SecurityFilterChain
     * @throws Exception if configuration fails
     */
    @Bean
    @ConditionalOnMissingBean(SecurityFilterChain.class)
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/public/**").permitAll()
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/swagger-ui.html").permitAll()
                        .requestMatchers("/actuator/**").permitAll()
                        .anyRequest().authenticated()
                )
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .authenticationProvider(authenticationProvider());

        // Add JWT filter if available
        jwtAuthenticationFilterProvider.ifAvailable(filter -> {
            http.addFilterBefore(filter, UsernamePasswordAuthenticationFilter.class);
        });

        return http.build();
    }

    /**
     * Configure the authentication provider.
     *
     * @return the configured AuthenticationProvider
     */
    @Bean
    @ConditionalOnMissingBean(AuthenticationProvider.class)
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        userDetailsServiceProvider.ifAvailable(authProvider::setUserDetailsService);
        return authProvider;
    }

    /**
     * Configure the password encoder.
     *
     * @return the BCrypt password encoder
     */
    @Bean
    @ConditionalOnMissingBean(PasswordEncoder.class)
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Configure the authentication manager.
     *
     * @param config the authentication configuration
     * @return the AuthenticationManager
     * @throws Exception if configuration fails
     */
    @Bean
    @ConditionalOnMissingBean(AuthenticationManager.class)
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    /**
     * Configure CORS.
     *
     * @return the CorsConfigurationSource
     */
    @Bean
    @ConditionalOnMissingBean(name = "corsConfigurationSource")
    public org.springframework.web.cors.CorsConfigurationSource corsConfigurationSource() {
        org.springframework.web.cors.CorsConfiguration configuration = new org.springframework.web.cors.CorsConfiguration();

        // Use defaults if securityProperties is not available
        if (securityPropertiesProvider.getIfAvailable() != null) {
            SecurityProperties props = securityPropertiesProvider.getIfAvailable();
            configuration.setAllowedOrigins(props.getCors().getAllowedOrigins());
            configuration.setAllowedMethods(props.getCors().getAllowedMethods());
            configuration.setAllowedHeaders(props.getCors().getAllowedHeaders());
            configuration.setAllowCredentials(props.getCors().getAllowCredentials());
            configuration.setMaxAge(props.getCors().getMaxAge());
        } else {
            // Default CORS configuration
            configuration.setAllowedOrigins(java.util.List.of("http://localhost:4200", "http://localhost:5173"));
            configuration.setAllowedMethods(java.util.List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
            configuration.setAllowedHeaders(java.util.List.of("*"));
            configuration.setAllowCredentials(true);
            configuration.setMaxAge(3600L);
        }

        org.springframework.web.cors.UrlBasedCorsConfigurationSource source =
                new org.springframework.web.cors.UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", configuration);
        return source;
    }
}
