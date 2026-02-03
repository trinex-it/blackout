package it.trinex.blackout.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.HashMap;
import java.util.Map;

@Data
@ConfigurationProperties(prefix = "blackout.parent.datasource")
public class ParentDatasourceProperties {
    /**
     * Required: parent application repository package
     */
    private String repository;

    /**
     * Required: parent application entity package
     */
    private String model;

    /**
     * Whether parent datasource configuration is enabled
     */
    private boolean enabled = true;

    /**
     * JPA/Hibernate properties for parent application.
     * Uses same pattern as spring.jpa.properties.*
     */
    private Map<String, String> jpaProperties = new HashMap<>();

    public ParentDatasourceProperties() {
        // Sensible defaults
        jpaProperties.put("hibernate.hbm2ddl.auto", "update");
        jpaProperties.put("hibernate.format_sql", "true");
    }

    /**
     * Checks if the parent datasource is properly configured
     */
    public boolean isConfigured() {
        return repository != null && !repository.isEmpty()
                && model != null && !model.isEmpty();
    }
}
