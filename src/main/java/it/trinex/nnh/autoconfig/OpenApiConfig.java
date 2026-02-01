package it.trinex.nnh.autoconfig;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import it.trinex.nnh.properties.OpenApiProperties;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * Auto-configuration for OpenAPI/Swagger documentation.
 *
 * This configuration provides automatic OpenAPI documentation setup with sensible defaults.
 * Can be customized via application properties (nnh.openapi.*) or by overriding beans.
 *
 * To disable OpenAPI documentation, set:
 * nnh.openapi.enabled=false
 *
 * To customize, provide your own OpenAPI or GroupedOpenApi bean in your application.
 */
@AutoConfiguration
@ConditionalOnWebApplication
@ConditionalOnClass(GroupedOpenApi.class)
@ConditionalOnProperty(prefix = "nnh.openapi", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(OpenApiProperties.class)
@RequiredArgsConstructor
public class OpenApiConfig {

    private final OpenApiProperties openApiProperties;

    /**
     * Configures the main OpenAPI bean with API metadata and JWT security scheme.
     * Can be overridden by providing a custom OpenAPI bean in the application.
     *
     * @return OpenAPI configuration with title, description, version, contact, license info, and JWT security
     */
    @Bean
    @ConditionalOnMissingBean
    public OpenAPI customOpenAPI() {
        OpenAPI openApi = new OpenAPI();
        Info info = new Info();

        info.setTitle(openApiProperties.getTitle());
        info.setDescription(openApiProperties.getDescription());
        info.setVersion(openApiProperties.getVersion());

        // Add contact information if provided
        if (openApiProperties.getContactName() != null ||
            openApiProperties.getContactEmail() != null ||
            openApiProperties.getContactUrl() != null) {

            Contact contact = new Contact();
            contact.setName(openApiProperties.getContactName());
            contact.setEmail(openApiProperties.getContactEmail());
            contact.setUrl(openApiProperties.getContactUrl());
            info.setContact(contact);
        }

        // Add license information if provided
        if (openApiProperties.getLicenseName() != null || openApiProperties.getLicenseUrl() != null) {
            License license = new License();
            license.setName(openApiProperties.getLicenseName());
            license.setUrl(openApiProperties.getLicenseUrl());
            info.setLicense(license);
        }

        openApi.setInfo(info);

        // Configure JWT Bearer authentication security scheme
        SecurityScheme securityScheme = new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")
                .description("JWT authentication token. Use the format: Bearer <your-jwt-token>");

        SecurityRequirement securityRequirement = new SecurityRequirement()
                .addList("bearerAuth");

        openApi.components(new Components().addSecuritySchemes("bearerAuth", securityScheme));
        openApi.addSecurityItem(securityRequirement);

        return openApi;
    }

    /**
     * Configures GroupedOpenApi for controller scanning.
     * Can be overridden by providing a custom GroupedOpenApi bean in the application.
     *
     * @return GroupedOpenApi configuration with package and path filters
     */
    @Bean
    @ConditionalOnMissingBean
    public GroupedOpenApi groupedOpenAPI() {
        GroupedOpenApi.Builder builder = GroupedOpenApi.builder()
                .group(openApiProperties.getGroup());

        // Set package to scan if configured
        if (openApiProperties.getBasePackage() != null) {
            builder.packagesToScan(openApiProperties.getBasePackage());
        }

        // Set paths to match if configured
        if (openApiProperties.getPathsToMatch() != null && !openApiProperties.getPathsToMatch().isEmpty()) {
            builder.pathsToMatch(openApiProperties.getPathsToMatch().toArray(new String[0]));
        }

        return builder.build();
    }
}
