package it.trinex.nnh.autoconfig;

import it.trinex.nnh.properties.NNHDataSourceProperties;
import jakarta.persistence.EntityManagerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;

@AutoConfiguration
@EnableJpaRepositories(
    basePackages = "it.trinex.nnh",
    entityManagerFactoryRef = "nnhEntityManager",
    transactionManagerRef = "nnhTransactionManager"
)
@EntityScan("it.trinex.nnh.model")
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
            EntityManagerFactoryBuilder builder,
            @Qualifier("nnhDataSource") DataSource ds
    ) {
        return builder
                .dataSource(ds)
                .packages("it.trinex.nnh.model")
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