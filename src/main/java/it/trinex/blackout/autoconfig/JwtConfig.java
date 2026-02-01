package it.trinex.blackout.autoconfig;

import it.trinex.blackout.AbstractNNHPrincipalFactory;
import it.trinex.blackout.NNHPrincipalFactory;
import it.trinex.blackout.model.NNHUserPrincipal;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.security.core.userdetails.UserDetails;

@AutoConfiguration
public class JwtConfig {
    @Bean
    @ConditionalOnMissingBean(NNHPrincipalFactory.class)
    public NNHPrincipalFactory<UserDetails> defaultPrincipalFactory() {
        return new AbstractNNHPrincipalFactory<UserDetails>() {
            @Override
            protected NNHUserPrincipal.NNHUserPrincipalBuilder<?, ?> getBuilder() {
                return NNHUserPrincipal.builder();
            }
        };
    }
}
