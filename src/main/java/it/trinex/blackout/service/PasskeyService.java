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
import it.trinex.blackout.dto.request.PasswordReauthenticationRequest;
import it.trinex.blackout.dto.request.RegistrationFinishRequest;
import it.trinex.blackout.dto.request.RegistrationStartRequest;
import it.trinex.blackout.dto.response.*;
import it.trinex.blackout.exception.*;
import it.trinex.blackout.model.AuthAccount;
import it.trinex.blackout.model.Passkey;
import it.trinex.blackout.properties.WebAuthnProperties;
import it.trinex.blackout.repository.PasskeyRepository;
import it.trinex.blackout.security.BlackoutUserPrincipal;
import it.trinex.blackout.service.redis.RedisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
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

    private static final String PASSKEY_REAUTH_COOKIE_NAME = "reauth_token";

    // In-memory storage for challenges (in production, use Redis or database)
    private final Map<String, Challenge> challengeStore = new ConcurrentHashMap<>();
    private final UserDetailsService userDetailsService;
    private final JwtService jwtService;
    private final ObjectMapper objectMapper;
    private final CookieService cookieService;
    private final PasswordEncoder passwordEncoder;
    private final RedisService redisService;

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
                        .displayName(extractSubject(authAccount)) //TODO: CI SERVE?
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
    public RegistrationFinishResponse finishRegistration(RegistrationFinishRequest request) {

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

            // Extract and validate origin from clientDataJSON
            Origin origin = extractAndValidateOrigin(clientDataJSON);

            // Create server property
            ServerProperty serverProperty = new ServerProperty(
                    origin,
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

            String accessToken = jwtService.generateAccessToken(currentUserService.getCurrentPrincipal());
            redisService.revokeAllUserTokens(authAccount.getId());

            ResponseCookie accessTokenCookie = cookieService.generateAccessCookie(accessToken);

            log.info("Successfully registered passkey for user: {}", extractSubject(authAccount));

            return RegistrationFinishResponse.builder()
                            .accessToken(accessToken)
                            .accessTokenCookie(accessTokenCookie)
                            .build();

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

            // Extract and validate origin from clientDataJSON
            Origin origin = extractAndValidateOrigin(clientDataJSON);

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
                    origin,
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

            challengeStore.remove(sessionId);

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
    public ReauthenticationFinishResponse finishReauthentication(AuthenticationFinishRequest request, String sessionId) {
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

            // Extract and validate origin from clientDataJSON
            Origin origin = extractAndValidateOrigin(clientDataJSON);

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
                    origin,
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

            log.info("Successfully reauthenticated user: {}", extractSubject(authAccount));

            return ReauthenticationFinishResponse.builder()
                    .reauthToken(jwt)
                    .tokenCookie(cookieService.generateGenericCookie(PASSKEY_REAUTH_COOKIE_NAME, jwt, webAuthnProperties.getReauthenticationTimeout()))
                    .build();
        } catch (Exception e) {
            log.error("Error during passkey authentication", e);
            throw new RuntimeException("Failed to authenticate with passkey: " + e.getMessage());
        }
    }

    public ReauthenticationFinishResponse passwordReauthentication(PasswordReauthenticationRequest request) {

        AuthAccount authAccount = currentUserService.getAuthAccount();

        if(authAccount.isPasswordless()) {
            throw new PasswordlessEnabledException("User is passwordless, cannot reauthenticate with password");
        }

        BlackoutUserPrincipal principal = currentUserService.getCurrentPrincipal();

        if(!passwordEncoder.matches(request.getPassword(),  authAccount.getPasswordHash())) {
            throw new PasswordMismatchException("Password is not valid");
        }

        String jwt = jwtService.generatePasskeyToken(principal);

        log.info("Successfully reauthenticated user: {}", extractSubject(authAccount));

        return ReauthenticationFinishResponse.builder()
                .reauthToken(jwt)
                .tokenCookie(cookieService.generateGenericCookie(PASSKEY_REAUTH_COOKIE_NAME, jwt, webAuthnProperties.getReauthenticationTimeout()))
                .build();
    }
    
    public List<Passkey> getUserPasskeys(AuthAccount authAccount) {
        return passkeyRepository.findByAuthAccount(authAccount);
    }

    /**
     * Extracts and validates the origin from clientDataJSON.
     * The origin must be one of the allowed origins configured in webAuthnProperties.origins
     *
     * @param clientDataJSON The clientDataJSON bytes from WebAuthn response
     * @return The validated Origin object
     * @throws IOException if clientDataJSON cannot be parsed
     * @throws SecurityException if the origin is not in the allowed list
     */
    private Origin extractAndValidateOrigin(byte[] clientDataJSON) throws IOException {
        // Parse clientDataJSON to extract origin
        Map<String, Object> clientData = objectMapper.readValue(
                new String(clientDataJSON, StandardCharsets.UTF_8),
                Map.class
        );

        String origin = (String) clientData.get("origin");

        if (origin == null) {
            throw new IllegalArgumentException("Origin not found in clientDataJSON");
        }

        // Check if allowed origins list is configured
        if (webAuthnProperties.getOrigins() == null || webAuthnProperties.getOrigins().isEmpty()) {
            throw new IllegalStateException("No allowed origins configured in blackout.webauthn.origins");
        }

        // Validate against allowed origins
        if (!webAuthnProperties.getOrigins().contains(origin)) {
            log.error("Origin '{}' is not allowed. Allowed origins: {}", origin, webAuthnProperties.getOrigins());
            throw new SecurityException("Origin '" + origin + "' is not allowed");
        }

        log.debug("Origin '{}' validated successfully", origin);
        return new Origin(origin);
    }

    public List<Passkey> getAllPasskeys() {
        return passkeyRepository.findByAuthAccount(currentUserService.getAuthAccount());
    }

    public void deletePasskey(Long id) {
        try {
            passkeyRepository.deleteById(id);
        } catch (EmptyResultDataAccessException e) {
            throw new PasskeyNotFoundException("Passkey not found with id: " + id);
        }
    }


    private byte[] generateRandomBytes(int length) {
        byte[] bytes = new byte[length];
        new Random().nextBytes(bytes);
        return bytes;
    }
}
