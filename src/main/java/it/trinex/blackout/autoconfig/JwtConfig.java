package it.trinex.blackout.autoconfig;

import it.trinex.blackout.AbstractBlackoutPrincipalFactory;
import it.trinex.blackout.BlackoutPrincipalFactory;
import it.trinex.blackout.model.BlackoutUserPrincipal;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.security.core.userdetails.UserDetails;

@AutoConfiguration
public class JwtConfig {
    @Bean
    @ConditionalOnMissingBean(BlackoutPrincipalFactory.class)
    public BlackoutPrincipalFactory<UserDetails> defaultPrincipalFactory() {
        return new AbstractBlackoutPrincipalFactory<UserDetails>() {
            @Override
            protected BlackoutUserPrincipal.BlackoutUserPrincipalBuilder<?, ?> getBuilder() {
                return BlackoutUserPrincipal.builder();
            }
        };
    }
}
