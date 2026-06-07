package io.github.dekkerding.examples.domain.embedding.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 向量化请求参数
 *
 * @author Notebook RAG Team
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmbeddingRequest {

    /**
     * 模型名称
     * <p>如: text-embedding-ada-002, text-embedding-v3</p>
     */
    @Builder.Default
    private String model = "text-embedding-ada-002";

    /**
     * 向量维度
     * <p>0表示使用模型默认维度</p>
     */
    @Builder.Default
    private int dimensions = 0;

    /**
     * 是否使用缓存
     */
    @Builder.Default
    private boolean useCache = true;

    /**
     * 缓存TTL（秒）
     */
    @Builder.Default
    private long cacheTTL = 86400; // 24小时

    /**
     * 编码格式
     */
    @Builder.Default
    private EncodingFormat encodingFormat = EncodingFormat.FLOAT;

    /**
     * 批处理大小
     */
    @Builder.Default
    private int batchSize = 100;

    /**
     * 是否重试
     */
    @Builder.Default
    private boolean retry = true;

    /**
     * 重试次数
     */
    @Builder.Default
    private int maxRetries = 3;

    /**
     * 超时时间（毫秒）
     */
    @Builder.Default
    private long timeout = 30000;

    /**
     * 用户自定义前缀（用于多租户隔离）
     */
    private String userPrefix;

    /**
     * 编码格式枚举
     */
    public enum EncodingFormat {
        /**
         * 浮点数（默认）
         */
        FLOAT,
        /**
         * Base64编码
         */
        BASE64
    }

    /**
     * 获取默认配置
     */
    public static EmbeddingRequest defaultConfig() {
        return EmbeddingRequest.builder().build();
    }

    /**
     * OpenAI配置
     */
    public static EmbeddingRequest openAIConfig() {
        return EmbeddingRequest.builder()
                .model("text-embedding-ada-002")
                .dimensions(1536)
                .build();
    }

    /**
     * 通义千问配置
     */
    public static EmbeddingRequest qwenConfig() {
        return EmbeddingRequest.builder()
                .model("text-embedding-v3")
                .dimensions(1024)
                .build();
    }

    /**
     * 本地模型配置
     */
    public static EmbeddingRequest localConfig() {
        return EmbeddingRequest.builder()
                .model("local-bge-large-zh")
                .dimensions(1024)
                .batchSize(32) // 本地模型批量处理能力有限
                .build();
    }
}
