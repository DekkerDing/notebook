package io.github.dekkerding.examples.config;

import io.github.dekkerding.examples.application.EnhancedTwoLevelCacheService;
import lombok.extern.slf4j.Slf4j;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.integration.redis.util.RedisLockRegistry;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Redisson 配置类
 *
 * <p>支持中文的Redis配置：
 * <ul>
 *   <li>使用UTF-8编码确保中文字符正确显示</li>
 *   <li>配置StringRedisTemplate支持中文</li>
 *   <li>配置RedisTemplate支持中文</li>
 * </ul>
 *
 * @author Redis ToolKit Team
 * @since 1.0.0
 */
@Slf4j
@Configuration
public class RedissonConfiguration {

    @Value("${spring.redis.key-prefix}")
    private String keyPrefix;

    /**
     * 创建支持中文的RedissonClient
     *
     * <p>配置说明：
     * <ul>
     *   <li>使用默认配置，Redisson 3.x默认使用UTF-8编码</li>
     *   <li>通过RBucket<String>指定字符串类型，支持中文</li>
     *   <li>所有字符使用UTF-8编码</li>
     * </ul>
     */
    @Bean
    public RedissonClient redissonClient() throws IOException {

        Config config = new Config();

        // Redisson 3.x 默认使用UTF-8编码，支持中文
        // 使用集群配置
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

        RedissonClient redissonClient = Redisson.create(config);

        log.info("Redisson客户端初始化完成（支持中文）: version={}, nodes={}",
                "3.16.8", "192.168.10.109:6379,192.168.10.107:6379");

        return redissonClient;
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

    /**
     * 配置RedisTemplate，支持中文序列化
     *
     * <p>使用StringRedisSerializer序列化key和value，
     * 确保中文字符在Redis中正确存储和显示。
     *
     * @return RedisTemplate
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory factory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);

        // 使用StringRedisSerializer，支持中文
        StringRedisSerializer stringSerializer = new StringRedisSerializer(StandardCharsets.UTF_8);

        // key使用String序列化
        template.setKeySerializer(stringSerializer);
        template.setHashKeySerializer(stringSerializer);

        // value使用String序列化
        template.setValueSerializer(stringSerializer);
        template.setHashValueSerializer(stringSerializer);

        template.afterPropertiesSet();

        log.info("RedisTemplate初始化完成（支持中文）");

        return template;
    }

    /**
     * StringRedisTemplate配置
     *
     * <p>专门用于处理字符串类型的数据，天然支持中文。
     *
     * @return StringRedisTemplate
     */
    @Bean
    public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory factory) {
        StringRedisTemplate template = new StringRedisTemplate();
        template.setConnectionFactory(factory);

        // 确保使用UTF-8编码
        StringRedisSerializer stringSerializer = new StringRedisSerializer(StandardCharsets.UTF_8);
        template.setKeySerializer(stringSerializer);
        template.setValueSerializer(stringSerializer);
        template.setHashKeySerializer(stringSerializer);
        template.setHashValueSerializer(stringSerializer);

        template.afterPropertiesSet();

        log.info("StringRedisTemplate初始化完成（支持中文）");

        return template;
    }

    /**
     * 增强型缓存配置（仅在test profile下生效）
     *
     * <p>在test profile下，将EnhancedTwoLevelCacheService配置为primary bean，
     * 替代标准的TwoLevelCacheService。
     */
    @Configuration
    @Profile("test")
    static class EnhancedCacheConfiguration {

        /**
         * 配置增强型缓存服务作为primary bean
         */
        @Bean
        @Primary
        @ConditionalOnMissingBean(EnhancedTwoLevelCacheService.class)
        public EnhancedTwoLevelCacheService enhancedTwoLevelCacheService() {
            return new EnhancedTwoLevelCacheService();
        }
    }
}