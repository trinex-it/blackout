package it.trinex.blackout.service;


import com.webauthn4j.WebAuthnManager;
import com.webauthn4j.converter.util.ObjectConverter;
import com.webauthn4j.data.*;
import com.webauthn4j.data.attestation.AttestationObject;
import com.webauthn4j.authenticator.AuthenticatorImpl;
import com.webauthn4j.data.attestation.authenticator.AAGUID;
import com.webauthn4j.data.attestation.authenticator.AttestedCredentialData;
import com.webauthn4j.data.attestation.authenticator.COSEKey;
import com.webauthn4j.data.client.Origin;
import com.webauthn4j.data.client.challenge.Challenge;
import com.webauthn4j.data.client.challenge.DefaultChallenge;
import com.webauthn4j.server.ServerProperty;
import com.webauthn4j.util.Base64UrlUtil;
import it.trinex.blackout.dto.request.AuthenticationFinishRequest;
import it.trinex.blackout.dto.request.RegistrationFinishRequest;
import it.trinex.blackout.dto.response.AuthResponseDTO;
import it.trinex.blackout.dto.response.AuthenticationStartResponse;
import it.trinex.blackout.dto.response.RegistrationStartResponse;
import it.trinex.blackout.exception.EarlyFinishException;
import it.trinex.blackout.exception.UnauthorizedException;
import it.trinex.blackout.model.AuthAccount;
import it.trinex.blackout.model.Passkey;
import it.trinex.blackout.properties.WebAuthnProperties;
import it.trinex.blackout.repository.PasskeyRepository;
import it.trinex.blackout.security.BlackoutUserPrincipal;
import it.trinex.blackout.service.redis.RedisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class PasskeyService {
    
    private final PasskeyRepository passkeyRepository;
    private final WebAuthnManager webAuthnManager = WebAuthnManager.createNonStrictWebAuthnManager();
    private final ObjectConverter objectConverter = new ObjectConverter();
    private final CurrentUserService<BlackoutUserPrincipal> currentUserService;

    private final WebAuthnProperties webAuthnProperties;

    private static final String PASSKEY_REAUTH_COOKIE_NAME = "passkey_reatuh_session";
    private static final Long PASSKEY_COOKIE_DURATION = 900L;
    
    // In-memory storage for challenges (in production, use Redis or database)
    private final Map<String, Challenge> challengeStore = new ConcurrentHashMap<>();
    private final UserDetailsService userDetailsService;
    private final JwtService jwtService;
    private final ObjectMapper objectMapper;
    private final CookieService cookieService;

    private String extractSubject(AuthAccount authAccount) {
        if(authAccount.getUsername() == null || authAccount.getUsername().isEmpty()) {
            return authAccount.getEmail();
        }
        return authAccount.getUsername();
    }
    
    /**
     * Start registration process - generate challenge and options
     */
    public RegistrationStartResponse startRegistration() {

        AuthAccount authAccount = currentUserService.getAuthAccount();

        log.info("Starting registration for user: {}", extractSubject(authAccount));
        
        // Generate challenge
        byte[] challengeBytes = generateRandomBytes(32);
        Challenge challenge = new DefaultChallenge(challengeBytes);
        String challengeBase64 = Base64UrlUtil.encodeToString(challengeBytes);
        
        // Store challenge temporarily
        challengeStore.put(String.valueOf(authAccount.getId()), challenge);
        
        // Generate temporary user handle for registration
        String userHandle = Base64UrlUtil.encodeToString(generateRandomBytes(32));
        
        // Build registration options
        return RegistrationStartResponse.builder()
                .challenge(challengeBase64)
                .rp(RegistrationStartResponse.RpInfo.builder()
                        .name(webAuthnProperties.getRpName())
                        .id(webAuthnProperties.getRpId())
                        .build())
                .user(RegistrationStartResponse.UserInfo.builder()
                        .id(userHandle)
                        .name(extractSubject(authAccount))
                        .displayName(authAccount.getFirstName()) //TODO: CI SERVE?
                        .build())
                .pubKeyCredParams(Arrays.asList(
                        RegistrationStartResponse.PublicKeyCredentialParameters.builder()
                                .type("public-key")
                                .alg(-7) // ES256
                                .build(),
                        RegistrationStartResponse.PublicKeyCredentialParameters.builder()
                                .type("public-key")
                                .alg(-257) // RS256
                                .build()
                ))
                .timeout(60000L)
                .authenticatorSelection(RegistrationStartResponse.AuthenticatorSelection.builder()
                        .authenticatorAttachment("platform")
                        .residentKey("preferred")
                        .requireResidentKey(false)
                        .userVerification("preferred")
                        .build())
                .attestation("none")
                .build();
    }
    
    /**
     * Finish registration - verify credential and store passkey
     */
    @Transactional
    public void finishRegistration(RegistrationFinishRequest request) {

        AuthAccount authAccount = currentUserService.getAuthAccount();

        log.info("Finishing registration for user: {}", extractSubject(authAccount));

        try {
            // Get stored challenge
            Challenge challenge = challengeStore.get(String.valueOf(authAccount.getId()));
            if (challenge == null) {
                throw new EarlyFinishException("Passkey creation not initialized. Use /passkey/register/start");
            }
            
            // Decode the attestation data
            byte[] clientDataJSON = Base64UrlUtil.decode(request.getResponse().getClientDataJSON());
            byte[] attestationObject = Base64UrlUtil.decode(request.getResponse().getAttestationObject());
            
            // Create server property
            ServerProperty serverProperty = new ServerProperty(
                    new Origin(webAuthnProperties.getOrigin()),
                    webAuthnProperties.getRpId(),
                    challenge,
                    null
            );
            
            // Validate the registration
            RegistrationRequest registrationRequest = new RegistrationRequest(attestationObject, clientDataJSON);
            RegistrationParameters registrationParameters = new RegistrationParameters(
                    serverProperty,
                    null,
                    false,
                    true
            );
            
            RegistrationData response = webAuthnManager.parse(registrationRequest);
            webAuthnManager.validate(response, registrationParameters);
            
            // Extract credential data
            AttestationObject attObj = response.getAttestationObject();
            AttestedCredentialData credentialData = attObj.getAuthenticatorData().getAttestedCredentialData();
            
            if (credentialData == null) {
                throw new IllegalArgumentException("No credential data found");
            }
            
            byte[] credentialId = credentialData.getCredentialId();
            COSEKey coseKey = credentialData.getCOSEKey();

            // Create passkey
            Passkey passkey = new Passkey();
            passkey.setCredentialId(Base64UrlUtil.encodeToString(credentialId));
            
            // Serialize COSEKey to bytes
            byte[] coseKeyBytes = objectConverter.getCborConverter().writeValueAsBytes(coseKey);
            passkey.setPublicKey(Base64.getEncoder().encodeToString(coseKeyBytes));
            
            passkey.setSignCount(attObj.getAuthenticatorData().getSignCount());
            passkey.setAaguid(credentialData.getAaguid().toString());
            passkey.setAuthAccount(authAccount);
            passkey.setDeviceName(request.getDeviceName() != null ? request.getDeviceName() : "Unknown Device");
            passkey.setEnabled(true);
            
            // Store transports if available
            if (request.getResponse().getTransports() != null) {
                passkey.setTransports(String.join(",", request.getResponse().getTransports()));
            }
            
            passkeyRepository.save(passkey);
            
            // Clean up challenge
            challengeStore.remove(String.valueOf(authAccount.getId()));
            
            log.info("Successfully registered passkey for user: {}", extractSubject(authAccount));
            
        } catch (Exception e) {
            log.error("Error during passkey registration", e);
            throw new RuntimeException("Failed to register passkey: " + e.getMessage());
        }
    }
    
    /**
     * Start authentication - generate challenge
     */
    public AuthenticationStartResponse startAuthentication() {
        // Generate challenge
        byte[] challengeBytes = generateRandomBytes(32);
        Challenge challenge = new DefaultChallenge(challengeBytes);
        String challengeBase64 = Base64UrlUtil.encodeToString(challengeBytes);
        // Store challenge with session-specific key
        String key = "auth_" + UUID.randomUUID();

        challengeStore.put(key, challenge);
        
        log.info("Stored challenge with key: {}", key);
        log.debug("Challenge store size: {}", challengeStore.size());
        

        return AuthenticationStartResponse.builder()
                .sessionId(key)
                .challenge(challengeBase64)
                .timeout(60000L)
                .rpId(webAuthnProperties.getRpId())
                .allowCredentials(List.of())
                .userVerification("preferred")
                .build();
    }
    
    /**
     * Finish authentication - verify signature and authenticate user
     */
    @Transactional
    public AuthResponseDTO finishAuthentication(AuthenticationFinishRequest request, String sessionId) {
        try {

            Challenge challenge = challengeStore.get(sessionId);
            if (challenge == null) {
                throw new EarlyFinishException("Passkey creation not initialized. Use /passkey/register/start");
            }

            // Find passkey by credential ID
            String credentialId = request.getId();
            log.info("Attempting to authenticate with credential ID: {}", credentialId);
            
            Optional<Passkey> passkeyOpt = passkeyRepository.findByCredentialId(credentialId);
            
            if (passkeyOpt.isEmpty()) {
                log.error("Passkey not found for credential ID: {}", credentialId);
                throw new IllegalArgumentException("Passkey not found");
            }
            
            Passkey passkey = passkeyOpt.get();
            AuthAccount authAccount = passkey.getAuthAccount();
            log.info("Found passkey for user: {}", extractSubject(authAccount));

            // Decode authentication data
            byte[] credentialIdBytes = Base64UrlUtil.decode(credentialId);
            byte[] clientDataJSON = Base64UrlUtil.decode(request.getResponse().getClientDataJSON());
            byte[] authenticatorData = Base64UrlUtil.decode(request.getResponse().getAuthenticatorData());
            byte[] signature = Base64UrlUtil.decode(request.getResponse().getSignature());
            
            // Parse and validate authentication (WebAuthn4J will handle basic verification)
            AuthenticationRequest authRequest = new AuthenticationRequest(
                    credentialIdBytes,
                    authenticatorData,
                    clientDataJSON,
                    signature
            );
            
            // Parse the authentication data
            AuthenticationData authData = webAuthnManager.parse(authRequest);

            // Validate the response matches our stored credential
            if (!Arrays.equals(authData.getCredentialId(), credentialIdBytes)) {
                throw new IllegalArgumentException("Credential ID mismatch");
            }

            // Decode COSEKey from database
            byte[] coseKeyBytes = Base64.getDecoder().decode(passkey.getPublicKey());
            COSEKey coseKey = objectConverter.getCborConverter().readValue(coseKeyBytes, COSEKey.class);

            // Reconstruct AttestedCredentialData
            byte[] credentialIdBytes2 = Base64UrlUtil.decode(passkey.getCredentialId());
            UUID tempAaguid = UUID.fromString(passkey.getAaguid());

            AAGUID aaguid = new AAGUID(tempAaguid);

            AttestedCredentialData credentialData = new AttestedCredentialData(
                    aaguid,
                    credentialIdBytes2,
                    coseKey
            );

            // Create Authenticator
            AuthenticatorImpl authenticator = new AuthenticatorImpl(
                    credentialData,
                    null,  // attestationStatement - not needed for authentication validation
                    passkey.getSignCount()
            );

            // Create ServerProperty with the challenge
            ServerProperty serverProperty = new ServerProperty(
                    new Origin(webAuthnProperties.getOrigin()),
                    webAuthnProperties.getRpId(),
                    challenge,
                    null
            );

            // Create authentication parameters for validation
            AuthenticationParameters authenticationParameters = new AuthenticationParameters(
                    serverProperty,
                    authenticator,
                    null,  // allowCredentials
                    false,  // userVerificationRequired (preferred, not required)
                    true    // userPresenceRequired
            );

            // Validate the authentication response against the challenge
            webAuthnManager.validate(authData, authenticationParameters);

            // Update passkey usage
            passkey.setSignCount(authData.getAuthenticatorData().getSignCount());
            passkey.setLastUsedAt(LocalDateTime.now());
            passkeyRepository.save(passkey);
            
            log.info("Successfully authenticated user: {}", extractSubject(authAccount));

            // Extract authenticated user principal
            BlackoutUserPrincipal userPrincipal = (BlackoutUserPrincipal) userDetailsService.loadUserByUsername(extractSubject(authAccount));

            // Generate tokens
            String accessToken = jwtService.generateAccessToken(userPrincipal);
            String refreshToken = jwtService.generateRefreshToken(userPrincipal);

            // Calculate expiration time for client
            long accessTokenExpirationMs = jwtService.calculateAccessTokenExpiration().toEpochMilli()
                    - System.currentTimeMillis();
            long refreshTokenExpirationMs = jwtService.calculateRefreshTokenExpiration().toEpochMilli()
                    - System.currentTimeMillis();

            log.info("User '{}' logged in successfully", extractSubject(authAccount));

            // Determine if we should set the refresh token
            long refreshTokenMaxAge = Duration.ofMillis(refreshTokenExpirationMs).toSeconds();

            String userJson = objectMapper.writeValueAsString(jwtService.extractAllClaims(accessToken));

            return AuthResponseDTO.builder()
                    .needOTP(false)
                    .access_token(accessToken)
                    .refresh_token(refreshToken)
                    .access_token_expiration(accessTokenExpirationMs)
                    .refresh_token_expiration(refreshTokenMaxAge)
                    .userJson(userJson)
                    .build();
            
        } catch (Exception e) {
            log.error("Error during passkey authentication", e);
            throw new RuntimeException("Failed to authenticate with passkey: " + e.getMessage());
        }
    }

    /**
     * Start authentication - generate challenge
     */
    public AuthenticationStartResponse startReauthentication() {
        // Generate challenge
        byte[] challengeBytes = generateRandomBytes(32);
        Challenge challenge = new DefaultChallenge(challengeBytes);
        String challengeBase64 = Base64UrlUtil.encodeToString(challengeBytes);
        // Store challenge with session-specific key
        String key = "auth_" + UUID.randomUUID();

        challengeStore.put(key, challenge);

        log.info("Stored challenge with key: {}", key);
        log.debug("Challenge store size: {}", challengeStore.size());

        AuthAccount authAccount = currentUserService.getAuthAccount();

        List<AuthenticationStartResponse.AllowCredential> allowedCredentials =
                passkeyRepository.findByAuthAccount(authAccount)
                        .stream()
                        .map(pk -> AuthenticationStartResponse.AllowCredential.builder()
                                .id(pk.getCredentialId())
                                .type("public-key")
                                .transports(pk.getTransports() != null ?
                                        Arrays.asList(pk.getTransports().split(",")) : null)
                                .build())
                        .toList();

        return AuthenticationStartResponse.builder()
                .sessionId(key)
                .challenge(challengeBase64)
                .timeout(60000L)
                .rpId(webAuthnProperties.getRpId())
                .allowCredentials(allowedCredentials)
                .userVerification("preferred")
                .build();
    }

    /**
     * Finish authentication - verify signature and authenticate user
     */
    @Transactional
    public ResponseCookie finishReauthentication(AuthenticationFinishRequest request, String sessionId) {
        try {

            Challenge challenge = challengeStore.get(sessionId);
            if (challenge == null) {
                throw new EarlyFinishException("Passkey creation not initialized. Use /passkey/register/start");
            }

            // Find passkey by credential ID
            String credentialId = request.getId();
            log.info("Attempting to authenticate with credential ID: {}", credentialId);

            Optional<Passkey> passkeyOpt = passkeyRepository.findByCredentialId(credentialId);

            if (passkeyOpt.isEmpty()) {
                log.error("Passkey not found for credential ID: {}", credentialId);
                throw new IllegalArgumentException("Passkey not found");
            }

            Passkey passkey = passkeyOpt.get();
            AuthAccount authAccount = passkey.getAuthAccount();
            AuthAccount currentAuthAccount = currentUserService.getAuthAccount();

            if(!authAccount.getId().equals(currentAuthAccount.getId())) {
                throw new UnauthorizedException("Current user does not correspond to the passkey used.");
            }

            log.info("Found passkey for user: {}", extractSubject(authAccount));

            // Decode authentication data
            byte[] credentialIdBytes = Base64UrlUtil.decode(credentialId);
            byte[] clientDataJSON = Base64UrlUtil.decode(request.getResponse().getClientDataJSON());
            byte[] authenticatorData = Base64UrlUtil.decode(request.getResponse().getAuthenticatorData());
            byte[] signature = Base64UrlUtil.decode(request.getResponse().getSignature());

            // Parse and validate authentication (WebAuthn4J will handle basic verification)
            AuthenticationRequest authRequest = new AuthenticationRequest(
                    credentialIdBytes,
                    authenticatorData,
                    clientDataJSON,
                    signature
            );

            // Parse the authentication data
            AuthenticationData authData = webAuthnManager.parse(authRequest);

            // Validate the response matches our stored credential
            if (!Arrays.equals(authData.getCredentialId(), credentialIdBytes)) {
                throw new IllegalArgumentException("Credential ID mismatch");
            }

            // Decode COSEKey from database
            byte[] coseKeyBytes = Base64.getDecoder().decode(passkey.getPublicKey());
            COSEKey coseKey = objectConverter.getCborConverter().readValue(coseKeyBytes, COSEKey.class);

            // Reconstruct AttestedCredentialData
            byte[] credentialIdBytes2 = Base64UrlUtil.decode(passkey.getCredentialId());
            UUID tempAaguid = UUID.fromString(passkey.getAaguid());

            AAGUID aaguid = new AAGUID(tempAaguid);

            AttestedCredentialData credentialData = new AttestedCredentialData(
                    aaguid,
                    credentialIdBytes2,
                    coseKey
            );

            // Create Authenticator
            AuthenticatorImpl authenticator = new AuthenticatorImpl(
                    credentialData,
                    null,  // attestationStatement - not needed for authentication validation
                    passkey.getSignCount()
            );

            // Create ServerProperty with the challenge
            ServerProperty serverProperty = new ServerProperty(
                    new Origin(webAuthnProperties.getOrigin()),
                    webAuthnProperties.getRpId(),
                    challenge,
                    null
            );

            // Create authentication parameters for validation
            AuthenticationParameters authenticationParameters = new AuthenticationParameters(
                    serverProperty,
                    authenticator,
                    null,  // allowCredentials
                    false,  // userVerificationRequired (preferred, not required)
                    true    // userPresenceRequired
            );

            // Validate the authentication response against the challenge
            webAuthnManager.validate(authData, authenticationParameters);

            // Update passkey usage
            passkey.setSignCount(authData.getAuthenticatorData().getSignCount());
            passkey.setLastUsedAt(LocalDateTime.now());
            passkeyRepository.save(passkey);

            BlackoutUserPrincipal principal = currentUserService.getCurrentPrincipal();

            String jwt = jwtService.generatePasskeyToken(principal);
            String jti = jwtService.extractJti(jwt); //todo: fallo

            log.info("Successfully reauthenticated user: {}", extractSubject(authAccount));

            return cookieService.generateGenericCookie(PASSKEY_REAUTH_COOKIE_NAME, jwt, PASSKEY_COOKIE_DURATION);
        } catch (Exception e) {
            log.error("Error during passkey authentication", e);
            throw new RuntimeException("Failed to authenticate with passkey: " + e.getMessage());
        }
    }
    
    public List<Passkey> getUserPasskeys(AuthAccount authAccount) {
        return passkeyRepository.findByAuthAccount(authAccount);
    }

    
    private byte[] generateRandomBytes(int length) {
        byte[] bytes = new byte[length];
        new Random().nextBytes(bytes);
        return bytes;
    }
}
