package io.github.dekkerding.examples.domain.retrieval.service;

import io.github.dekkerding.examples.domain.retrieval.model.RetrieveContext;
import io.github.dekkerding.examples.domain.retrieval.model.RetrieveRequest;
import io.github.dekkerding.examples.domain.retrieval.model.RetrieveResult;

import java.util.List;

/**
 * 检索服务接口 - 统一的检索服务入口
 *
 * <p>功能：</p>
 * <ul>
 *   <li>文本检索（单个/批量）</li>
 *   <li>向量检索</li>
 *   <li>混合检索</li>
 *   <li>检索结果过滤与排序</li>
 * </ul>
 *
 * @author Notebook RAG Team
 * @since 1.0.0
 */
public interface RetrieveService {

    /**
     * 简单文本检索
     * @param query 查询文本
     * @param topK 返回结果数量
     * @return 检索结果列表
     */
    List<RetrieveResult> retrieve(String query, int topK);

    /**
     * 使用请求参数进行检索
     * @param query 查询文本
     * @param request 检索请求参数
     * @return 检索结果列表
     */
    List<RetrieveResult> retrieve(String query, RetrieveRequest request);

    /**
     * 使用上下文进行检索
     * @param context 检索上下文
     * @return 检索结果列表
     */
    List<RetrieveResult> retrieve(RetrieveContext context);

    /**
     * 多路检索融合
     * @param query 查询文本
     * @param topK 每路检索返回数量
     * @return 融合后的检索结果
     */
    List<RetrieveResult> multiWayRetrieve(String query, int topK);

    /**
     * 向量检索
     * @param queryVector 查询向量
     * @param topK 返回结果数量
     * @return 检索结果列表
     */
    List<RetrieveResult> vectorRetrieve(float[] queryVector, int topK);

    /**
     * 批量检索
     * @param queries 查询文本列表
     * @param topK 每个查询返回结果数量
     * @return 检索结果列表（包含所有查询的结果）
     */
    List<RetrieveResult> retrieveBatch(List<String> queries, int topK);

    /**
     * 获取检索统计信息
     * @return 检索统计
     */
    RetrieveStats getStats();

    /**
     * 检索统计信息
     */
    class RetrieveStats {
        public long totalRetrieves;
        public long totalResults;
        public double avgResultsPerRetrieve;
        public double avgLatency;
        public long cacheHits;
        public long cacheMisses;

        public double getCacheHitRate() {
            long total = cacheHits + cacheMisses;
            return total > 0 ? (double) cacheHits / total : 0.0;
        }

        @Override
        public String toString() {
            return "RetrieveStats{" +
                    "totalRetrieves=" + totalRetrieves +
                    ", totalResults=" + totalResults +
                    ", avgResultsPerRetrieve=" + String.format("%.2f", avgResultsPerRetrieve) +
                    ", avgLatency=" + String.format("%.2fms", avgLatency) +
                    ", cacheHitRate=" + String.format("%.2f%%", getCacheHitRate() * 100) +
                    '}';
        }
    }
}
