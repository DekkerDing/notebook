package io.github.dekkerding.examples.interfaces.retrieval.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Min;
import javax.validation.constraints.Max;

/**
 * 搜索请求
 *
 * @author Notebook RAG Team
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchRequest {

    /**
     * 查询文本
     */
    @NotBlank(message = "查询文本不能为空")
    private String query;

    /**
     * 返回结果数量
     */
    @Min(value = 1, message = "topK最小为1")
    @Max(value = 100, message = "topK最大为100")
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
    private String retrieveType = "HYBRID";

    /**
     * 知识库ID列表
     */
    private String[] knowledgeBaseIds;

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
     * 用户ID
     */
    private String userId;
}
