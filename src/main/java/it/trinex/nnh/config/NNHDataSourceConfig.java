package it.trinex.nnh.config;

import jakarta.persistence.EntityManagerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;

@Configuration
@EnableJpaRepositories(
    basePackages = "${nnh.jpa.repository-package}",
    entityManagerFactoryRef = "nnhEntityManager",
    transactionManagerRef = "nnhTransactionManager"
)
@EntityScan("${nnh.jpa.model-package}")
public class NNHDataSourceConfig {
    @Bean
    @ConditionalOnMissingBean(name = "nnhDataSource")
    @ConfigurationProperties(prefix = "nnh.datasource")
    public DataSource nnhDataSource() {
        return DataSourceBuilder.create().build();
    }

    @Bean
    @ConditionalOnMissingBean(name = "nnhEntityManager")
    public LocalContainerEntityManagerFactoryBean nnhEntityManager(
            EntityManagerFactoryBuilder builder,
            @Qualifier("nnhDataSource") DataSource ds
    ) {
        return builder
                .dataSource(ds)
                .persistenceUnit("nnhPU")
                .build();
    }

    @Bean
    @ConditionalOnMissingBean(name = "nnhTransactionManager")
    public PlatformTransactionManager nnhTransactionManager(
            @Qualifier("nnhEntityManager") EntityManagerFactory emf
    ) {
        return new JpaTransactionManager(emf);
    }
}