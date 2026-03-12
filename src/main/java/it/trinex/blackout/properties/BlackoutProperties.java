package it.trinex.blackout.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "blackout")
public class BlackoutProperties {

    private String baseUrl = "";

    /**
     * Whether authentication is handled via cookies.
     * When true, OpenAPI will not configure any security scheme (no "Authorize" button in Swagger UI).
     * Authentication is handled automatically by the browser via the access_token cookie.
     * Default: false (JWT Bearer authentication with Authorization header)
     */
    private boolean cookie = false;

}
