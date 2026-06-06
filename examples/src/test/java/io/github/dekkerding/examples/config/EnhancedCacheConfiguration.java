package io.github.dekkerding.examples.config;

import io.github.dekkerding.examples.application.EnhancedTwoLevelCacheService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

/**
 * 增强型缓存配置
 *
 * <p>在test profile下，将EnhancedTwoLevelCacheService配置为primary bean，
 * 替代标准的TwoLevelCacheService。
 *
 * @author Redis ToolKit Team
 * @since 1.0.0
 */
@Configuration
@Profile("test")
public class EnhancedCacheConfiguration {

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
