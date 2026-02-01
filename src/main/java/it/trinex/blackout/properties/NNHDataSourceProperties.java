package it.trinex.blackout.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "nnh.datasource")
public class NNHDataSourceProperties {

    private String url;

    private String username;

    private String password;

    private String driverClassName;

    // JPA Properties
    private JpaProperties jpa = new JpaProperties();

    @Data
    public static class JpaProperties {
        private HibernateProperties hibernate = new HibernateProperties();

        @Data
        public static class HibernateProperties {
            private String ddlAuto = "update";
            private String dialect;
        }
    }
}
