package it.trinex.blackout.autoconfig;

import it.trinex.blackout.service.redis.BitchAssRedisService;
import it.trinex.blackout.service.redis.RedisService;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

/**
 * Fallback configuration that provides a no-op RedisService when Redis is disabled.
 * This bean is only created when blackout.redis.enabled=false or when the property is not set.
 */
@AutoConfiguration
@ConditionalOnProperty(prefix = "blackout.redis", name = "enabled", havingValue = "false", matchIfMissing = true)
public class BitchAssRedisConfiguration {

    @Bean
    @ConditionalOnMissingBean(RedisService.class)
    public RedisService redisService() {
        return new BitchAssRedisService();
    }

}
