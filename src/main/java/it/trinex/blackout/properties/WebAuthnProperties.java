package it.trinex.blackout.properties;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for cookie-based authentication.
 * Allows customization of cookie behavior through application.yml or application.properties.
 */
@Data
@ConfigurationProperties(prefix = "blackout.webauthn")
public class WebAuthnProperties {

    private boolean enabled = false;

    private String rpId;

    private String rpName;

    private String origin = "localhost";
}
