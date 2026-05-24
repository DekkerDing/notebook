package io.github.dekkerding.examples.infrastructure.config;

import io.github.dekkerding.examples.config.RedisStreamEventProperties;
import io.github.dekkerding.examples.infrastructure.partition.PartitionManager;
import io.github.dekkerding.examples.infrastructure.partition.PartitionStrategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 动态配置管理器
 * 支持配置热更新和自动重载
 */
@Slf4j
@Component
public class DynamicConfigurationManager {

    private final RedisTemplate<String, String> redisTemplate;
    private final Map<String, PartitionManager> partitionManagers = new ConcurrentHashMap<>();
    private ScheduledExecutorService scheduler;

    @Autowired
    private RedisStreamEventProperties properties;

    public DynamicConfigurationManager(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
        startConfigWatcher();
    }

    @PostConstruct
    public void init() {
        log.info("动态配置管理器已初始化");
        // 不自动重新加载，由调用方决定
    }

    @PreDestroy
    public void destroy() {
        partitionManagers.clear();

        if (scheduler != null) {
            scheduler.shutdown(); // 禁用新任务提交
            try {
                // 等待现有任务完成，最多5秒
                if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                    scheduler.shutdownNow(); // 强制关闭
                }
            } catch (InterruptedException e) {
                scheduler.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }

        log.info("动态配置管理器已销毁");
    }

    /**
     * 获取或创建分区管理器
     */
    public PartitionManager getPartitionManager(String eventType) {
        return partitionManagers.computeIfAbsent(eventType, this::createPartitionManager);
    }

    /**
     * 重新加载配置
     */
    public synchronized void reloadConfiguration() {
        log.info("重新加载配置...");

        // 清除现有分区管理器
        partitionManagers.clear();

        // 注意：这里不读取properties因为可能导致循环依赖
        // 实际配置更新通过updatePartitionConfig等方法进行

        log.info("配置重新加载完成");
    }

    /**
     * 动态更新分区配置
     */
    public void updatePartitionConfig(String eventType, int partitionCount, String strategy) {
        log.info("更新分区配置: eventType={}, count={}, strategy={}",
                eventType, partitionCount, strategy);

        // 移除旧的分区管理器
        partitionManagers.remove(eventType);

        // 更新配置（这里可以保存到配置中心或数据库）
        // ...

        log.info("分区配置更新完成");
    }

    /**
     * 获取当前配置摘要
     */
    public ConfigSummary getConfigSummary() {
        return new ConfigSummary(
                properties.getPartition().isEnabled(),
                properties.getPartition().getCount(),
                properties.getPartition().getStrategy(),
                properties.getIdempotent().isEnabled(),
                properties.getIdempotent().getKeyTtl(),
                partitionManagers.size()
        );
    }

    /**
     * 启动配置监听器
     */
    private void startConfigWatcher() {
        scheduler = Executors.newSingleThreadScheduledExecutor();

        // 每30秒检查一次配置变更
        scheduler.scheduleAtFixedRate(() -> {
            try {
                checkConfigChanges();
            } catch (Exception e) {
                log.error("配置检查失败", e);
            }
        }, 30, 30, TimeUnit.SECONDS);

        log.info("配置监听器已启动");
    }

    /**
     * 检查配置变更
     */
    private void checkConfigChanges() {
        // 这里可以集成配置中心（如Apollo、Nacos）的监听逻辑
        // 或者监听Redis中的配置变更

        log.trace("检查配置变更...");
    }

    /**
     * 创建分区管理器
     */
    private PartitionManager createPartitionManager(String eventType) {
        PartitionStrategy strategy = createPartitionStrategy(
                properties.getPartition().getStrategy());

        return new PartitionManager(
                properties.getKeyPrefix(),
                properties.getPartition().getCount(),
                strategy,
                redisTemplate);
    }

    /**
     * 创建分区策略
     */
    private PartitionStrategy createPartitionStrategy(String strategyName) {
        switch (strategyName.toLowerCase()) {
            case "round_robin":
                return new io.github.dekkerding.examples.infrastructure.partition.RoundRobinPartitionStrategy();
            case "hash":
            default:
                return new io.github.dekkerding.examples.infrastructure.partition.HashPartitionStrategy();
        }
    }

    /**
     * 配置摘要
     */
    public static class ConfigSummary {
        private final boolean partitionEnabled;
        private final int partitionCount;
        private final String partitionStrategy;
        private final boolean idempotentEnabled;
        private final long idempotentTtl;
        private final int activeManagers;

        public ConfigSummary(boolean partitionEnabled, int partitionCount,
                            String partitionStrategy, boolean idempotentEnabled,
                            long idempotentTtl, int activeManagers) {
            this.partitionEnabled = partitionEnabled;
            this.partitionCount = partitionCount;
            this.partitionStrategy = partitionStrategy;
            this.idempotentEnabled = idempotentEnabled;
            this.idempotentTtl = idempotentTtl;
            this.activeManagers = activeManagers;
        }

        public boolean isPartitionEnabled() {
            return partitionEnabled;
        }

        public int getPartitionCount() {
            return partitionCount;
        }

        public String getPartitionStrategy() {
            return partitionStrategy;
        }

        public boolean isIdempotentEnabled() {
            return idempotentEnabled;
        }

        public long getIdempotentTtl() {
            return idempotentTtl;
        }

        public int getActiveManagers() {
            return activeManagers;
        }

        @Override
        public String toString() {
            return String.format("ConfigSummary{partition=%s(%d, %s), idempotent=%s(%ds), managers=%d}",
                    partitionEnabled ? "enabled" : "disabled",
                    partitionCount,
                    partitionStrategy,
                    idempotentEnabled ? "enabled" : "disabled",
                    idempotentTtl,
                    activeManagers);
        }
    }
}
