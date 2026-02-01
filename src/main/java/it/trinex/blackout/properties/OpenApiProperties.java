package it.trinex.blackout.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * Configuration properties for OpenAPI/Swagger documentation.
 * Allows customization of API documentation through application.yml or application.properties.
 */
@Data
@ConfigurationProperties(prefix = "blackout.openapi")
public class OpenApiProperties {

    /**
     * Whether OpenAPI documentation is enabled.
     * Default: true
     */
    private boolean enabled = true;

    /**
     * API title displayed in Swagger UI.
     * Default: "API Documentation"
     */
    private String title = "API Documentation";

    /**
     * API description/summary.
     * Default: "Spring Boot Application API"
     */
    private String description = "Spring Boot Application API";

    /**
     * API version.
     * Default: "1.0.0"
     */
    private String version = "1.0.0";

    /**
     * Group name for the OpenAPI documentation.
     * Default: "blackout-api"
     */
    private String group = "test-api";

    /**
     * Contact name for API support.
     */
    private String contactName;

    /**
     * Contact email for API support.
     */
    private String contactEmail;

    /**
     * Contact URL for API support.
     */
    private String contactUrl;

    /**
     * License name (e.g., "Apache 2.0", "MIT").
     */
    private String licenseName;

    /**
     * License URL.
     */
    private String licenseUrl;

    /**
     * Base package to scan for REST controllers.
     * If null, scans all packages.
     */
    private String basePackage;

    /**
     * Paths to include in documentation.
     * Default: all paths included
     */
    private List<String> pathsToMatch;

    /**
     * Paths to exclude from documentation.
     */
    private List<String> pathsToExclude;
}
