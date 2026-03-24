package it.trinex.blackout.autoconfig;

import it.trinex.blackout.properties.WebAuthProperties;
import it.trinex.blackout.repository.ChallengeRepo;
import it.trinex.blackout.repository.webauthn.JpaPublicKeyCredentialUserEntityRepository;
import it.trinex.blackout.repository.webauthn.JpaUserCredentialRepository;
import it.trinex.blackout.repository.webauthn.RedisPublicKeyCredentialCreationOptionsRepository;
import it.trinex.blackout.repository.webauthn.RedisPublicKeyCredentialRequestOptionsRepository;
import it.trinex.blackout.repository.webauthn.WebAuthnCredentialRecordJpaRepo;
import it.trinex.blackout.repository.webauthn.WebAuthnUserEntityJpaRepo;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.web.webauthn.authentication.PublicKeyCredentialRequestOptionsRepository;
import org.springframework.security.web.webauthn.management.PublicKeyCredentialUserEntityRepository;
import org.springframework.security.web.webauthn.management.UserCredentialRepository;
import org.springframework.security.web.webauthn.registration.PublicKeyCredentialCreationOptionsRepository;

@AutoConfiguration
@EnableConfigurationProperties(WebAuthProperties.class)
@ConditionalOnProperty(prefix = "blackout.webauthn", name = "enabled", havingValue = "true")
public class WebAuthnConfig {

    @Bean
    @ConditionalOnMissingBean
    public PublicKeyCredentialUserEntityRepository publicKeyCredentialUserEntityRepository(WebAuthnUserEntityJpaRepo repo) {
        return new JpaPublicKeyCredentialUserEntityRepository(repo);
    }

    @Bean
    @ConditionalOnMissingBean
    public UserCredentialRepository userCredentialRepository(WebAuthnCredentialRecordJpaRepo repo) {
        return new JpaUserCredentialRepository(repo);
    }

    @Bean
    @ConditionalOnMissingBean
    public PublicKeyCredentialCreationOptionsRepository creationOptionsRepository(RedisTemplate<String, Object> redisGenericTemplate) {
        return new RedisPublicKeyCredentialCreationOptionsRepository(redisGenericTemplate);
    }

    @Bean
    @ConditionalOnMissingBean
    public PublicKeyCredentialRequestOptionsRepository requestOptionsRepository(RedisTemplate<String, Object> redisGenericTemplate) {
        return new RedisPublicKeyCredentialRequestOptionsRepository(redisGenericTemplate);
    }

    @Bean
    public ChallengeRepo challengeRepository(RedisTemplate<String, Object> redisGenericTemplate) {
        return new ChallengeRepo(redisGenericTemplate);
    }
}
