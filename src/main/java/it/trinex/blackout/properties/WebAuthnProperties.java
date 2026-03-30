package it.trinex.blackout.properties;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

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

    private List<String> origins = new ArrayList<>();

    private Long reauthenticationTimeout = 900L;
}
