package it.trinex.blackout.autoconfig;

import it.trinex.blackout.properties.BlackoutDataSourceProperties;
import it.trinex.blackout.properties.ParentDatasourceProperties;
import jakarta.persistence.EntityManagerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.JpaVendorAdapter;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;

/**
 * Datasource configuration for the Blackout library itself.
 * This configuration always creates the library's datasource context, with the following behavior:
 * - If blackout.datasource.url is configured, uses that configuration
 * - Otherwise, falls back to the default Spring datasource (shared with parent application)
 * - All beans are conditional on missing, allowing full user override
 */
@AutoConfiguration
@EnableJpaRepositories(
        basePackages = "it.trinex.blackout",
        entityManagerFactoryRef = "blackoutEntityManager",
        transactionManagerRef = "blackoutTransactionManager"
)
@EnableConfigurationProperties({
        BlackoutDataSourceProperties.class,
        ParentDatasourceProperties.class
})
public class BlackoutLibraryDatasourceConfig {

    private static final Logger log = LoggerFactory.getLogger(BlackoutLibraryDatasourceConfig.class);

    /**
     * Creates the datasource for the Blackout library.
     *
     * Priority order:
     * 1. If blackout.datasource.url is configured -> creates dedicated datasource
     * 2. If a dataSource bean exists in context -> returns it (shared mode)
     * 3. Otherwise throws IllegalStateException (no datasource available)
     *
     * @param properties Blackout datasource properties
     * @param existingDataSourceProvider ObjectProvider for default datasource (optional, avoids circular dependency)
     * @return the datasource to use for the Blackout library
     */
    @Bean
    @ConditionalOnMissingBean(name = "blackoutDataSource")
    public DataSource blackoutDataSource(
            BlackoutDataSourceProperties properties,
            ObjectProvider<DataSource> existingDataSourceProvider
    ) {
        if (properties.isConfigured()) {
            log.info("Blackout library using dedicated datasource: {}", properties.getUrl());
            return createBlackoutDataSource(properties);
        }

        DataSource existingDataSource = existingDataSourceProvider.getIfAvailable();
        if (existingDataSource != null) {
            log.info("Blackout library using shared Spring datasource");
            return existingDataSource;
        }

        throw new IllegalStateException(
                "No datasource configured for Blackout library. " +
                "Configure either 'blackout.datasource.url' or 'spring.datasource.url'."
        );
    }

    /**
     * Creates a dedicated Blackout datasource from properties.
     */
    private DataSource createBlackoutDataSource(BlackoutDataSourceProperties properties) {
        return DataSourceBuilder.create()
                .url(properties.getUrl())
                .username(properties.getUsername())
                .password(properties.getPassword())
                .driverClassName(properties.getDriverClassName())
                .build();
    }

    /**
     * Creates the EntityManagerFactory for the Blackout library.
     * Scans entities from the it.trinex.blackout.model package.
     */
    @Bean
    @ConditionalOnMissingBean(name = "blackoutEntityManager")
    public LocalContainerEntityManagerFactoryBean blackoutEntityManager(
            @Qualifier("blackoutDataSource") DataSource ds,
            BlackoutDataSourceProperties properties
    ) {
        LocalContainerEntityManagerFactoryBean emf = new LocalContainerEntityManagerFactoryBean();
        emf.setDataSource(ds);
        emf.setPackagesToScan("it.trinex.blackout.model");
        emf.setPersistenceUnitName("blackoutPU");

        JpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
        emf.setJpaVendorAdapter(vendorAdapter);

        // Apply JPA properties from configuration (Hibernate auto-detects dialect)
        emf.getJpaPropertyMap().putAll(properties.getJpaProperties());

        log.info("Blackout library EntityManager configured (dialect auto-detected by Hibernate)");

        return emf;
    }

    /**
     * Creates the transaction manager for the Blackout library.
     */
    @Bean
    @ConditionalOnMissingBean(name = "blackoutTransactionManager")
    public PlatformTransactionManager blackoutTransactionManager(
            @Qualifier("blackoutEntityManager") EntityManagerFactory emf
    ) {
        return new JpaTransactionManager(emf);
    }
}
