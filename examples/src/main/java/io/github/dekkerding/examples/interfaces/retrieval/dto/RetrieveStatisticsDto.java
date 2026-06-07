package io.github.dekkerding.examples.interfaces.retrieval.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 检索统计信息
 *
 * @author Notebook RAG Team
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RetrieveStatisticsDto {

    /**
     * 总查询数
     */
    private long totalQueries;

    /**
     * 平均延迟（毫秒）
     */
    private double averageLatency;

    /**
     * 缓存命中率
     */
    private double cacheHitRate;

    /**
     * 向量检索次数
     */
    private long vectorSearchCount;

    /**
     * 关键词检索次数
     */
    private long keywordSearchCount;

    /**
     * 混合检索次数
     */
    private long hybridSearchCount;
}
