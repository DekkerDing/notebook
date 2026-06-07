package io.github.dekkerding.examples.domain.retrieval.fusion;

import io.github.dekkerding.examples.domain.retrieval.model.RetrieveResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 检索结果融合服务
 *
 * <p>使用RRF（Reciprocal Rank Fusion）算法融合多个检索策略的结果。</p>
 *
 * <p>RRF算法：</p>
 * <pre>
 * score(doc) = Σ weight_i / (k + rank_i(doc))
 *
 * 其中：
 * - weight_i: 第i个策略的权重
 * - rank_i(doc): 文档在第i个策略中的排名（从1开始）
 * - k: 平滑参数（默认60），用于降低排名差异的影响
 * </pre>
 *
 * @author Notebook RAG Team
 * @since 1.0.0
 */
@Slf4j
@Service
public class ResultFusionService {

    /**
     * RRF平滑参数k
     */
    private static final int RRF_K = 60;

    /**
     * 融合多个策略的结果
     *
     * @param strategyResults 各策略的执行结果
     * @param weights 各策略的权重
     * @return 融合后的结果列表
     */
    public List<RetrieveResult> fuse(List<StrategyResult> strategyResults, Map<String, Double> weights) {
        Map<String, FusionScore> docScores = new HashMap<>();

        // 为每个策略的结果计算RRF分数
        for (StrategyResult sr : strategyResults) {
            if (!sr.isSuccess() || sr.getResults() == null || sr.getResults().isEmpty()) {
                continue;
            }

            double weight = weights.getOrDefault(sr.getStrategyName(), 1.0);

            // 按排名计算RRF分数
            for (int rank = 0; rank < sr.getResults().size(); rank++) {
                RetrieveResult result = sr.getResults().get(rank);
                String docId = result.getDocId();

                if (docId == null) {
                    continue;
                }

                FusionScore fs = docScores.computeIfAbsent(docId, k -> new FusionScore());
                fs.docId = docId;
                fs.originalResults.add(result);

                // 计算RRF分量: weight / (k + rank)
                // rank从0开始，所以是rank + 1
                double rrfScore = weight / (RRF_K + rank + 1);
                fs.totalScore += rrfScore;

                // 记录各策略的排名
                fs.strategyRanks.put(sr.getStrategyName(), rank + 1);
            }
        }

        // 转换为RetrieveResult列表
        List<RetrieveResult> fusedResults = new ArrayList<>();

        for (FusionScore fs : docScores.values()) {
            // 选择分数最高的原始结果作为基础
            RetrieveResult baseResult = selectBestResult(fs.originalResults);

            // 创建融合后的结果
            RetrieveResult fusedResult = RetrieveResult.builder()
                    .docId(fs.docId)
                    .text(baseResult.getText())
                    .score(fs.totalScore)
                    .source("multiway")
                    .metadata(buildFusionMetadata(fs, baseResult))
                    .build();

            fusedResults.add(fusedResult);
        }

        // 按融合分数排序
        fusedResults.sort((a, b) -> Double.compare(b.getScore(), a.getScore()));

        log.debug("融合完成: input={}, output={}",
                strategyResults.size(), fusedResults.size());

        return fusedResults;
    }

    /**
     * 选择最佳原始结果
     */
    private RetrieveResult selectBestResult(List<RetrieveResult> results) {
        return results.stream()
                .max(Comparator.comparingDouble(RetrieveResult::getScore))
                .orElse(results.get(0));
    }

    /**
     * 构建融合元数据
     */
    private Map<String, Object> buildFusionMetadata(FusionScore fs, RetrieveResult baseResult) {
        Map<String, Object> metadata = new HashMap<>();

        // 融合相关信息
        metadata.put("fusion_score", fs.totalScore);
        metadata.put("strategy_count", fs.strategyRanks.size());
        metadata.put("strategy_ranks", new HashMap<>(fs.strategyRanks));

        // 保留原始元数据
        if (baseResult.getMetadata() != null) {
            metadata.putAll(baseResult.getMetadata());
        }

        // 记录各策略的分数
        Map<String, Double> strategyScores = new HashMap<>();
        for (RetrieveResult r : fs.originalResults) {
            if (r.getSource() != null) {
                strategyScores.put(r.getSource(), r.getScore());
            }
        }
        metadata.put("strategy_scores", strategyScores);

        return metadata;
    }

    /**
     * 去重并融合结果
     *
     * @param results 多个策略的结果列表
     * @return 去重并融合后的结果
     */
    public List<RetrieveResult> deduplicateAndFuse(List<List<RetrieveResult>> results) {
        Map<String, RetrieveResult> resultMap = new LinkedHashMap<>();

        for (List<RetrieveResult> strategyResults : results) {
            for (RetrieveResult result : strategyResults) {
                String docId = result.getDocId();

                if (docId == null) {
                    continue;
                }

                RetrieveResult existing = resultMap.get(docId);

                if (existing == null || result.getScore() > existing.getScore()) {
                    resultMap.put(docId, result);
                }
            }
        }

        return new ArrayList<>(resultMap.values());
    }

    /**
     * 融合分数记录
     */
    @lombok.Data
    @lombok.AllArgsConstructor
    @lombok.NoArgsConstructor
    private static class FusionScore {
        String docId;
        double totalScore = 0.0;
        List<RetrieveResult> originalResults = new ArrayList<>();
        Map<String, Integer> strategyRanks = new HashMap<>();
    }
}
