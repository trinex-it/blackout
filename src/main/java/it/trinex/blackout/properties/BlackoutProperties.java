package it.trinex.blackout.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "blackout")
public class BlackoutProperties {

    private String baseUrl = "/api";

}
