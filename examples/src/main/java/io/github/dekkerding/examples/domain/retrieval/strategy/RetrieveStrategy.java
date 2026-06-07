package io.github.dekkerding.examples.domain.retrieval.strategy;

import io.github.dekkerding.examples.domain.retrieval.model.RetrieveContext;
import io.github.dekkerding.examples.domain.retrieval.model.RetrieveRequest;
import io.github.dekkerding.examples.domain.retrieval.model.RetrieveResult;

import java.util.List;

/**
 * 检索策略接口 - 所有检索策略的基础接口
 *
 * <p>实现类：</p>
 * <ul>
 *   <li>BM25RetrieveStrategy - BM25稀疏检索</li>
 *   <li>VectorRetrieveStrategy - 向量密集检索</li>
 *   <li>TfIdfRetrieveStrategy - TF-IDF检索</li>
 *   <li>HydeRetrieveStrategy - HyDE查询扩展</li>
 * </ul>
 *
 * @author Notebook RAG Team
 * @since 1.0.0
 */
public interface RetrieveStrategy {

    /**
     * 执行检索
     *
     * @param query 查询文本
     * @param request 检索请求参数
     * @param context 检索上下文
     * @return 检索结果列表
     */
    List<RetrieveResult> retrieve(String query, RetrieveRequest request, RetrieveContext context);

    /**
     * 获取策略名称
     * @return 策略名称（如 "bm25", "vector", "tfidf" 等）
     */
    String getStrategyName();

    /**
     * 获取策略描述
     * @return 策略描述
     */
    default String getDescription() {
        return "Default retrieve strategy";
    }
}
