package it.trinex.blackout.service;

import dev.samstevens.totp.code.CodeVerifier;
import dev.samstevens.totp.code.HashingAlgorithm;
import dev.samstevens.totp.exceptions.QrGenerationException;
import dev.samstevens.totp.qr.QrData;
import dev.samstevens.totp.qr.QrGenerator;
import static dev.samstevens.totp.util.Utils.getDataUriForImage;
import dev.samstevens.totp.secret.SecretGenerator;
import it.trinex.blackout.dto.response.TOTPRegistrationResponse;
import it.trinex.blackout.model.AuthAccount;
import it.trinex.blackout.properties.TOTPProperties;
import it.trinex.blackout.repository.AuthAccountRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TOTPService {

    private final TOTPProperties properties;
    private final SecretGenerator secretGenerator;
    private final AuthAccountRepo authAccountRepo;
    private final QrGenerator qrGenerator;
    private final CodeVerifier codeVerifier;

    public TOTPRegistrationResponse registerAndSave(AuthAccount authAccount) throws QrGenerationException {
        String secret = secretGenerator.generate();
        authAccount.setTotpSecret(secret);
        authAccountRepo.save(authAccount);
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
        return codeVerifier.isValidCode(code, secret);
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
