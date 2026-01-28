package it.trinex.nnh.config;

import it.trinex.nnh.service.CurrentUserService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Conditional security configuration based on properties.
 *
 * <p>This configuration provides conditional beans based on application properties.</p>
 */
@Configuration
@ConditionalOnProperty(
        prefix = "nnh.security",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class ConditionalSecurityConfig {

    /**
     * Create CurrentUserService bean.
     *
     * @return the CurrentUserService
     */
    @Bean
    @ConditionalOnProperty(
            prefix = "nnh.security",
            name = "enabled",
            havingValue = "true",
            matchIfMissing = true
    )
    public CurrentUserService currentUserService() {
        return new CurrentUserService();
    }
}
