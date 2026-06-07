package io.github.dekkerding.examples.domain.embedding.service;

import io.github.dekkerding.examples.domain.embedding.model.EmbeddingRequest;
import io.github.dekkerding.examples.domain.embedding.model.EmbeddingResult;
import io.github.dekkerding.examples.domain.embedding.provider.EmbeddingProvider;

import java.util.List;

/**
 * 文本向量化服务 - 统一的向量化服务入口
 *
 * <p>功能：</p>
 * <ul>
 *   <li>文本向量化（单个/批量）</li>
 *   <li>向量缓存管理</li>
 *   <li>批量处理优化</li>
 *   <li>多提供商支持</li>
 * </ul>
 *
 * @author Notebook RAG Team
 * @since 1.0.0
 */
public interface EmbeddingService {

    /**
     * 对单个文本进行向量化
     * @param text 输入文本
     * @return 向量结果
     */
    EmbeddingResult embed(String text);

    /**
     * 对单个文本进行向量化（使用请求参数）
     * @param text 输入文本
     * @param request 向量化请求参数
     * @return 向量结果
     */
    EmbeddingResult embed(String text, EmbeddingRequest request);

    /**
     * 批量向量化
     * @param texts 文本列表
     * @return 向量结果列表
     */
    List<EmbeddingResult> embedBatch(List<String> texts);

    /**
     * 批量向量化（使用请求参数）
     * @param texts 文本列表
     * @param request 向量化请求参数
     * @return 向量结果列表
     */
    List<EmbeddingResult> embedBatch(List<String> texts, EmbeddingRequest request);

    /**
     * 获取当前使用的提供商
     * @return 向量化提供商
     */
    EmbeddingProvider getProvider();

    /**
     * 设置向量化提供商
     * @param provider 提供商实例
     */
    void setProvider(EmbeddingProvider provider);

    /**
     * 清空缓存
     */
    void clearCache();

    /**
     * 获取缓存统计
     * @return 缓存统计信息
     */
    CacheStats getCacheStats();

    /**
     * 缓存统计信息
     */
    class CacheStats {
        public long hitCount;
        public long missCount;
        public long totalCount;
        public double hitRate;

        public CacheStats(long hitCount, long missCount) {
            this.hitCount = hitCount;
            this.missCount = missCount;
            this.totalCount = hitCount + missCount;
            this.hitRate = totalCount > 0 ? (double) hitCount / totalCount : 0.0;
        }

        @Override
        public String toString() {
            return "CacheStats{" +
                    "hitCount=" + hitCount +
                    ", missCount=" + missCount +
                    ", hitRate=" + String.format("%.2f%%", hitRate * 100) +
                    '}';
        }
    }
}
