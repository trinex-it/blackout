package it.trinex.blackout.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Collections;
import java.util.Set;

/**
 * Configuration properties for WebAuthn (Passkeys) authentication.
 */
@Data
@ConfigurationProperties(prefix = "blackout.webauthn")
public class WebAuthProperties {

    /**
     * Whether WebAuthn authentication is enabled.
     * Default: false
     */
    private boolean enabled = false;

    /**
     * The Relying Party ID. This is typically the domain name of the website.
     * For example: "example.com"
     */
    private String rpId;

    /**
     * The Relying Party Name. This is a human-readable name for the application.
     * For example: "Blackout Auth System"
     */
    private String rpName = "Blackout Auth System";

    /**
     * The list of allowed origins.
     * For example: ["https://example.com"]
     */
    private Set<String> allowedOrigins = Collections.emptySet();

    /**
     * The timeout for the WebAuthn ceremony in milliseconds.
     * Default: 60000 (1 minute)
     */
    private long timeout = 60000;

    /**
     * The user verification requirement.
     * Options: "required", "preferred", "discouraged"
     * Default: "preferred"
     */
    private String userVerification = "preferred";

    /**
     * The attestation conveyance preference.
     * Options: "none", "indirect", "direct", "enterprise"
     * Default: "none"
     */
    private String attestation = "none";
}
