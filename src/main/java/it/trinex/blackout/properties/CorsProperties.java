package it.trinex.blackout.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Setter
@Getter
@Component
@ConfigurationProperties(prefix = "blackout.cors")
public class CorsProperties {

    // Note: When allowCredentials is true, you cannot use "*" for allowedOrigins
    // For development with curl/browser, you can set allowCredentials=false in application.yml
    private List<String> allowedOrigins = new ArrayList<>(List.of("*"));
    private List<String> allowedMethods = new ArrayList<>(List.of("*"));
    private List<String> allowedHeaders = new ArrayList<>(List.of("*"));
    private boolean allowCredentials = false;  // Changed to false for development
    private long maxAge = 3600;

}
