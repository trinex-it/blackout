package it.trinex.blackout.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.HashMap;
import java.util.Map;

@Data
@ConfigurationProperties(prefix = "blackout.datasource")
public class BlackoutDataSourceProperties {

    private String url;

    private String username;

    private String password;

    private String driverClassName;

    /**
     * Optional: allows explicit control over whether to fallback to spring.datasource
     * If true, when blackout datasource is not configured, will use the default datasource
     */
    private boolean useSpringDatasourceFallback = true;

    /**
     * JPA/Hibernate properties (like spring.jpa.properties.*)
     * Allows setting any Hibernate property, e.g.:
     * - hibernate.hbm2ddl.auto
     * - hibernate.dialect
     * - hibernate.format_sql
     * - hibernate.show_sql
     */
    private Map<String, String> jpaProperties = new HashMap<>();

    public BlackoutDataSourceProperties() {
        // Sensible defaults - users can override via YAML
        jpaProperties.put("hibernate.hbm2ddl.auto", "update");
        jpaProperties.put("hibernate.format_sql", "true");
    }

    /**
     * Checks if ALL datasource properties are properly configured.
     * A datasource is only considered configured if url, username, password,
     * and driverClassName are all non-null and non-empty.
     */
    public boolean isConfigured() {
        return url != null && !url.isEmpty()
                && username != null && !username.isEmpty()
                && password != null && !password.isEmpty()
                && driverClassName != null && !driverClassName.isEmpty();
    }
}
