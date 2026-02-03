package it.trinex.blackout.autoconfig;

import it.trinex.blackout.properties.BlackoutDataSourceProperties;
import it.trinex.blackout.properties.ParentDatasourceProperties;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * Orchestration configuration for dual-datasource setup.
 *
 * This class imports and coordinates:
 * - BlackoutLibraryDatasourceConfig: datasource for the Blackout library itself
 * - ParentApplicationDatasourceConfig: datasource for the parent application (optional)
 *
 * Behavior:
 * - Library datasource is always created (either dedicated or shared)
 * - Parent datasource is created only when repository/model properties are configured
 * - All beans are @ConditionalOnMissingBean, allowing full user override
 *
 * @see BlackoutLibraryDatasourceConfig
 * @see ParentApplicationDatasourceConfig
 */
@AutoConfiguration
@ImportAutoConfiguration({
        BlackoutLibraryDatasourceConfig.class,
        ParentApplicationDatasourceConfig.class
})
@EnableConfigurationProperties({
        BlackoutDataSourceProperties.class,
        ParentDatasourceProperties.class
})
public class BlackoutDataSourceConfig {
    // Orchestration only - all logic moved to specialized configs
}
