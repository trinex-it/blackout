package it.trinex.blackout.service;

import it.trinex.blackout.dto.request.ResetPasswordOTPRequest;
import it.trinex.blackout.dto.request.ResetPasswordRequest;
import it.trinex.blackout.exception.InvalidResetOTPException;
import it.trinex.blackout.exception.PasskeyRequiredException;
import it.trinex.blackout.exception.PasswordMismatchException;
import it.trinex.blackout.model.AuthAccount;
import it.trinex.blackout.model.Passkey;
import it.trinex.blackout.repository.AuthAccountRepo;
import it.trinex.blackout.security.BlackoutUserPrincipal;
import it.trinex.blackout.service.redis.RedisService;
import jakarta.mail.MessagingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.context.Context;

import java.io.UnsupportedEncodingException;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
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
    private final MailService mailService;


    public void sendResetPasswordEmail(String subject) throws MessagingException, UnsupportedEncodingException {
        if (mailService == null) {
            throw new IllegalStateException("Email service is not enabled. Please set blackout.mail.enabled=true to use this feature.");
        }

        Optional<AuthAccount> optionalAccount = authAccountRepo.findByUsername(subject)
            .or(() -> authAccountRepo.findByEmail(subject));

        if (optionalAccount.isEmpty()) {
            return;
        }

        AuthAccount authAccount = optionalAccount.get();
        String otp = generateResetOTP(subject);
        Context context = new Context();
        context.setVariable("otp", otp);
        context.setVariable("firstName", authAccount.getFirstName());
        mailService.sendMail(authAccount.getEmail(), context, "Password reset");
    }

    @Transactional
    public void enablePasswordlessLogin() {
        AuthAccount authAccount = currentUserService.getAuthAccount();

        List<Passkey> passkeys = authAccount.getPasskeys();

        if (passkeys.isEmpty()) {
            throw new PasskeyRequiredException("You need to have at least one passkey to enable passwordless login.");
        }

        authAccount.setPasswordless(true);
        authAccountRepo.save(authAccount);
    }

    private String generateResetOTP(String subject) {

        String lastOTP = redisTemplate.opsForValue().get(PASSWORD_OTP_KEY_PREFIX + subject);

        // invalido otp attivo se presente
        if (lastOTP != null && !lastOTP.isBlank()) {
            redisTemplate.delete(PASSWORD_OTP_KEY_PREFIX + subject);
        }

        try {
            String completeKey = PASSWORD_OTP_KEY_PREFIX + subject;
            int ttlSeconds = 300;
            // OTP GENERATION
            String resetOTP = String.format("%06d", java.util.concurrent.ThreadLocalRandom.current().nextInt(1_000_000));

            redisTemplate.opsForValue().set(completeKey, resetOTP, Duration.ofSeconds(ttlSeconds));
            log.debug("Generated reset OTP for {} (TTL: {}s)", subject, ttlSeconds);

            return resetOTP;
        } catch (Exception e) {
            log.warn("Failed to set reset OTP in Redis (graceful degradation): {}", e.getMessage());
            return null;
        }
    }

    public boolean checkResetOTP(String resetKey, String userOTP) {
        try {
            String key = PASSWORD_OTP_KEY_PREFIX + resetKey;
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

    private void removeResetOTP(String resetKey) {
        redisTemplate.delete(PASSWORD_OTP_KEY_PREFIX + resetKey);
    }

    public void resetPasswordWithOTP(ResetPasswordOTPRequest request, String subject) {

        AuthAccount authAccount = authAccountRepo.findByUsername(subject).orElse(
                authAccountRepo.findByEmail(subject).orElseThrow(
                        () -> new UsernameNotFoundException("Username not found: " + subject)
                )
        );

        String hashedPassword = passwordEncoder.encode(request.getNewPassword());

        if(!checkResetOTP(subject, request.getOtp())) {
            throw new InvalidResetOTPException("Invalid Reset OTP");
        }

        authAccount.setPasswordHash(hashedPassword);
        authAccount.setPasswordless(false);
        authAccountRepo.save(authAccount);
        redisService.revokeAllUserTokens(authAccount.getId());
        removeResetOTP(subject);
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
