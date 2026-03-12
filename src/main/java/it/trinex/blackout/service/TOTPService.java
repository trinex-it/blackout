package it.trinex.blackout.service;

import dev.samstevens.totp.code.CodeVerifier;
import dev.samstevens.totp.code.HashingAlgorithm;
import dev.samstevens.totp.exceptions.QrGenerationException;
import dev.samstevens.totp.qr.QrData;
import dev.samstevens.totp.qr.QrGenerator;
import static dev.samstevens.totp.util.Utils.getDataUriForImage;

import dev.samstevens.totp.recovery.RecoveryCodeGenerator;
import dev.samstevens.totp.secret.SecretGenerator;
import it.trinex.blackout.dto.request.Disable2FAWithRecoveryRequest;
import it.trinex.blackout.dto.response.TFAEnabledResponse;
import it.trinex.blackout.dto.response.TOTPRegistrationResponse;
import it.trinex.blackout.exception.*;
import it.trinex.blackout.model.AuthAccount;
import it.trinex.blackout.properties.TOTPProperties;
import it.trinex.blackout.repository.AuthAccountRepo;
import it.trinex.blackout.security.BlackoutUserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TOTPService {

    private final TOTPProperties properties;
    private final SecretGenerator secretGenerator;
    private final AuthAccountRepo authAccountRepo;
    private final QrGenerator qrGenerator;
    private final CodeVerifier codeVerifier;
    private final CurrentUserService currentUserService;
    private final RecoveryCodeGenerator recoveryCodeGenerator;
    private final AuthenticationManager authenticationManager;

    public TFAEnabledResponse enable2FA(String secret, String totp) throws QrGenerationException {
        if(codeVerifier.isValidCode(secret, totp)) {
            AuthAccount authAccount = currentUserService.getAuthAccount();
            if(authAccount.getTotpSecret() != null && !authAccount.getTotpSecret().isEmpty()) {
                throw new TFAAlreadyEnabledException("2FA already enabled");
            }
            List<String> recoveryCodes = List.of(recoveryCodeGenerator.generateCodes(8));
            authAccount.setRecoveryCodes(recoveryCodes);
            authAccount.setTotpSecret(secret);
            authAccountRepo.save(authAccount);
            return TFAEnabledResponse.builder()
                    .recoveryCodes(recoveryCodes)
                    .build();
        } else {
            throw new InvalidTOTPCodeException("2FA code is not valid");
        }
    }

    public void disable2FA(String totp) {
        AuthAccount authAccount = currentUserService.getAuthAccount();
        String secret = authAccount.getTotpSecret();
        if(secret == null || secret.isBlank()) {
            throw new TFANotEnabledException("2FA code is not enabled");
        }
        if(!codeVerifier.isValidCode(secret, totp)) {
            throw new InvalidTOTPCodeException("2FA code is not valid");
        }
        authAccount.setTotpSecret(null);
        authAccount.setRecoveryCodes(null);
        authAccountRepo.save(authAccount);
    }

    @Transactional("blackoutTransactionManager")
    public void disable2FAWithRecoveryCode(Disable2FAWithRecoveryRequest request) {
        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getSubject(),
                            request.getPassword()));
        } catch (AuthenticationException e) {
            throw new UnauthorizedException("Invalid username or password");
        }

        AuthAccount authAccount = authAccountRepo.findByUsername(request.getSubject()).orElse(
                authAccountRepo.findByEmail(request.getSubject()).get()
        );

        String secret = authAccount.getTotpSecret();
        if(secret == null || secret.isBlank()) {
            throw new TFANotEnabledException("2FA code is not enabled");
        }
        List<String> recoveryCodes = authAccount.getRecoveryCodes();
        if (!recoveryCodes.contains(request.getRecoveryCode())) {
            throw new InvalidRecoveryCodeException("Recovery code is not valid");
        }
        authAccount.setTotpSecret(null);
        authAccount.setRecoveryCodes(null);
        authAccountRepo.save(authAccount);
    }

    public TOTPRegistrationResponse generateTOTP() throws QrGenerationException {
        String secret = secretGenerator.generate();
        AuthAccount authAccount = currentUserService.getAuthAccount();
        if(authAccount.getTotpSecret() != null && !authAccount.getTotpSecret().isEmpty()) {
            throw new TFAAlreadyEnabledException("2FA already enabled");
        }
        QrData qrData = new QrData.Builder()
                .label(generateLabel(authAccount))
                .secret(secret)
                .issuer(properties.getAppName())
                .algorithm(HashingAlgorithm.SHA1)
                .digits(6)
                .period(30)
                .build();
        String qrURI = getDataUriForImage(qrGenerator.generate(qrData), "image/png");
        return new TOTPRegistrationResponse(secret, qrURI);
    }

    public boolean verifyCode(String code, String secret) {
        return codeVerifier.isValidCode(secret, code);
    }
    
    public String generateLabel(AuthAccount authAccount) {
        String email = authAccount.getEmail();
        String username = authAccount.getUsername();

        if (email != null && username != null) {
            return email + " - " + username;
        } else if (email != null) {
            return email;
        } else {
            return username;
        }
    }

}
