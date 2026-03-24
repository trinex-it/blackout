package it.trinex.blackout.repository.webauthn;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.web.webauthn.api.PublicKeyCredentialCreationOptions;
import org.springframework.security.web.webauthn.registration.PublicKeyCredentialCreationOptionsRepository;

import java.time.Duration;
import java.util.Arrays;
import java.util.UUID;

@RequiredArgsConstructor
public class RedisPublicKeyCredentialCreationOptionsRepository implements PublicKeyCredentialCreationOptionsRepository {

    private static final String COOKIE_NAME = "webauthn-create-session";
    private static final String REDIS_PREFIX = "webauthn:create:";
    
    private final RedisTemplate<String, Object> redis;

    @Override
    public void save(HttpServletRequest request, HttpServletResponse response, PublicKeyCredentialCreationOptions options) {
        if (options == null) {
            deleteCookie(request, response);
            return;
        }

        String sessionId = UUID.randomUUID().toString();
        redis.opsForValue().set(REDIS_PREFIX + sessionId, options, Duration.ofMinutes(5));

        Cookie cookie = new Cookie(COOKIE_NAME, sessionId);
        cookie.setHttpOnly(true);
        cookie.setSecure(request.isSecure());
        cookie.setPath("/");
        cookie.setMaxAge(300); // 5 minutes
        response.addCookie(cookie);
    }

    @Override
    public PublicKeyCredentialCreationOptions load(HttpServletRequest request) {
        String sessionId = getSessionIdFromCookie(request);
        if (sessionId == null) {
            return null;
        }
        return (PublicKeyCredentialCreationOptions) redis.opsForValue().get(REDIS_PREFIX + sessionId);
    }
    
    private void deleteCookie(HttpServletRequest request, HttpServletResponse response) {
        String sessionId = getSessionIdFromCookie(request);
        if (sessionId != null) {
            redis.delete(REDIS_PREFIX + sessionId);
        }
        
        Cookie cookie = new Cookie(COOKIE_NAME, "");
        cookie.setHttpOnly(true);
        cookie.setSecure(request.isSecure());
        cookie.setPath("/");
        cookie.setMaxAge(0);
        response.addCookie(cookie);
    }

    private String getSessionIdFromCookie(HttpServletRequest request) {
        if (request.getCookies() == null) {
            return null;
        }
        return Arrays.stream(request.getCookies())
                .filter(c -> COOKIE_NAME.equals(c.getName()))
                .map(Cookie::getValue)
                .findFirst()
                .orElse(null);
    }
}
