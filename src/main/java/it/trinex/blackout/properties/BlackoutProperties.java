package it.trinex.blackout.properties;

import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;


@Getter
@Component
@ConfigurationProperties(prefix = "blackout")
public class BlackoutProperties {

    private final String baseUrl = "/api";

}
