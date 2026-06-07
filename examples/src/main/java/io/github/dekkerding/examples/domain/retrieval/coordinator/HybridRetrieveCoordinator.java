package io.github.dekkerding.examples.domain.retrieval.coordinator;

import io.github.dekkerding.examples.domain.retrieval.fusion.ResultFusionService;
import io.github.dekkerding.examples.domain.retrieval.model.RetrieveContext;
import io.github.dekkerding.examples.domain.retrieval.model.RetrieveRequest;
import io.github.dekkerding.examples.domain.retrieval.model.RetrieveResult;
import io.github.dekkerding.examples.domain.retrieval.strategy.RetrieveStrategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 混合检索协调器
 *
 * <p>负责协调多个检索策略的执行，提供高级检索功能。</p>
 *
 * <p>核心功能：</p>
 * <ul>
 *   <li>策略编排：根据配置选择合适的检索策略组合</li>
 *   <li>结果融合：智能融合多个策略的检索结果</li>
 *   <li>缓存管理：缓存常见查询结果</li>
 *   <li>性能监控：记录检索性能指标</li>
 * </ul>
 *
 * @author Notebook RAG Team
 * @since 1.0.0
 */
@Slf4j
@Service
public class HybridRetrieveCoordinator {

    @Autowired(required = false)
    private List<RetrieveStrategy> strategies;

    @Autowired
    private ResultFusionService fusionService;

    /**
     * 默认检索策略
     */
    private static final String DEFAULT_STRATEGY = "multiway";

    /**
     * 执行混合检索
     *
     * @param query 查询文本
     * @param request 检索请求参数
     * @return 检索结果
     */
    public List<RetrieveResult> retrieve(String query, RetrieveRequest request) {
        return retrieve(query, request, RetrieveContext.of(query));
    }

    /**
     * 执行混合检索（带上下文）
     *
     * @param query 查询文本
     * @param request 检索请求参数
     * @param context 检索上下文
     * @return 检索结果
     */
    public List<RetrieveResult> retrieve(String query, RetrieveRequest request, RetrieveContext context) {
        log.info("执行混合检索: query={}, strategy={}",
                query,
                request != null && request.getStrategy() != null ? request.getStrategy() : DEFAULT_STRATEGY);

        long startTime = System.currentTimeMillis();

        try {
            // 1. 确定使用的策略
            String strategyName = determineStrategy(request);
            RetrieveStrategy strategy = findStrategy(strategyName);

            if (strategy == null) {
                log.warn("未找到策略: {}, 使用默认策略", strategyName);
                strategy = findStrategy(DEFAULT_STRATEGY);
            }

            if (strategy == null) {
                log.error("没有可用的检索策略");
                return Collections.emptyList();
            }

            // 2. 执行检索
            List<RetrieveResult> results = strategy.retrieve(query, request, context);

            // 3. 后处理
            results = postProcess(results, request);

            long latency = System.currentTimeMillis() - startTime;
            log.info("混合检索完成: results={}, latency={}ms", results.size(), latency);

            // 4. 更新上下文
            context.markEnd();
            context.addResults(results);

            return results;

        } catch (Exception e) {
            log.error("混合检索失败: query={}", query, e);
            return Collections.emptyList();
        }
    }

    /**
     * 执行多策略检索并返回各策略结果
     *
     * @param query 查询文本
     * @param request 检索请求参数
     * @param strategyNames 要使用的策略名称列表
     * @return 各策略的结果
     */
    public Map<String, List<RetrieveResult>> retrieveMultiStrategy(
            String query,
            RetrieveRequest request,
            List<String> strategyNames) {

        Map<String, List<RetrieveResult>> results = new HashMap<>();
        RetrieveContext context = RetrieveContext.of(query, request);

        for (String strategyName : strategyNames) {
            RetrieveStrategy strategy = findStrategy(strategyName);

            if (strategy != null) {
                try {
                    List<RetrieveResult> strategyResults = strategy.retrieve(query, request, context);
                    results.put(strategyName, strategyResults);
                } catch (Exception e) {
                    log.error("策略 {} 执行失败", strategyName, e);
                    results.put(strategyName, Collections.emptyList());
                }
            } else {
                log.warn("未找到策略: {}", strategyName);
                results.put(strategyName, Collections.emptyList());
            }
        }

        return results;
    }

    /**
     * 确定使用的检索策略
     */
    private String determineStrategy(RetrieveRequest request) {
        if (request == null || request.getStrategy() == null) {
            return DEFAULT_STRATEGY;
        }
        return request.getStrategy();
    }

    /**
     * 查找指定策略
     */
    private RetrieveStrategy findStrategy(String strategyName) {
        if (strategies == null) {
            return null;
        }

        return strategies.stream()
                .filter(s -> strategyName.equals(s.getStrategyName()))
                .findFirst()
                .orElse(null);
    }

    /**
     * 结果后处理
     */
    private List<RetrieveResult> postProcess(List<RetrieveResult> results, RetrieveRequest request) {
        if (results == null || results.isEmpty()) {
            return results;
        }

        // 1. 过滤低分结果
        double minScore = (request != null) ? request.getMinScore() : 0.0;
        if (minScore > 0) {
            results = results.stream()
                    .filter(r -> r.getScore() >= minScore)
                    .collect(Collectors.toList());
        }

        // 2. 截取TopK
        int topK = (request != null) ? request.getTopK() : 10;
        if (results.size() > topK) {
            results = results.subList(0, topK);
        }

        return results;
    }

    /**
     * 获取所有可用策略
     */
    public List<String> getAvailableStrategies() {
        if (strategies == null) {
            return Collections.emptyList();
        }

        return strategies.stream()
                .map(RetrieveStrategy::getStrategyName)
                .collect(Collectors.toList());
    }

    /**
     * 获取策略描述
     */
    public Map<String, String> getStrategyDescriptions() {
        if (strategies == null) {
            return Collections.emptyMap();
        }

        Map<String, String> descriptions = new HashMap<>();

        for (RetrieveStrategy strategy : strategies) {
            descriptions.put(strategy.getStrategyName(), strategy.getDescription());
        }

        return descriptions;
    }
}
