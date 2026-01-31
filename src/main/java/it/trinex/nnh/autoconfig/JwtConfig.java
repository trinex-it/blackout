package it.trinex.nnh.autoconfig;

import it.trinex.nnh.AbstractNNHPrincipalFactory;
import it.trinex.nnh.NNHPrincipalFactory;
import it.trinex.nnh.model.NNHUserPrincipal;
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
