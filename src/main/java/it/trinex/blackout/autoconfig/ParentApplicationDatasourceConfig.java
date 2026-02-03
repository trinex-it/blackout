package it.trinex.blackout.autoconfig;

import it.trinex.blackout.properties.ParentDatasourceProperties;
import jakarta.persistence.EntityManagerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Primary;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.boot.jdbc.autoconfigure.DataSourceProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.JpaVendorAdapter;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;

/**
 * Datasource configuration for the parent application.
 * This configuration only activates when both repository and model packages are specified
 * via blackout.parent.datasource.repository and blackout.parent.datasource.model properties.
 *
 * The parent datasource always uses spring.datasource configuration properties.
 * This allows complete isolation between the library and parent application datasources.
 */
@AutoConfiguration
@EnableJpaRepositories(
        basePackages = "${blackout.parent.datasource.repository}",
        entityManagerFactoryRef = "entityManagerFactory",
        transactionManagerRef = "parentTransactionManager"
)
@ConditionalOnProperty(prefix = "blackout.parent.datasource", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(ParentDatasourceProperties.class)
public class ParentApplicationDatasourceConfig {

    private static final Logger log = LoggerFactory.getLogger(ParentApplicationDatasourceConfig.class);

    /**
     * Creates the datasource for the parent application.
     * Uses spring.datasource.* configuration properties.
     */
    @Bean
    @ConditionalOnMissingBean(name = "parentDataSource")
    @ConditionalOnProperty(prefix = "blackout.parent.datasource", name = {"repository", "model"})
    public DataSource parentDataSource(DataSourceProperties dataSourceProperties) {
        DataSource dataSource = DataSourceBuilder.create()
                .url(dataSourceProperties.getUrl())
                .username(dataSourceProperties.getUsername())
                .password(dataSourceProperties.getPassword())
                .driverClassName(dataSourceProperties.getDriverClassName())
                .build();

        log.info("Parent application datasource configured: {}", dataSourceProperties.getUrl());

        return dataSource;
    }

    /**
     * Creates the EntityManagerFactory for the parent application.
     * Scans entities from the package specified in blackout.parent.datasource.model.
     */
    @Bean(name = "entityManagerFactory")
    @ConditionalOnMissingBean(name = "entityManagerFactory")
    @ConditionalOnProperty(prefix = "blackout.parent.datasource", name = {"repository", "model"})
    public LocalContainerEntityManagerFactoryBean entityManagerFactory(
            @Qualifier("parentDataSource") DataSource ds,
            ParentDatasourceProperties properties
    ) {
        LocalContainerEntityManagerFactoryBean emf = new LocalContainerEntityManagerFactoryBean();
        emf.setDataSource(ds);
        emf.setPackagesToScan(properties.getModel());
        emf.setPersistenceUnitName("parentPU");

        JpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
        emf.setJpaVendorAdapter(vendorAdapter);

        // Apply JPA properties from configuration (Hibernate auto-detects dialect)
        emf.getJpaPropertyMap().putAll(properties.getJpaProperties());

        log.info("Parent application EntityManager configured for model package: {}",
                properties.getModel());

        return emf;
    }

    /**
     * Creates the transaction manager for the parent application.
     * This is marked as @Primary so @Transactional uses it by default.
     */
    @Bean
    @Primary
    @ConditionalOnMissingBean(name = "parentTransactionManager")
    @ConditionalOnProperty(prefix = "blackout.parent.datasource", name = {"repository", "model"})
    public PlatformTransactionManager parentTransactionManager(
            @Qualifier("entityManagerFactory") EntityManagerFactory emf
    ) {
        return new JpaTransactionManager(emf);
    }
}
