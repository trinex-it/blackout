package it.trinex.nnh.config;

import it.trinex.nnh.security.jwt.JwtService;
import it.trinex.nnh.security.jwt.JwtUserPrincipal;
import it.trinex.nnh.util.CookieUtils;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

/**
 * Main auto-configuration entry point for NNH Security.
 *
 * <p>This class is automatically loaded by Spring Boot when it appears in the classpath.
 * It configures all the necessary beans for JWT-based authentication.</p>
 *
 * <p>To disable the entire auto-configuration, set:</p>
 * <pre>
 * nnh:
 *   security:
 *     enabled: false
 * </pre>
 */
@AutoConfiguration
@EnableConfigurationProperties(SecurityProperties.class)
@ConditionalOnClass({JwtService.class, JwtUserPrincipal.class})
@ConditionalOnProperty(
        prefix = "nnh.security",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true
)
@Import({
        JwtSecurityConfiguration.class,
        ConditionalSecurityConfig.class
})
public class SecurityAutoConfiguration {

    /**
     * Create CookieUtils bean if not already provided.
     *
     * @param securityProperties the security properties
     * @return the CookieUtils bean
     */
    @Bean
    @ConditionalOnMissingBean
    public CookieUtils cookieUtils(SecurityProperties securityProperties) {
        return new CookieUtils(securityProperties);
    }
}
