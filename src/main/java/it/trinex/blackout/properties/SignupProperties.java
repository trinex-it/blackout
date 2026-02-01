package it.trinex.blackout.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for user signup functionality.
 * Allows customization of signup behavior through application.yml or application.properties.
 */
@Data
@ConfigurationProperties(prefix = "nnh.signup")
public class SignupProperties {

    /**
     * Whether user signup is enabled.
     * Default: true
     */
    private boolean enabled = true;

    /**
     * Default role assigned to users upon signup.
     * Default: "USER"
     */
    private String defaultRole = "USER";
}
