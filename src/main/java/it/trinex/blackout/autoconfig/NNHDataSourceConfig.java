package it.trinex.blackout.autoconfig;

import it.trinex.blackout.properties.NNHDataSourceProperties;
import jakarta.persistence.EntityManagerFactory;
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

@AutoConfiguration
@EnableJpaRepositories(
        basePackages = "it.trinex.blackout",
        entityManagerFactoryRef = "nnhEntityManager",
        transactionManagerRef = "nnhTransactionManager"
)
@EnableConfigurationProperties(NNHDataSourceProperties.class)
public class NNHDataSourceConfig {

    @Bean
    @ConditionalOnMissingBean(name = "nnhDataSource")
    public DataSource nnhDataSource(NNHDataSourceProperties properties) {
        return DataSourceBuilder.create()
                .url(properties.getUrl())
                .username(properties.getUsername())
                .password(properties.getPassword())
                .driverClassName(properties.getDriverClassName())
                .build();
    }

    @Bean
    @ConditionalOnMissingBean(name = "nnhEntityManager")
    public LocalContainerEntityManagerFactoryBean nnhEntityManager(
            @Qualifier("nnhDataSource") DataSource ds,
            NNHDataSourceProperties properties
    ) {
        LocalContainerEntityManagerFactoryBean emf = new LocalContainerEntityManagerFactoryBean();
        emf.setDataSource(ds);
        emf.setPackagesToScan("it.trinex.nnh.model");
        emf.setPersistenceUnitName("nnhPU");

        JpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
        emf.setJpaVendorAdapter(vendorAdapter);

        // Get Hibernate dialect from properties or auto-detect from driver
        String dialect = properties.getJpa().getHibernate().getDialect();
        if (dialect == null || dialect.isEmpty()) {
            dialect = detectDialect(properties.getDriverClassName());
        }

        // Get ddl-auto from properties or use default
        String ddlAuto = properties.getJpa().getHibernate().getDdlAuto();
        if (ddlAuto == null || ddlAuto.isEmpty()) {
            ddlAuto = "update";
        }

        // Set JPA properties
        emf.getJpaPropertyMap().put("hibernate.hbm2ddl.auto", ddlAuto);
        emf.getJpaPropertyMap().put("hibernate.dialect", dialect);
        emf.getJpaPropertyMap().put("hibernate.format_sql", "true");

        return emf;
    }

    @Bean
    @ConditionalOnMissingBean(name = "nnhTransactionManager")
    public PlatformTransactionManager nnhTransactionManager(
            @Qualifier("nnhEntityManager") EntityManagerFactory emf
    ) {
        return new JpaTransactionManager(emf);
    }

    /**
     * Auto-detect Hibernate dialect based on JDBC driver class name
     */
    private String detectDialect(String driverClassName) {
        if (driverClassName == null) {
            return "org.hibernate.dialect.PostgreSQLDialect";
        }

        if (driverClassName.contains("postgresql")) {
            return "org.hibernate.dialect.PostgreSQLDialect";
        } else if (driverClassName.contains("mysql")) {
            return "org.hibernate.dialect.MySQLDialect";
        } else if (driverClassName.contains("h2")) {
            return "org.hibernate.dialect.H2Dialect";
        } else if (driverClassName.contains("hsql")) {
            return "org.hibernate.dialect.HSQLDialect";
        } else if (driverClassName.contains("mariadb")) {
            return "org.hibernate.dialect.MariaDBDialect";
        } else if (driverClassName.contains("oracle")) {
            return "org.hibernate.dialect.OracleDialect";
        } else if (driverClassName.contains("mssql") || driverClassName.contains("sqlserver")) {
            return "org.hibernate.dialect.SQLServerDialect";
        }

        // Default to PostgreSQL
        return "org.hibernate.dialect.PostgreSQLDialect";
    }
}
