package io.github.dekkerding.examples.domain.retrieval.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 检索请求参数
 *
 * @author Notebook RAG Team
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RetrieveRequest {

    /**
     * 查询文本
     */
    private String query;

    /**
     * 返回结果数量
     */
    @Builder.Default
    private int topK = 10;

    /**
     * 最小分数阈值
     */
    @Builder.Default
    private double minScore = 0.0;

    /**
     * 检索类型
     */
    @Builder.Default
    private RetrieveType retrieveType = RetrieveType.HYBRID;

    /**
     * 检索策略名称
     */
    private String strategy;

    /**
     * 知识库ID列表（限定检索范围）
     */
    private String[] knowledgeBaseIds;

    /**
     * 过滤器（元数据过滤）
     */
    private Filter filter;

    /**
     * 是否启用重排序
     */
    @Builder.Default
    private boolean enableRerank = false;

    /**
     * 重排序模型
     */
    private String rerankModel;

    /**
     * 是否使用缓存
     */
    @Builder.Default
    private boolean useCache = true;

    /**
     * 超时时间（毫秒）
     */
    @Builder.Default
    private long timeout = 5000;

    /**
     * 用户ID（用于个性化检索）
     */
    private String userId;

    /**
     * 检索类型枚举
     */
    public enum RetrieveType {
        /**
         * 仅BM25检索
         */
        BM25,
        /**
         * 仅向量检索
         */
        VECTOR,
        /**
         * 仅TF-IDF检索
         */
        TF_IDF,
        /**
         * 混合检索（默认）
         */
        HYBRID,
        /**
         * 多路融合
         */
        MULTI_WAY
    }

    /**
     * 过滤器
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Filter {
        /**
         * 文档类型过滤
         */
        private String documentType;

        /**
         * 创建时间范围（开始）
         */
        private Long createdAfter;

        /**
         * 创建时间范围（结束）
         */
        private Long createdBefore;

        /**
         * 自定义过滤条件
         */
        private java.util.Map<String, Object> conditions;
    }

    /**
     * 获取默认配置
     */
    public static RetrieveRequest defaultConfig() {
        return RetrieveRequest.builder().build();
    }

    /**
     * BM25检索配置
     */
    public static RetrieveRequest bm25Config() {
        return RetrieveRequest.builder()
                .retrieveType(RetrieveType.BM25)
                .build();
    }

    /**
     * 向量检索配置
     */
    public static RetrieveRequest vectorConfig() {
        return RetrieveRequest.builder()
                .retrieveType(RetrieveType.VECTOR)
                .build();
    }

    /**
     * 混合检索配置
     */
    public static RetrieveRequest hybridConfig() {
        return RetrieveRequest.builder()
                .retrieveType(RetrieveType.HYBRID)
                .build();
    }

    /**
     * 验证请求有效性
     */
    public boolean isValid() {
        return query != null && !query.trim().isEmpty()
                && topK > 0;
    }
}
