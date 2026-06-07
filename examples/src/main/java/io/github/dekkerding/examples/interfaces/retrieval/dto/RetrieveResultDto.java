package io.github.dekkerding.examples.interfaces.retrieval.dto;

import io.github.dekkerding.examples.domain.retrieval.model.RetrieveResult;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * 检索结果DTO
 *
 * @author Notebook RAG Team
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RetrieveResultDto {

    /**
     * 文档ID
     */
    private String docId;

    /**
     * 文本内容
     */
    private String text;

    /**
     * 检索分数
     */
    private Double score;

    /**
     * 检索来源
     */
    private String source;

    /**
     * 元数据
     */
    private Map<String, Object> metadata;

    /**
     * 分块位置
     */
    private Integer chunkIndex;

    /**
     * 起始位置
     */
    private Integer startIndex;

    /**
     * 结束位置
     */
    private Integer endIndex;

    /**
     * 是否来自缓存
     */
    private Boolean fromCache;

    /**
     * 检索耗时
     */
    private Long latency;

    /**
     * 文本摘要
     */
    private String summary;

    /**
     * 从实体创建DTO
     */
    public static RetrieveResultDto fromEntity(RetrieveResult result) {
        if (result == null) {
            return null;
        }

        return RetrieveResultDto.builder()
                .docId(result.getDocId())
                .text(result.getText())
                .score(result.getScore())
                .source(result.getSource())
                .metadata(result.getMetadata())
                .chunkIndex(result.getChunkIndex())
                .startIndex(result.getStartIndex())
                .endIndex(result.getEndIndex())
                .fromCache(result.isFromCache())
                .latency(result.getLatency())
                .summary(result.getSummary())
                .build();
    }

    /**
     * 从实体列表创建DTO列表
     */
    public static List<RetrieveResultDto> fromEntityList(List<RetrieveResult> results) {
        if (results == null || results.isEmpty()) {
            return java.util.Collections.emptyList();
        }

        return results.stream()
                .map(RetrieveResultDto::fromEntity)
                .collect(java.util.stream.Collectors.toList());
    }

    /**
     * 判断是否有效
     */
    public boolean isValid() {
        return docId != null && !docId.isEmpty()
                && text != null && !text.trim().isEmpty();
    }

    /**
     * 获取高亮文本（简化版）
     */
    public String getHighlightText() {
        if (text == null || text.isEmpty()) {
            return "";
        }

        // 返回前200个字符
        return text.length() > 200 ? text.substring(0, 200) + "..." : text;
    }

    /**
     * 获取元数据值
     */
    @SuppressWarnings("unchecked")
    public <T> T getMetadataValue(String key, Class<T> type) {
        if (metadata == null) {
            return null;
        }

        Object value = metadata.get(key);
        if (value == null) {
            return null;
        }

        return (T) value;
    }
}
