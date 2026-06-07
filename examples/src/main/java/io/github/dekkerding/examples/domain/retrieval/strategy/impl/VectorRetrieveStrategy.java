package io.github.dekkerding.examples.domain.retrieval.strategy.impl;

import io.github.dekkerding.examples.domain.retrieval.model.RetrieveContext;
import io.github.dekkerding.examples.domain.retrieval.model.RetrieveRequest;
import io.github.dekkerding.examples.domain.retrieval.model.RetrieveResult;
import io.github.dekkerding.examples.domain.retrieval.strategy.RetrieveStrategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 向量检索策略 - 基于向量相似度的密集检索
 *
 * <p>向量检索（或称密集检索）通过将文本映射到高维向量空间，利用向量相似度来检索相关文档。</p>
 *
 * <p>核心概念：</p>
 * <ul>
 *   <li><b>向量表示</b>：将文本转换为固定维度的数值向量</li>
 *   <li><b>相似度计算</b>：通常使用余弦相似度或欧氏距离</li>
 *   <li><b>向量索引</b>：使用HNSW、IVF等加速检索</li>
 * </ul>
 *
 * <p>相似度计算方法：</p>
 * <pre>
 * 1. 余弦相似度（推荐）：
 *    similarity = (A · B) / (||A|| × ||B||)
 *    范围：[-1, 1]，值越大越相似
 *
 * 2. 欧氏距离：
 *    distance = sqrt(Σ(Ai - Bi)²)
 *    范围：[0, +∞]，值越小越相似
 *
 * 3. 点积：
 *    similarity = A · B
 *    范围：(-∞, +∞)，常用于归一化向量
 * </pre>
 *
 * <p>优势：</p>
 * <ul>
 *   <li>能捕捉语义相似性，不依赖精确匹配</li>
 *   <li>对同义词、改写等有良好支持</li>
 *   <li>适合跨语言检索</li>
 * </ul>
 *
 * <p>劣势：</p>
 * <ul>
 *   <li>计算复杂度高</li>
 *   <li>需要向量索引加速</li>
 *   <li>对专有名词识别较弱</li>
 * </ul>
 *
 * @author Notebook RAG Team
 * @since 1.0.0
 */
@Slf4j
@Component
public class VectorRetrieveStrategy implements RetrieveStrategy {

    /**
     * 默认向量维度
     */
    private static final int DEFAULT_DIMENSION = 1536;

    /**
     * 默认相似度阈值
     */
    private static final double DEFAULT_SIMILARITY_THRESHOLD = 0.7;

    /**
     * 向量维度
     */
    private int dimension = DEFAULT_DIMENSION;

    /**
     * 相似度阈值
     */
    private double similarityThreshold = DEFAULT_SIMILARITY_THRESHOLD;

    /**
     * 相似度计算方法
     */
    private SimilarityMethod similarityMethod = SimilarityMethod.COSINE;

    /**
     * 向量索引（文档ID → 向量）
     */
    private final Map<String, float[]> vectorIndex = new HashMap<>();

    /**
     * 文档内容索引（文档ID → 内容）
     */
    private final Map<String, String> contentIndex = new HashMap<>();

    /**
     * 向量统计信息
     */
    private VectorStats vectorStats = new VectorStats();

    @Override
    public List<RetrieveResult> retrieve(String query, RetrieveRequest request, RetrieveContext context) {
        log.debug("向量检索: query={}, dimension={}, method={}",
                query, dimension, similarityMethod);

        long startTime = System.currentTimeMillis();

        // 1. 获取查询向量
        float[] queryVector;
        if (context != null && context.getQueryVector() != null) {
            queryVector = context.getQueryVector();
        } else {
            // 模拟向量化（实际应该调用EmbeddingService）
            queryVector = simulateEmbedding(query);
        }

        if (queryVector == null || queryVector.length == 0) {
            log.warn("查询向量化失败");
            return Collections.emptyList();
        }

        // 2. 检查向量维度
        if (queryVector.length != dimension) {
            log.warn("查询向量维度不匹配: expected={}, actual={}",
                    dimension, queryVector.length);
            return Collections.emptyList();
        }

        // 3. 计算相似度
        List<VectorResult> vectorResults = new ArrayList<>();

        for (Map.Entry<String, float[]> entry : vectorIndex.entrySet()) {
            String docId = entry.getKey();
            float[] docVector = entry.getValue();

            double similarity = calculateSimilarity(queryVector, docVector);

            if (similarity >= similarityThreshold) {
                vectorResults.add(new VectorResult(docId, similarity));
            }
        }

        // 4. 排序（按相似度降序）
        vectorResults.sort((a, b) -> Double.compare(b.similarity, a.similarity));

        // 5. 转换为检索结果
        int topK = request != null ? request.getTopK() : 10;
        List<RetrieveResult> results = new ArrayList<>();

        for (int i = 0; i < Math.min(topK, vectorResults.size()); i++) {
            VectorResult vr = vectorResults.get(i);
            String content = contentIndex.get(vr.docId);

            RetrieveResult result = RetrieveResult.builder()
                    .docId(vr.docId)
                    .text(content)
                    .score(vr.similarity)
                    .source("vector")
                    .metadata(buildMetadata(vr))
                    .latency(System.currentTimeMillis() - startTime)
                    .build();

            results.add(result);
        }

        // 6. 更新统计信息
        vectorStats.totalRetrieves++;
        vectorStats.totalResults += results.size();
        vectorStats.avgSimilarity = (vectorStats.avgSimilarity * (vectorStats.totalRetrieves - 1)
                + getAverageSimilarity(results)) / vectorStats.totalRetrieves;

        log.debug("向量检索完成: results={}, 耗时={}ms",
                results.size(), System.currentTimeMillis() - startTime);

        return results;
    }

    @Override
    public String getStrategyName() {
        return "vector";
    }

    @Override
    public String getDescription() {
        return "向量密集检索，基于向量相似度进行语义检索";
    }

    /**
     * 计算相似度
     */
    private double calculateSimilarity(float[] vector1, float[] vector2) {
        if (vector1.length != vector2.length) {
            throw new IllegalArgumentException("Vector dimensions must match");
        }

        switch (similarityMethod) {
            case COSINE:
                return cosineSimilarity(vector1, vector2);
            case EUCLIDEAN:
                // 将距离转换为相似度
                double distance = euclideanDistance(vector1, vector2);
                return 1.0 / (1.0 + distance); // 距离越小，相似度越高
            case DOT_PRODUCT:
                return dotProduct(vector1, vector2);
            default:
                return cosineSimilarity(vector1, vector2);
        }
    }

    /**
     * 余弦相似度
     * similarity = (A · B) / (||A|| × ||B||)
     */
    private double cosineSimilarity(float[] vector1, float[] vector2) {
        double dotProduct = 0.0;
        double norm1 = 0.0;
        double norm2 = 0.0;

        for (int i = 0; i < vector1.length; i++) {
            dotProduct += vector1[i] * vector2[i];
            norm1 += vector1[i] * vector1[i];
            norm2 += vector2[i] * vector2[i];
        }

        return dotProduct / (Math.sqrt(norm1) * Math.sqrt(norm2));
    }

    /**
     * 欧氏距离
     * distance = sqrt(Σ(Ai - Bi)²)
     */
    private double euclideanDistance(float[] vector1, float[] vector2) {
        double sum = 0.0;
        for (int i = 0; i < vector1.length; i++) {
            double diff = vector1[i] - vector2[i];
            sum += diff * diff;
        }
        return Math.sqrt(sum);
    }

    /**
     * 点积
     */
    private double dotProduct(float[] vector1, float[] vector2) {
        double sum = 0.0;
        for (int i = 0; i < vector1.length; i++) {
            sum += vector1[i] * vector2[i];
        }
        return sum;
    }

    /**
     * 模拟向量化（实际应该调用EmbeddingService）
     */
    private float[] simulateEmbedding(String text) {
        float[] vector = new float[dimension];

        // 简单的哈希模拟（实际应该使用真正的embedding模型）
        for (int i = 0; i < dimension; i++) {
            vector[i] = (float) Math.random();
        }

        // 归一化
        float norm = 0.0f;
        for (float v : vector) {
            norm += v * v;
        }
        norm = (float) Math.sqrt(norm);

        if (norm > 0) {
            for (int i = 0; i < dimension; i++) {
                vector[i] /= norm;
            }
        }

        return vector;
    }

    /**
     * 计算平均相似度
     */
    private double getAverageSimilarity(List<RetrieveResult> results) {
        if (results.isEmpty()) {
            return 0.0;
        }

        return results.stream()
                .mapToDouble(RetrieveResult::getScore)
                .average()
                .orElse(0.0);
    }

    /**
     * 构建元数据
     */
    private Map<String, Object> buildMetadata(VectorResult vr) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("similarity_method", similarityMethod.name());
        metadata.put("vector_dimension", dimension);
        return metadata;
    }

    // ===== 索引管理方法 =====

    /**
     * 添加文档向量到索引
     * @param docId 文档ID
     * @param content 文档内容
     * @param vector 文档向量
     */
    public void indexDocument(String docId, String content, float[] vector) {
        if (docId == null || vector == null) {
            return;
        }

        // 检查向量维度
        if (vector.length != dimension) {
            log.warn("向量维度不匹配: expected={}, actual={}", dimension, vector.length);
            return;
        }

        // 添加到索引
        vectorIndex.put(docId, vector);
        contentIndex.put(docId, content);

        // 更新统计
        vectorStats.totalDocuments++;
        vectorStats.totalVectors++;

        log.debug("索引文档向量: docId={}, dimension={}", docId, vector.length);
    }

    /**
     * 添加文档（自动向量化）
     */
    public void indexDocument(String docId, String content) {
        float[] vector = simulateEmbedding(content);
        indexDocument(docId, content, vector);
    }

    /**
     * 批量索引
     */
    public void indexBatch(Map<String, String> documents) {
        for (Map.Entry<String, String> entry : documents.entrySet()) {
            indexDocument(entry.getKey(), entry.getValue());
        }
        log.info("批量索引完成: totalDocs={}", vectorIndex.size());
    }

    /**
     * 清空索引
     */
    public void clearIndex() {
        vectorIndex.clear();
        contentIndex.clear();
        vectorStats = new VectorStats();
        log.info("向量索引已清空");
    }

    /**
     * 获取索引统计信息
     */
    public VectorStats getStats() {
        vectorStats.totalDocuments = vectorIndex.size();
        return vectorStats;
    }

    /**
     * 设置向量维度
     */
    public void setDimension(int dimension) {
        if (dimension <= 0) {
            throw new IllegalArgumentException("Dimension must be positive");
        }

        // 如果索引不为空且维度不同，需要清空索引
        if (!vectorIndex.isEmpty() && this.dimension != dimension) {
            log.warn("维度变化，清空现有索引: old={}, new={}", this.dimension, dimension);
            clearIndex();
        }

        this.dimension = dimension;
    }

    /**
     * 设置相似度阈值
     */
    public void setSimilarityThreshold(double threshold) {
        if (threshold < 0 || threshold > 1) {
            throw new IllegalArgumentException("Threshold must be between 0 and 1");
        }
        this.similarityThreshold = threshold;
    }

    /**
     * 设置相似度计算方法
     */
    public void setSimilarityMethod(SimilarityMethod method) {
        this.similarityMethod = method;
        log.info("相似度计算方法已更新: {}", method);
    }

    // ===== 内部数据结构 =====

    /**
     * 向量检索结果
     */
    private static class VectorResult {
        String docId;
        double similarity;

        VectorResult(String docId, double similarity) {
            this.docId = docId;
            this.similarity = similarity;
        }
    }

    /**
     * 相似度计算方法
     */
    public enum SimilarityMethod {
        /**
         * 余弦相似度（推荐）
         */
        COSINE,
        /**
         * 欧氏距离
         */
        EUCLIDEAN,
        /**
         * 点积
         */
        DOT_PRODUCT
    }

    /**
     * 向量统计信息
     */
    @lombok.Data
    public static class VectorStats {
        private int totalDocuments = 0;
        private long totalVectors = 0;
        private long totalRetrieves = 0;
        private long totalResults = 0;
        private double avgSimilarity = 0.0;

        @Override
        public String toString() {
            return "VectorStats{" +
                    "totalDocuments=" + totalDocuments +
                    ", totalRetrieves=" + totalRetrieves +
                    ", avgResultsPerRetrieve=" +
                        (totalRetrieves > 0 ? String.format("%.2f", (double) totalResults / totalRetrieves) : "N/A") +
                    ", avgSimilarity=" + String.format("%.4f", avgSimilarity) +
                    '}';
        }
    }
}
