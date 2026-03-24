package it.trinex.blackout.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.web.webauthn.api.PublicKeyCredentialCreationOptions;
import org.springframework.stereotype.Repository;

import java.time.Duration;

@Repository
@RequiredArgsConstructor
public class ChallengeRepo {

    private final RedisTemplate<String, Object> redis;

    public void save(PublicKeyCredentialCreationOptions options) {
        String key = buildKey(options.getUser().getName());
        redis.opsForValue().set(
                key,
                options,
                Duration.ofMinutes(5)
        );
    }

    public PublicKeyCredentialCreationOptions load(String subject) {
        return (PublicKeyCredentialCreationOptions) redis.opsForValue().get(buildKey(subject));
    }

    public void remove(String subject) {
        redis.delete(buildKey(subject));
    }

    private String buildKey(String username) {
        return "webauthn:challenge:" + username;
    }
}
