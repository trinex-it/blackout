package it.trinex.blackout.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;


@Getter
@Component
@ConfigurationProperties(prefix = "blackout")
public class BlackoutProperties {

    private final String baseUrl = "/api";

}
