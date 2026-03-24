package it.trinex.blackout.autoconfig;

import it.trinex.blackout.properties.RedisProperties;
import it.trinex.blackout.service.redis.RedisService;
import it.trinex.blackout.service.redis.RealRedisService;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;

@AutoConfiguration
@EnableConfigurationProperties(RedisProperties.class)
@ConditionalOnProperty(prefix = "blackout.redis", name = "enabled", havingValue = "true")
public class RedisConfiguration {

    private final RedisProperties redisProperties;

    public RedisConfiguration(RedisProperties redisProperties) {
        this.redisProperties = redisProperties;
    }

    @Bean
    public LettuceConnectionFactory redisConnectionFactory() {
        // RedisStandaloneConfiguration = configurazione per un singolo nodo Redis (non cluster)
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration();
        config.setHostName(redisProperties.getHost());
        config.setPort(redisProperties.getPort());

        // Imposta la password solo se è stata configurata
        if (redisProperties.getPassword() != null && !redisProperties.getPassword().isBlank()) {
            config.setPassword(redisProperties.getPassword());
        }

        return new LettuceConnectionFactory(config);
    }

    @Bean
    public RedisTemplate<String, String> redisTemplate(LettuceConnectionFactory connectionFactory) {
        RedisTemplate<String, String> template = new RedisTemplate<>();

        template.setConnectionFactory(connectionFactory);

        return template;
    }

    @Bean
    public RedisService redisService(RedisTemplate<String, String> redisTemplate) {
        return new RealRedisService(redisTemplate);
    }

}
