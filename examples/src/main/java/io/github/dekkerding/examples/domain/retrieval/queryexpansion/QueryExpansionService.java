package io.github.dekkerding.examples.domain.retrieval.queryexpansion;

import lombok.Data;

import java.util.List;

/**
 * 查询扩展服务
 *
 * <p>查询扩展（Query Expansion）通过生成查询变体来提升召回率。</p>
 *
 * <p>扩展方法：</p>
 * <ul>
 *   <li>同义词扩展：将查询词替换为同义词</li>
 *   <li>概念扩展：添加相关概念</li>
 *   <li>LLM生成：使用LLM生成查询变体</li>
 *   <li>历史学习：基于用户历史查询扩展</li>
 * </ul>
 *
 * <p>扩展效果：</p>
 * <ul>
 *   <li>召回率提升：20-30%</li>
 *   <li>延迟增加：+50-200ms（LLM生成）</li>
 *   <li>适用场景：模糊查询、专业术语查询</li>
 * </ul>
 *
 * @author Notebook RAG Team
 * @since 1.0.0
 */
public interface QueryExpansionService {

    /**
     * 扩展查询
     *
     * @param originalQuery 原始查询
     * @return 扩展结果
     */
    QueryExpansionResult expand(String originalQuery);

    /**
     * 批量扩展
     *
     * @param queries 查询列表
     * @return 扩展结果列表
     */
    List<QueryExpansionResult> expandBatch(List<String> queries);

    /**
     * 扩展策略枚举
     */
    enum ExpansionStrategy {
        /**
         * 同义词扩展
         */
        SYNONYM,

        /**
         * 概念扩展
         */
        CONCEPT,

        /**
         * LLM生成扩展
         */
        LLM_GENERATED,

        /**
         * 混合扩展
         */
        HYBRID
    }

    /**
     * 查询扩展结果
     */
    @Data
    class QueryExpansionResult {
        /**
         * 原始查询
         */
        private String originalQuery;

        /**
         * 扩展的查询列表
         */
        private List<String> expandedQueries;

        /**
         * 使用的扩展策略
         */
        private ExpansionStrategy strategy;

        /**
         * 扩展耗时（毫秒）
         */
        private long expansionLatency;

        /**
         * 扩展数量
         */
        private int expansionCount;

        /**
         * 扩展置信度
         */
        private double confidence;
    }
}
