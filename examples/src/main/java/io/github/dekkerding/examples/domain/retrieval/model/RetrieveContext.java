package io.github.dekkerding.examples.domain.retrieval.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.*;

/**
 * 检索上下文 - 包含检索过程中的所有上下文信息
 *
 * <p>功能：</p>
 * <ul>
 *   <li>存储检索查询和参数</li>
 *   <li>记录检索过程信息</li>
 *   <li>追踪检索来源</li>
 *   <li>支持检索结果合并</li>
 * </ul>
 *
 * @author Notebook RAG Team
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RetrieveContext {

    /**
     * 查询文本
     */
    private String query;

    /**
     * 查询向量（用于向量检索）
     */
    private float[] queryVector;

    /**
     * 检索请求参数
     */
    private RetrieveRequest request;

    /**
     * 检索结果列表
     */
    @Builder.Default
    private List<RetrieveResult> results = new ArrayList<>();

    /**
     * 检索开始时间
     */
    @Builder.Default
    private long startTime = System.currentTimeMillis();

    /**
     * 检索结束时间
     */
    @Builder.Default
    private long endTime = 0;

    /**
     * 用户ID
     */
    private String userId;

    /**
     * 会话ID（用于多轮对话）
     */
    private String sessionId;

    /**
     * 额外上下文信息
     */
    @Builder.Default
    private Map<String, Object> extra = new HashMap<>();

    /**
     * 创建简单的检索上下文
     */
    public static RetrieveContext of(String query) {
        return RetrieveContext.builder()
                .query(query)
                .request(RetrieveRequest.defaultConfig())
                .build();
    }

    /**
     * 创建带请求参数的检索上下文
     */
    public static RetrieveContext of(String query, RetrieveRequest request) {
        return RetrieveContext.builder()
                .query(query)
                .request(request)
                .build();
    }

    /**
     * 获取检索耗时（毫秒）
     */
    public long getDuration() {
        if (endTime == 0) {
            endTime = System.currentTimeMillis();
        }
        return endTime - startTime;
    }

    /**
     * 添加检索结果
     */
    public void addResult(RetrieveResult result) {
        if (results == null) {
            results = new ArrayList<>();
        }
        results.add(result);
    }

    /**
     * 添加检索结果列表
     */
    public void addResults(List<RetrieveResult> newResults) {
        if (results == null) {
            results = new ArrayList<>();
        }
        if (newResults != null) {
            results.addAll(newResults);
        }
    }

    /**
     * 合并检索结果（去重）
     */
    public void mergeResults(List<RetrieveResult> newResults) {
        if (results == null) {
            results = new ArrayList<>();
        }

        if (newResults == null || newResults.isEmpty()) {
            return;
        }

        // 按docId去重，保留分数高的
        Map<String, RetrieveResult> resultMap = new LinkedHashMap<>();

        // 先添加现有结果
        for (RetrieveResult result : results) {
            if (result.getDocId() != null) {
                resultMap.put(result.getDocId(), result);
            }
        }

        // 合并新结果
        for (RetrieveResult result : newResults) {
            if (result.getDocId() == null) {
                results.add(result);
                continue;
            }

            RetrieveResult existing = resultMap.get(result.getDocId());
            if (existing == null || result.getScore() > existing.getScore()) {
                resultMap.put(result.getDocId(), result);
            }
        }

        results = new ArrayList<>(resultMap.values());
    }

    /**
     * 按分数排序
     */
    public void sortByScore() {
        if (results == null) {
            return;
        }
        results.sort((a, b) -> Double.compare(b.getScore(), a.getScore()));
    }

    /**
     * 截取TopK结果
     */
    public void truncate(int topK) {
        if (results == null || results.size() <= topK) {
            return;
        }
        results = new ArrayList<>(results.subList(0, topK));
    }

    /**
     * 过滤低分结果
     */
    public void filterByMinScore(double minScore) {
        if (results == null) {
            return;
        }
        results.removeIf(result -> result.getScore() < minScore);
    }

    /**
     * 获取检索统计信息
     */
    public RetrieveStatistics getStatistics() {
        RetrieveStatistics stats = new RetrieveStatistics();

        stats.totalResults = results != null ? results.size() : 0;
        stats.duration = getDuration();

        if (results != null && !results.isEmpty()) {
            DoubleSummaryStatistics scoreStats = results.stream()
                    .mapToDouble(RetrieveResult::getScore)
                    .summaryStatistics();

            stats.maxScore = scoreStats.getMax();
            stats.minScore = scoreStats.getMin();
            stats.avgScore = scoreStats.getAverage();

            // 来源分布
            Map<String, Long> sourceDistribution = new HashMap<>();
            for (RetrieveResult result : results) {
                String source = result.getSource() != null ? result.getSource() : "unknown";
                sourceDistribution.put(source, sourceDistribution.getOrDefault(source, 0L) + 1);
            }
            stats.sourceDistribution = sourceDistribution;
        }

        return stats;
    }

    /**
     * 标记检索结束
     */
    public void markEnd() {
        this.endTime = System.currentTimeMillis();
    }

    /**
     * 检索统计信息
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RetrieveStatistics {
        public int totalResults;
        public long duration;
        public double maxScore;
        public double minScore;
        public double avgScore;
        public Map<String, Long> sourceDistribution;

        @Override
        public String toString() {
            return "RetrieveStatistics{" +
                    "totalResults=" + totalResults +
                    ", duration=" + duration +
                    ", avgScore=" + String.format("%.4f", avgScore) +
                    ", sourceDistribution=" + sourceDistribution +
                    '}';
        }
    }
}
