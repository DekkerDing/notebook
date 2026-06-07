package io.github.dekkerding.examples.domain.retrieval.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * 检索结果
 *
 * @author Notebook RAG Team
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RetrieveResult {

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
    @Builder.Default
    private double score = 0.0;

    /**
     * 检索来源（如 "bm25", "vector", "hybrid" 等）
     */
    private String source;

    /**
     * 元数据
     */
    @Builder.Default
    private Map<String, Object> metadata = new java.util.HashMap<>();

    /**
     * 分块位置（如果来自分块文档）
     */
    @Builder.Default
    private int chunkIndex = -1;

    /**
     * 起始位置
     */
    @Builder.Default
    private int startIndex = 0;

    /**
     * 结束位置
     */
    @Builder.Default
    private int endIndex = 0;

    /**
     * 是否来自缓存
     */
    @Builder.Default
    private boolean fromCache = false;

    /**
     * 检索耗时（毫秒）
     */
    @Builder.Default
    private long latency = 0;

    /**
     * 创建简单的检索结果
     */
    public static RetrieveResult of(String docId, String text, double score) {
        return RetrieveResult.builder()
                .docId(docId)
                .text(text)
                .score(score)
                .build();
    }

    /**
     * 获取文本摘要（前100个字符）
     */
    public String getSummary() {
        if (text == null) return "";
        return text.length() > 100
                ? text.substring(0, 100) + "..."
                : text;
    }

    /**
     * 判断是否有效
     */
    public boolean isValid() {
        return docId != null && !docId.isEmpty()
                && text != null && !text.trim().isEmpty();
    }

    /**
     * 获取元数据值
     */
    @SuppressWarnings("unchecked")
    public <T> T getMetadataValue(String key, Class<T> type) {
        Object value = metadata.get(key);
        if (value == null) {
            return null;
        }
        return (T) value;
    }

    /**
     * 添加元数据
     */
    public void addMetadata(String key, Object value) {
        this.metadata.put(key, value);
    }

    /**
     * 克隆结果
     */
    public RetrieveResult copy() {
        RetrieveResult copy = RetrieveResult.builder()
                .docId(this.docId)
                .text(this.text)
                .score(this.score)
                .source(this.source)
                .chunkIndex(this.chunkIndex)
                .startIndex(this.startIndex)
                .endIndex(this.endIndex)
                .fromCache(this.fromCache)
                .latency(this.latency)
                .build();

        if (this.metadata != null) {
            copy.metadata = new java.util.HashMap<>(this.metadata);
        }

        return copy;
    }

    @Override
    public String toString() {
        return "RetrieveResult{" +
                "docId='" + docId + '\'' +
                ", score=" + String.format("%.4f", score) +
                ", source='" + source + '\'' +
                ", summary='" + getSummary() + '\'' +
                ", fromCache=" + fromCache +
                ", latency=" + latency +
                '}';
    }
}
