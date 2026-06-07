package io.github.dekkerding.examples.domain.embedding.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 向量化结果
 *
 * @author Notebook RAG Team
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmbeddingResult {

    /**
     * 输入文本
     */
    private String input;

    /**
     * 向量数据
     */
    private List<Float> embedding;

    /**
     * 使用的模型
     */
    private String model;

    /**
     * 向量维度
     */
    private int dimensions;

    /**
     * Token消耗量
     */
    @Builder.Default
    private int tokens = 0;

    /**
     * 是否来自缓存
     */
    @Builder.Default
    private boolean fromCache = false;

    /**
     * 处理耗时（毫秒）
     */
    @Builder.Default
    private long duration = 0;

    /**
     * 错误信息（如果失败）
     */
    private String error;

    /**
     * 创建失败结果
     */
    public static EmbeddingResult failure(String input, String error) {
        return EmbeddingResult.builder()
                .input(input)
                .error(error)
                .build();
    }

    /**
     * 判断是否成功
     */
    public boolean isSuccess() {
        return error == null && embedding != null && !embedding.isEmpty();
    }

    /**
     * 获取向量大小
     */
    public int size() {
        return embedding != null ? embedding.size() : 0;
    }

    /**
     * 计算与另一个向量的余弦相似度
     */
    public double cosineSimilarity(EmbeddingResult other) {
        if (this.embedding == null || other.embedding == null) {
            return 0.0;
        }

        if (this.embedding.size() != other.embedding.size()) {
            throw new IllegalArgumentException("Vector dimensions must match");
        }

        double dotProduct = 0.0;
        double norm1 = 0.0;
        double norm2 = 0.0;

        for (int i = 0; i < this.embedding.size(); i++) {
            dotProduct += this.embedding.get(i) * other.embedding.get(i);
            norm1 += this.embedding.get(i) * this.embedding.get(i);
            norm2 += other.embedding.get(i) * other.embedding.get(i);
        }

        return dotProduct / (Math.sqrt(norm1) * Math.sqrt(norm2));
    }

    /**
     * 计算与另一个向量的欧氏距离
     */
    public double euclideanDistance(EmbeddingResult other) {
        if (this.embedding == null || other.embedding == null) {
            return Double.MAX_VALUE;
        }

        if (this.embedding.size() != other.embedding.size()) {
            throw new IllegalArgumentException("Vector dimensions must match");
        }

        double sum = 0.0;
        for (int i = 0; i < this.embedding.size(); i++) {
            double diff = this.embedding.get(i) - other.embedding.get(i);
            sum += diff * diff;
        }

        return Math.sqrt(sum);
    }

    /**
     * 转为数组
     */
    public float[] toArray() {
        if (embedding == null) {
            return new float[0];
        }

        float[] array = new float[embedding.size()];
        for (int i = 0; i < embedding.size(); i++) {
            array[i] = embedding.get(i);
        }
        return array;
    }

    @Override
    public String toString() {
        return "EmbeddingResult{" +
                "input='" + (input != null ? input.substring(0, Math.min(20, input.length())) + "..." : "null") + '\'' +
                ", dimensions=" + dimensions +
                ", tokens=" + tokens +
                ", fromCache=" + fromCache +
                ", duration=" + duration +
                ", success=" + isSuccess() +
                '}';
    }
}
