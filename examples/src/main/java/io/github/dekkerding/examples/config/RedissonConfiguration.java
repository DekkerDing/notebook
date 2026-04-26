package io.github.dekkerding.examples.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.stream.StreamMessageListenerContainer;
import org.springframework.integration.redis.util.RedisLockRegistry;

import java.io.IOException;

@Configuration
public class RedissonConfiguration {

    @Value("${spring.redis.key-prefix}")
    private String keyPrefix;

    @Bean
    public RedissonClient redissonClient() throws IOException {

        Config config = new Config();

        // 使用 clusterServersConfig 配置 Redis 集群
        config.useClusterServers()
                // 集群节点至少配置一个，Redisson 会自动发现其它节点
                .addNodeAddress(
                        "redis://192.168.10.109:6379",
                        "redis://192.168.10.109:6380",
                        "redis://192.168.10.109:6381",
                        "redis://192.168.10.107:6379",
                        "redis://192.168.10.107:6380",
                        "redis://192.168.10.107:6381"
                )
                .setScanInterval(2000) // 集群状态扫描间隔，默认 1000ms
                .setPassword("drk@2025"); // 如果集群有密码认证

        return Redisson.create(config);
    }

    /**
     * 注册Redis Lock注册器
     *
     * @param factory
     * @return RedisLockRegistry
     */
    @Bean
    public RedisLockRegistry redisLockRegistry(RedisConnectionFactory factory) {
        return new RedisLockRegistry(factory, keyPrefix + "lock");
    }

    @Bean
    public StreamMessageListenerContainer<String, MapRecord<String, String, String>> streamContainer(
            RedisConnectionFactory factory) {
        // 使用默认配置创建容器
        StreamMessageListenerContainer<String, MapRecord<String, String, String>> container =
                StreamMessageListenerContainer.create(factory);
        container.start();
        return container;
    }

}