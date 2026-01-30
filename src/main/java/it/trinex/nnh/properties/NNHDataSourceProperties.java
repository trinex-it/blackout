package it.trinex.nnh.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "nnh.datasource")
public class NNHDataSourceProperties {

    private String url;

    private String username;

    private String password;

    private String driverClassName;
}