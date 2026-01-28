package it.trinex.nnh.config;

import it.trinex.nnh.security.jwt.JwtAuthenticationFilter;
import it.trinex.nnh.security.jwt.JwtService;
import it.trinex.nnh.security.jwt.JwtProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.userdetails.UserDetailsService;

/**
 * JWT-specific security configuration.
 *
 * <p>This configuration provides JWT-related beans.</p>
 */
@Configuration
@ConditionalOnClass(JwtService.class)
@ConditionalOnProperty(
        prefix = "nnh.security",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class JwtSecurityConfiguration {

    /**
     * Create JwtProperties bean if not already provided.
     *
     * @return the JwtProperties bean
     */
    @Bean
    @ConditionalOnMissingBean(JwtProperties.class)
    public JwtProperties jwtProperties() {
        return new JwtProperties();
    }

    /**
     * Create JwtService bean if not already provided.
     *
     * @param jwtProperties the JWT properties
     * @return the JwtService bean
     */
    @Bean
    @ConditionalOnMissingBean(JwtService.class)
    public JwtService jwtService(JwtProperties jwtProperties) {
        return new JwtService(jwtProperties);
    }

    /**
     * Create JwtAuthenticationFilter bean if not already provided.
     *
     * @param jwtService the JWT service
     * @param userDetailsService the user details service
     * @return the JwtAuthenticationFilter bean
     */
    @Bean
    @ConditionalOnMissingBean(JwtAuthenticationFilter.class)
    public JwtAuthenticationFilter jwtAuthenticationFilter(
            JwtService jwtService,
            UserDetailsService userDetailsService
    ) {
        return new JwtAuthenticationFilter(jwtService, userDetailsService);
    }
}
