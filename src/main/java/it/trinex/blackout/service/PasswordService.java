package it.trinex.blackout.service;

import it.trinex.blackout.dto.request.ResetPasswordOTPRequest;
import it.trinex.blackout.dto.request.ResetPasswordRequest;
import it.trinex.blackout.exception.InvalidResetOTPException;
import it.trinex.blackout.exception.PasswordMismatchException;
import it.trinex.blackout.model.AuthAccount;
import it.trinex.blackout.repository.AuthAccountRepo;
import it.trinex.blackout.security.BlackoutUserPrincipal;
import it.trinex.blackout.service.redis.RedisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PasswordService {

    private final AuthAccountRepo authAccountRepo;
    private final PasswordEncoder passwordEncoder;
    private final RedisTemplate<String, String> redisTemplate;

    private static final String PASSWORD_OTP_KEY_PREFIX = "resetotp:";
    private final CurrentUserService<BlackoutUserPrincipal> currentUserService;
    private final RedisService redisService;


    public String generateResetOTP(String subject) {
        try {
            String key = PASSWORD_OTP_KEY_PREFIX + subject;
            int ttlSeconds = 300;
            // OTP GENERATION
            String resetOTP = String.format("%06d", java.util.concurrent.ThreadLocalRandom.current().nextInt(1_000_000));

            redisTemplate.opsForValue().set(key, resetOTP, Duration.ofSeconds(ttlSeconds));
            log.debug("Generated reset OTP for {} (TTL: {}s)", subject, ttlSeconds);

            return resetOTP;
        } catch (Exception e) {
            log.warn("Failed to set reset OTP in Redis (graceful degradation): {}", e.getMessage());
            return null;
        }
    }

    public boolean checkResetOTP(String subject, String userOTP) {
        try {
            String key = PASSWORD_OTP_KEY_PREFIX + subject;
            String realOTP = redisTemplate.opsForValue().get(key);
            if (realOTP != null && realOTP.equals(userOTP)) {
                return true;
            }
            return false;
        } catch (Exception e) {
            log.warn("Failed to get reset OTP in Redis (graceful degradation): {}", e.getMessage());
            throw e;
        }
    }

    public void resetPasswordWithOTP(ResetPasswordOTPRequest request) {

        AuthAccount authAccount = authAccountRepo.findByUsername(request.getSubject()).orElse(
                authAccountRepo.findByEmail(request.getSubject()).orElseThrow(
                        () -> new UsernameNotFoundException("Username not found: " + request.getSubject())
                )
        );

        String hashedPassword = passwordEncoder.encode(request.getNewPassword());

        if(checkResetOTP(request.getSubject(), request.getOtp())) {
            authAccount.setPasswordHash(hashedPassword);
            authAccountRepo.save(authAccount);
            return;
        }

        throw new InvalidResetOTPException("Invalid Reset OTP");
    }

    public void resetPasswordWithoutOTP(ResetPasswordRequest request) {

        BlackoutUserPrincipal currentUser = currentUserService.getCurrentPrincipal();

        AuthAccount authAccount = authAccountRepo.findByUsername(currentUser.getUsername()).orElse(
                authAccountRepo.findByEmail(currentUser.getEmail()).orElseThrow(
                        () -> new UsernameNotFoundException("Username not found: " + currentUser.getUsername())
                )
        );

        if(!passwordEncoder.matches(request.getOldPassword(), authAccount.getPasswordHash())) {
            throw new PasswordMismatchException("Old password does not match.");
        }

        String newHashedPassword = passwordEncoder.encode(request.getNewPassword());

        authAccount.setPasswordHash(newHashedPassword);
        authAccountRepo.save(authAccount);
        redisService.revokeAllUserTokens(authAccount.getId());
    }
}
