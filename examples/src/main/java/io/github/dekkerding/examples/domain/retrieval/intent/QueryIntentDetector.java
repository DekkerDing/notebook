package io.github.dekkerding.examples.domain.retrieval.intent;

import io.github.dekkerding.examples.domain.retrieval.model.RetrieveRequest;
import lombok.Data;

import java.util.*;

/**
 * 查询意图检测器
 *
 * <p>识别用户查询的真实意图，为选择合适的检索策略提供依据。</p>
 *
 * <p>支持的意图类型：</p>
 * <ul>
 *   <li>FACT_QUERY（事实查询）：询问具体事实或信息</li>
 *   <li>CONCEPT_QUERY（概念查询）：询问概念、原理、方法</li>
 *   <li>OPERATION_QUERY（操作查询）：询问如何执行某个操作</li>
 *   <li>RELATION_QUERY（关系查询）：询问实体间的关系</li>
 *   <li>COMPARISON_QUERY（对比查询）：对比两个或多个事物</li>
 *   <li>UNKNOWN（未知）：无法明确分类</li>
 * </ul>
 *
 * @author Notebook RAG Team
 * @since 1.0.0
 */
public interface QueryIntentDetector {

    /**
     * 检测查询意图
     *
     * @param query 查询文本
     * @return 检测结果
     */
    IntentDetectionResult detect(String query);

    /**
     * 批量检测
     *
     * @param queries 查询列表
     * @return 检测结果列表
     */
    List<IntentDetectionResult> detectBatch(List<String> queries);

    /**
     * 获取推荐策略
     *
     * @param intent 查询意图
     * @return 推荐的检索策略
     */
    default String getRecommendedStrategy(QueryIntent intent) {
        switch (intent) {
            case FACT_QUERY:
                return "bm25";  // 事实查询用BM25
            case CONCEPT_QUERY:
                return "vector";  // 概念查询用向量
            case OPERATION_QUERY:
                return "multiway";  // 操作查询用多路融合
            case RELATION_QUERY:
                return "graphrag";  // 关系查询用图谱
            case COMPARISON_QUERY:
                return "multiway";  // 对比查询用多路融合
            default:
                return "multiway";  // 默认多路融合
        }
    }

    /**
     * 查询意图枚举
     */
    enum QueryIntent {
        /**
         * 事实查询
         * 示例："Spring Boot的默认端口是什么？"
         */
        FACT_QUERY,

        /**
         * 概念查询
         * 示例："什么是微服务架构？"
         */
        CONCEPT_QUERY,

        /**
         * 操作查询
         * 示例："如何配置Redis连接池？"
         */
        OPERATION_QUERY,

        /**
         * 关系查询
         * 示例："张三和李四是什么关系？"
         */
        RELATION_QUERY,

        /**
         * 对比查询
         * 示例："HashMap和ConcurrentHashMap的区别？"
         */
        COMPARISON_QUERY,

        /**
         * 未知意图
         */
        UNKNOWN
    }

    /**
     * 意图检测结果
     */
    @Data
    class IntentDetectionResult {
        /**
         * 检测到的意图
         */
        private QueryIntent intent;

        /**
         * 置信度（0-1）
         */
        private double confidence;

        /**
         * 检测到的特征
         */
        private Map<String, Object> features;

        /**
         * 推荐的检索策略
         */
        private String recommendedStrategy;

        /**
         * 原始查询
         */
        private String query;

        /**
         * 检测耗时（毫秒）
         */
        private long detectionLatency;
    }
}
