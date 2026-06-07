package io.github.dekkerding.examples.domain.retrieval.strategy.impl;

import io.github.dekkerding.examples.domain.retrieval.model.RetrieveContext;
import io.github.dekkerding.examples.domain.retrieval.model.RetrieveRequest;
import io.github.dekkerding.examples.domain.retrieval.model.RetrieveResult;
import io.github.dekkerding.examples.domain.retrieval.strategy.RetrieveStrategy;
import io.github.dekkerding.examples.domain.retrieval.fusion.ResultFusionService;
import io.github.dekkerding.examples.domain.retrieval.fusion.StrategyResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

/**
 * 多路融合检索策略
 *
 * <p>同时执行多个检索策略并智能融合结果，综合利用各种检索方式的优势。</p>
 *
 * <p>核心特点：</p>
 * <ul>
 *   <li>并行执行：多种策略同时执行，降低总体延迟</li>
 *   <li>智能融合：使用RRF（Reciprocal Rank Fusion）算法融合结果</li>
 *   <li>自适应权重：根据查询特征动态调整各策略权重</li>
 *   <li>去重优化：自动去除重复文档，保留最高分结果</li>
 * </ul>
 *
 * <p>融合算法：</p>
 * <pre>
 * score(doc) = Σ weight_i / (k + rank_i(doc))
 *
 * 其中：
 * - weight_i: 第i个策略的权重
 * - rank_i(doc): 文档在第i个策略中的排名
 * - k: 平滑参数（默认60）
 * </pre>
 *
 * @author Notebook RAG Team
 * @since 1.0.0
 */
@Slf4j
@Component("multiWayRetrieveStrategy")
public class MultiWayRetrieveStrategy implements RetrieveStrategy {

    @Autowired
    @Lazy
    private List<RetrieveStrategy> allStrategies;

    @Autowired
    private ResultFusionService fusionService;

    private ExecutorService executorService;

    private static final int DEFAULT_THREADS = 4;

    /**
     * 默认策略权重（平衡配置）
     */
    private static final Map<String, Double> DEFAULT_WEIGHTS = new HashMap<>();
    static {
        DEFAULT_WEIGHTS.put("bm25", 0.4);
        DEFAULT_WEIGHTS.put("vector", 0.4);
        DEFAULT_WEIGHTS.put("graphrag", 0.2);
    }

    @Override
    public List<RetrieveResult> retrieve(String query, RetrieveRequest request, RetrieveContext context) {
        log.info("执行多路融合检索: query={}", query);

        long startTime = System.currentTimeMillis();

        // 1. 确定要使用的策略（排除自己，避免循环）
        List<RetrieveStrategy> strategies = getActiveStrategies();

        if (strategies.isEmpty()) {
            log.warn("没有可用的检索策略");
            return Collections.emptyList();
        }

        log.debug("使用策略: {}",
                strategies.stream()
                        .map(RetrieveStrategy::getStrategyName)
                        .collect(Collectors.joining(", ")));

        // 2. 并行执行所有策略
        List<CompletableFuture<StrategyResult>> futures = strategies.stream()
                .map(strategy -> executeStrategyAsync(strategy, query, request, context))
                .collect(Collectors.toList());

        // 3. 等待所有策略完成
        List<StrategyResult> strategyResults = futures.stream()
                .map(CompletableFuture::join)
                .collect(Collectors.toList());

        log.debug("所有策略执行完成: count={}, 总耗时={}ms",
                strategyResults.size(),
                System.currentTimeMillis() - startTime);

        // 4. 融合结果
        Map<String, Double> weights = determineWeights(query);
        List<RetrieveResult> fusedResults = fusionService.fuse(strategyResults, weights);

        // 5. 按分数排序
        fusedResults.sort((a, b) -> Double.compare(b.getScore(), a.getScore()));

        // 6. 截取TopK
        int topK = request != null ? request.getTopK() : 10;
        if (fusedResults.size() > topK) {
            fusedResults = fusedResults.subList(0, topK);
        }

        log.info("多路融合检索完成: results={}, 耗时={}ms",
                fusedResults.size(),
                System.currentTimeMillis() - startTime);

        return fusedResults;
    }

    @Override
    public String getStrategyName() {
        return "multiway";
    }

    @Override
    public String getDescription() {
        return "多路融合检索，并行执行多种检索策略并智能融合结果";
    }

    /**
     * 获取活动的检索策略（排除自己）
     */
    private List<RetrieveStrategy> getActiveStrategies() {
        if (allStrategies == null) {
            return Collections.emptyList();
        }

        return allStrategies.stream()
                .filter(s -> !s.getStrategyName().equals("multiway"))
                .filter(s -> !s.getStrategyName().equals("hybrid"))
                .collect(Collectors.toList());
    }

    /**
     * 异步执行单个检索策略
     */
    private CompletableFuture<StrategyResult> executeStrategyAsync(
            RetrieveStrategy strategy,
            String query,
            RetrieveRequest request,
            RetrieveContext context) {

        return CompletableFuture.supplyAsync(() -> {
            long startTime = System.currentTimeMillis();

            try {
                List<RetrieveResult> results = strategy.retrieve(query, request, context);

                long latency = System.currentTimeMillis() - startTime;
                log.debug("策略 {} 完成: results={}, latency={}ms",
                        strategy.getStrategyName(), results.size(), latency);

                return StrategyResult.builder()
                        .strategyName(strategy.getStrategyName())
                        .results(results)
                        .latency(latency)
                        .success(true)
                        .build();

            } catch (Exception e) {
                log.error("策略 {} 执行失败", strategy.getStrategyName(), e);

                long latency = System.currentTimeMillis() - startTime;
                return StrategyResult.builder()
                        .strategyName(strategy.getStrategyName())
                        .results(Collections.emptyList())
                        .latency(latency)
                        .success(false)
                        .errorMessage(e.getMessage())
                        .build();
            }
        }, getExecutor());
    }

    /**
     * 根据查询特征确定策略权重
     */
    private Map<String, Double> determineWeights(String query) {
        // 关键词特征：包含数字、专有名词（大写字母）
        if (isKeywordQuery(query)) {
            Map<String, Double> weights = new HashMap<>(DEFAULT_WEIGHTS);
            weights.put("bm25", 0.6);
            weights.put("vector", 0.3);
            weights.put("graphrag", 0.1);
            log.debug("检测到关键词查询，使用关键词权重");
            return weights;
        }

        // 关系特征：包含关系疑问词
        if (isRelationQuery(query)) {
            Map<String, Double> weights = new HashMap<>(DEFAULT_WEIGHTS);
            weights.put("bm25", 0.1);
            weights.put("vector", 0.3);
            weights.put("graphrag", 0.6);
            log.debug("检测到关系查询，使用关系权重");
            return weights;
        }

        // 默认平衡权重
        log.debug("使用默认平衡权重");
        return new HashMap<>(DEFAULT_WEIGHTS);
    }

    /**
     * 判断是否为关键词查询
     */
    private boolean isKeywordQuery(String query) {
        // 包含数字
        if (query.matches(".*\\d+.*")) {
            return true;
        }

        // 包含大写字母（可能是专有名词）
        if (query.matches(".*[A-Z].*")) {
            return true;
        }

        // 较短的查询（< 10字符）
        return query.length() < 10;
    }

    /**
     * 判断是否为关系查询
     */
    private boolean isRelationQuery(String query) {
        String[] relationWords = {"谁", "哪里", "什么关系", "关系", "属于", "包含", "连接"};
        for (String word : relationWords) {
            if (query.contains(word)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 获取线程池
     */
    private synchronized ExecutorService getExecutor() {
        if (executorService == null || executorService.isShutdown()) {
            executorService = Executors.newFixedThreadPool(DEFAULT_THREADS);
        }
        return executorService;
    }
}
