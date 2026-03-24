package it.trinex.blackout.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for email sending.
 * Allows customization of SMTP settings through application.yml or application.properties.
 */
@Data
@ConfigurationProperties(prefix = "blackout.mail")
public class MailProperties {

    /**
     * Whether email sending is enabled.
     * When disabled, no JavaMailSender bean will be created.
     * Default: false
     */
    private Boolean enabled = false;

    /**
     * SMTP server host.
     * Default: localhost
     */
    private String host = "localhost";

    /**
     * SMTP server port.
     * Default: 587 (submission port for STARTTLS)
     */
    private Integer port = 587;

    /**
     * Username for SMTP authentication.
     * Default: null (no authentication)
     */
    private String username;

    /**
     * Password for SMTP authentication.
     * Default: null
     */
    private String password;

    /**
     * Protocol to use for email sending.
     * Default: smtp
     */
    private String protocol = "smtp";

    /**
     * Default sender email address.
     * Default: noreply@localhost
     */
    private String from = "noreply@localhost";

    /**
     * Default sender name.
     * Default: Blackout
     */
    private String fromName = "Blackout";

    /**
     * Whether SMTP authentication is enabled.
     * Default: true
     */
    private Boolean auth = true;

    /**
     * Whether STARTTLS is enabled.
     * Default: true
     */
    private Boolean starttls = true;

    /**
     * Whether to enable debug mode for email sending.
     * When enabled, logs detailed SMTP communication.
     * Default: false
     */
    private Boolean debug = false;
}
