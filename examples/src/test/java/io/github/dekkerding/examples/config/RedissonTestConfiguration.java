package io.github.dekkerding.examples.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.integration.redis.util.RedisLockRegistry;

import java.io.IOException;

/**
 * 测试环境Redisson配置 - 支持单机模式
 *
 * @author Redis ToolKit Team
 * @since 1.0.0
 */
@Configuration
@Profile("test")
public class RedissonTestConfiguration {

    @Value("${spring.redis.host:localhost}")
    private String redisHost;

    @Value("${spring.redis.port:6379}")
    private int redisPort;

    @Value("${spring.redis.password:}")
    private String redisPassword;

    @Value("${spring.redis.key-prefix:examplesApplication:test:}")
    private String keyPrefix;

    @Bean
    public RedissonClient redissonClient() throws IOException {
        Config config = new Config();

        // 使用单服务器配置
        config.useSingleServer()
                .setAddress("redis://" + redisHost + ":" + redisPort)
                .setPassword(redisPassword.isEmpty() ? null : redisPassword)
                .setDatabase(0)
                .setConnectionPoolSize(8)
                .setConnectionMinimumIdleSize(2)
                .setTimeout(10000)
                .setRetryAttempts(3)
                .setRetryInterval(1000);

        return Redisson.create(config);
    }

    /**
     * 注册Redis Lock注册器
     */
    @Bean
    public RedisLockRegistry redisLockRegistry(RedisConnectionFactory factory) {
        return new RedisLockRegistry(factory, keyPrefix + "lock");
    }
}
