package it.trinex.nnh.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Setter
@Getter
@Component
@ConfigurationProperties(prefix = "nnh.cors")
public class CorsProperties {

    // getter & setter
    private List<String> allowedOrigins = List.of("*");
    private List<String> allowedMethods = List.of("*");
    private List<String> allowedHeaders = List.of("*");
    private boolean allowCredentials = true;
    private long maxAge = 3600;

}