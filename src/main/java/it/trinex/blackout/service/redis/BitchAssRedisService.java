package it.trinex.blackout.service.redis;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.Set;


@Service
@Slf4j
public class BitchAssRedisService implements RedisService {

    @PostConstruct
    public void init() {
        log.info("Redis configuration not found. Initialized Bitch Ass Redis Service");
    }


    @Override
    public void revokeRefreshToken(String jti, Date expiresAt) {
        return;
    }

    @Override
    public void revokeAccessToken(String jti, Date expiresAt) {
        return;
    }

    @Override
    public boolean isRefreshTokenRevoked(String jti) {
        return false;
    }

    @Override
    public boolean isAccessTokenRevoked(String jti) {
        return false;
    }

    @Override
    public void trackUserToken(Long userId, String jti, Date expiresAt, String tokenType) {
        return;
    }

    @Override
    public void trackChallenge(String sessionId, String challenge) {

    }

    @Override
    public void removeUserToken(Long userId, String jti, String tokenType) {
        return;
    }

    @Override
    public Set<String> getUserTokens(Long userId) {
        return Set.of();
    }

    @Override
    public int revokeAllUserTokens(Long userId) {
        return 0;
    }
}
