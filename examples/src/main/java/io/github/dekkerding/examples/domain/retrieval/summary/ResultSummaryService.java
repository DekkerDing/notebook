package io.github.dekkerding.examples.domain.retrieval.summary;

import io.github.dekkerding.examples.domain.retrieval.model.RetrieveResult;
import lombok.Data;

import java.util.List;

/**
 * 检索结果摘要服务
 *
 * <p>对检索结果进行智能摘要，提升用户体验，降低Token消耗。</p>
 *
 * <p>摘要策略：</p>
 * <ul>
 *   <li>Extractive（抽取式）：从原文中提取关键句子</li>
 *   <li>Abstractive（生成式）：使用LLM重新生成摘要</li>
 *   <li>Hybrid（混合式）：结合两种策略</li>
 * </ul>
 *
 * <p>价值：</p>
 * <ul>
 *   <li>快速获取关键信息：用户无需阅读所有结果</li>
 *   <li>降低Token消耗：摘要比原文短50-70%</li>
 *   <li>提升满意度：精准摘要提升用户体验</li>
 * </ul>
 *
 * @author Notebook RAG Team
 * @since 1.0.0
 */
public interface ResultSummaryService {

    /**
     * 生成单个结果的摘要
     *
     * @param result 检索结果
     * @return 摘要结果
     */
    SummaryResult summarize(RetrieveResult result);

    /**
     * 生成多个结果的汇总摘要
     *
     * @param results 检索结果列表
     * @param query 原始查询
     * @return 汇总摘要
     */
    AggregateSummary summarizeAggregate(List<RetrieveResult> results, String query);

    /**
     * 批量生成摘要
     *
     * @param results 结果列表
     * @return 摘要结果列表
     */
    List<SummaryResult> summarizeBatch(List<RetrieveResult> results);

    /**
     * 摘要策略枚举
     */
    enum SummaryStrategy {
        /**
         * 抽取式摘要
         */
        EXTRACTIVE,

        /**
         * 生成式摘要
         */
        ABSTRACTIVE,

        /**
         * 混合摘要
         */
        HYBRID
    }

    /**
     * 摘要结果
     */
    @Data
    class SummaryResult {
        /**
         * 原始文档ID
         */
        private String docId;

        /**
         * 摘要文本
         */
        private String summary;

        /**
         * 摘要策略
         */
        private SummaryStrategy strategy;

        /**
         * 压缩率（摘要长度/原文长度）
         */
        private double compressionRatio;

        /**
         * 关键句子列表
         */
        private List<String> keySentences;

        /**
         * 生成耗时（毫秒）
         */
        private long generationLatency;
    }

    /**
     * 汇总摘要
     */
    @Data
    class AggregateSummary {
        /**
         * 原始查询
         */
        private String query;

        /**
         * 汇总摘要
         */
        private String summary;

        /**
         * 关键点列表
         */
        private List<String> keyPoints;

        /**
         * 涵盖的文档数量
         */
        private int coveredDocCount;

        /**
         * 摘要策略
         */
        private SummaryStrategy strategy;

        /**
         * 生成耗时（毫秒）
         */
        private long generationLatency;
    }
}
