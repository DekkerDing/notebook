package io.github.dekkerding.examples.domain.chunk.service;

import io.github.dekkerding.examples.domain.chunk.model.ChunkConfig;
import io.github.dekkerding.examples.domain.chunk.model.ChunkNode;
import io.github.dekkerding.examples.domain.chunk.strategy.ChunkStrategy;
import io.github.dekkerding.examples.domain.chunk.strategy.impl.RecursiveChunkStrategy;
import io.github.dekkerding.examples.domain.chunk.strategy.impl.SeparatorChunkStrategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 文本分块服务 - 统一的分块服务入口
 *
 * <p>功能：</p>
 * <ul>
 *   <li>管理多个分块策略</li>
 *   <li>自动选择最佳策略</li>
 *   <li>提供分块统计和质量评估</li>
 *   <li>支持批量处理</li>
 * </ul>
 *
 * <p>使用示例：</p>
 * <pre>
 * // 使用默认策略
 * List&lt;ChunkNode&gt; chunks = chunkSplitService.split(text);
 *
 * // 使用指定策略
 * List&lt;ChunkNode&gt; chunks = chunkSplitService.split(text, "recursive");
 *
 * // 使用自定义配置
 * ChunkConfig config = ChunkConfig.builder()
 *     .chunkSize(500)
 *     .chunkOverlap(50)
 *     .build();
 * List&lt;ChunkNode&gt; chunks = chunkSplitService.split(text, "separator", config);
 * </pre>
 *
 * @author Notebook RAG Team
 * @since 1.0.0
 */
@Slf4j
@Service
public class ChunkSplitService {

    /**
     * 策略注册表
     */
    private final Map<String, ChunkStrategy> strategies = new ConcurrentHashMap<>();

    /**
     * 默认策略名称
     */
    private static final String DEFAULT_STRATEGY = "recursive";

    @Autowired(required = false)
    private SeparatorChunkStrategy separatorChunkStrategy;

    @Autowired(required = false)
    private RecursiveChunkStrategy recursiveChunkStrategy;

    /**
     * 初始化策略注册表
     */
    @PostConstruct
    public void init() {
        // 注册自动注入的策略
        registerStrategyIfPresent(separatorChunkStrategy);
        registerStrategyIfPresent(recursiveChunkStrategy);

        log.info("分块服务初始化完成: 已注册策略={}", strategies.keySet());
    }

    /**
     * 注册策略（如果存在）
     */
    private void registerStrategyIfPresent(ChunkStrategy strategy) {
        if (strategy != null) {
            strategies.put(strategy.getStrategyName(), strategy);
            log.debug("注册分块策略: {}", strategy.getStrategyName());
        }
    }

    /**
     * 手动注册策略
     * @param strategy 策略实例
     */
    public void registerStrategy(ChunkStrategy strategy) {
        if (strategy != null) {
            strategies.put(strategy.getStrategyName(), strategy);
            log.info("手动注册分块策略: {}", strategy.getStrategyName());
        }
    }

    /**
     * 使用默认策略和配置进行分块
     * @param text 待分块的文本
     * @return 分块列表
     */
    public List<ChunkNode> split(String text) {
        return split(text, DEFAULT_STRATEGY, null);
    }

    /**
     * 使用指定策略进行分块（使用默认配置）
     * @param text 待分块的文本
     * @param strategyName 策略名称
     * @return 分块列表
     */
    public List<ChunkNode> split(String text, String strategyName) {
        return split(text, strategyName, null);
    }

    /**
     * 使用指定策略和配置进行分块
     * @param text 待分块的文本
     * @param strategyName 策略名称
     * @param config 分块配置（为null时使用默认配置）
     * @return 分块列表
     */
    public List<ChunkNode> split(String text, String strategyName, ChunkConfig config) {
        log.info("开始分块: textLength={}, strategy={}, config={}",
                text != null ? text.length() : 0, strategyName, config);

        // 获取策略
        ChunkStrategy strategy = getStrategy(strategyName);
        if (strategy == null) {
            log.error("未找到分块策略: {}", strategyName);
            return Collections.emptyList();
        }

        // 使用默认配置
        if (config == null) {
            config = ChunkConfig.defaultConfig();
        }

        // 验证配置
        if (!config.isValid()) {
            throw new IllegalArgumentException("Invalid chunk config");
        }

        // 执行分块
        List<ChunkNode> chunks = strategy.split(text, config);

        log.info("分块完成: chunks={}", chunks.size());
        return chunks;
    }

    /**
     * 自动选择最佳策略并分块
     * <p>根据文本特征自动选择最合适的策略</p>
     *
     * @param text 待分块的文本
     * @return 分块列表
     */
    public List<ChunkNode> splitWithAutoStrategy(String text) {
        String bestStrategy = selectBestStrategy(text);
        log.info("自动选择策略: {}", bestStrategy);
        return split(text, bestStrategy);
    }

    /**
     * 批量分块
     * @param texts 文本列表
     * @param strategyName 策略名称
     * @param config 分块配置
     * @return 分块列表（包含所有文本的分块）
     */
    public List<ChunkNode> splitBatch(List<String> texts, String strategyName, ChunkConfig config) {
        List<ChunkNode> allChunks = new ArrayList<>();

        for (int i = 0; i < texts.size(); i++) {
            String text = texts.get(i);
            List<ChunkNode> chunks = split(text, strategyName, config);

            // 标记来源文档
            String docId = "doc_" + i;
            for (ChunkNode chunk : chunks) {
                chunk.getMetadata().setSourceDocId(docId);
            }

            allChunks.addAll(chunks);
        }

        return allChunks;
    }

    /**
     * 获取分块统计信息
     */
    public ChunkStatistics getStatistics(List<ChunkNode> chunks) {
        if (chunks == null || chunks.isEmpty()) {
            return new ChunkStatistics();
        }

        ChunkStatistics stats = new ChunkStatistics();
        stats.totalChunks = chunks.size();

        // 大小统计
        IntSummaryStatistics sizeStats = chunks.stream()
                .mapToInt(ChunkNode::getLength)
                .summaryStatistics();

        stats.minSize = sizeStats.getMin();
        stats.maxSize = sizeStats.getMax();
        stats.avgSize = sizeStats.getAverage();
        stats.totalChars = sizeStats.getSum();

        // Token统计
        IntSummaryStatistics tokenStats = chunks.stream()
                .mapToInt(ChunkNode::estimateTokens)
                .summaryStatistics();

        stats.minTokens = tokenStats.getMin();
        stats.maxTokens = tokenStats.getMax();
        stats.avgTokens = tokenStats.getAverage();
        stats.totalTokens = tokenStats.getSum();

        // 类型分布
        Map<String, Long> typeDistribution = new HashMap<>();
        for (ChunkNode chunk : chunks) {
            String type = chunk.getMetadata().getChunkType().name();
            typeDistribution.put(type, typeDistribution.getOrDefault(type, 0L) + 1);
        }
        stats.typeDistribution = typeDistribution;

        // 策略分布
        Map<String, Long> strategyDistribution = new HashMap<>();
        for (ChunkNode chunk : chunks) {
            String strategy = chunk.getMetadata().getStrategyName();
            if (strategy != null) {
                strategyDistribution.put(strategy, strategyDistribution.getOrDefault(strategy, 0L) + 1);
            }
        }
        stats.strategyDistribution = strategyDistribution;

        return stats;
    }

    /**
     * 选择最佳策略
     */
    private String selectBestStrategy(String text) {
        if (text == null || text.isEmpty()) {
            return DEFAULT_STRATEGY;
        }

        // 简单启发式规则
        int lineCount = text.split("\n").length;
        int paragraphCount = text.split("\n\n").length;
        double avgLineLength = text.length() / (double) lineCount;

        // 有明显段落结构
        if (paragraphCount > 3 && paragraphCount < lineCount / 2) {
            return "separator";
        }

        // 行较长，适合递归分块
        if (avgLineLength > 100) {
            return "recursive";
        }

        return DEFAULT_STRATEGY;
    }

    /**
     * 获取策略
     */
    private ChunkStrategy getStrategy(String strategyName) {
        if (strategyName == null || strategyName.isEmpty()) {
            strategyName = DEFAULT_STRATEGY;
        }
        return strategies.get(strategyName);
    }

    /**
     * 获取所有已注册的策略
     */
    public Set<String> getRegisteredStrategies() {
        return new HashSet<>(strategies.keySet());
    }

    /**
     * 分块统计信息
     */
    public static class ChunkStatistics {
        public int totalChunks;
        public int minSize;
        public int maxSize;
        public double avgSize;
        public long totalChars;
        public int minTokens;
        public int maxTokens;
        public double avgTokens;
        public long totalTokens;
        public Map<String, Long> typeDistribution;
        public Map<String, Long> strategyDistribution;

        public ChunkStatistics() {
            this.typeDistribution = new HashMap<>();
            this.strategyDistribution = new HashMap<>();
        }

        @Override
        public String toString() {
            return "ChunkStatistics{" +
                    "totalChunks=" + totalChunks +
                    ", minSize=" + minSize +
                    ", maxSize=" + maxSize +
                    ", avgSize=" + String.format("%.2f", avgSize) +
                    ", totalChars=" + totalChars +
                    ", avgTokens=" + String.format("%.2f", avgTokens) +
                    ", totalTokens=" + totalTokens +
                    ", typeDistribution=" + typeDistribution +
                    ", strategyDistribution=" + strategyDistribution +
                    '}';
        }
    }
}
