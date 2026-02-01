package it.trinex.blackout.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * Configuration properties for security filter chain endpoint rules.
 * Allows configuring permitAll and authenticated endpoints via application properties.
 */
@Data
@ConfigurationProperties(prefix = "blackout.filterchain")
public class FilterChainProperties {

    /**
     * List of endpoint patterns that should be accessible without authentication (permitAll).
     * Supports Ant-style patterns (e.g., /api/**, /api/public/**).
     */
    private List<String> allowed = new ArrayList<>();

    /**
     * List of endpoint patterns that require authentication.
     * Supports Ant-style patterns (e.g., /api/admin/**, /api/secure/**).
     */
    private List<String> authenticated = new ArrayList<>();
}
