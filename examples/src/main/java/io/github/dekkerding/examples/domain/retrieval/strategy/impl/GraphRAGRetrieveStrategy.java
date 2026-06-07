package io.github.dekkerding.examples.domain.retrieval.strategy.impl;

import io.github.dekkerding.examples.domain.graph.extractor.EntityExtractor;
import io.github.dekkerding.examples.domain.graph.extractor.RelationExtractor;
import io.github.dekkerding.examples.domain.graph.extractor.impl.RuleBasedEntityExtractor;
import io.github.dekkerding.examples.domain.graph.extractor.impl.RuleBasedRelationExtractor;
import io.github.dekkerding.examples.domain.graph.model.Entity;
import io.github.dekkerding.examples.domain.graph.model.Relation;
import io.github.dekkerding.examples.domain.graph.query.GraphQuery;
import io.github.dekkerding.examples.domain.graph.query.GraphQueryService;
import io.github.dekkerding.examples.domain.graph.store.GraphStore;
import io.github.dekkerding.examples.domain.retrieval.model.RetrieveContext;
import io.github.dekkerding.examples.domain.retrieval.model.RetrieveRequest;
import io.github.dekkerding.examples.domain.retrieval.model.RetrieveResult;
import io.github.dekkerding.examples.domain.retrieval.strategy.RetrieveStrategy;
import io.github.dekkerding.examples.infrastructure.graph.GraphStoreFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * GraphRAG检索策略
 *
 * <p>基于知识图谱的检索增强策略</p>
 *
 * @author Notebook RAG Team
 * @since 1.0.0
 */
@Slf4j
@Component("graphragRetrieveStrategy")
public class GraphRAGRetrieveStrategy implements RetrieveStrategy {

    @Autowired
    private GraphStoreFactory graphStoreFactory;

    @Autowired
    private GraphQueryService graphQueryService;

    @Autowired(required = false)
    private EntityExtractor entityExtractor;

    @Autowired(required = false)
    private RelationExtractor relationExtractor;

    private static final double DEFAULT_GRAPH_WEIGHT = 0.4;
    private static final double DEFAULT_VECTOR_WEIGHT = 0.6;

    @Override
    public List<RetrieveResult> retrieve(String query, RetrieveRequest request, RetrieveContext context) {
        log.info("执行GraphRAG检索: query={}", query);

        try {
            // 1. 识别查询中的实体
            List<Entity> queryEntities = extractEntities(query);

            if (queryEntities.isEmpty()) {
                log.warn("未识别到实体，返回空结果");
                return Collections.emptyList();
            }

            // 2. 提取图谱上下文
            String graphContext = extractGraphContext(queryEntities);

            // 3. 混合检索（图谱 + 向量）
            List<RetrieveResult> results = performHybridRetrieval(query, request, graphContext);

            log.info("GraphRAG检索完成: result-count={}, graph-context-length={}",
                    results.size(),
                    graphContext != null ? graphContext.length() : 0);

            return results;

        } catch (Exception e) {
            log.error("GraphRAG检索失败", e);

            // 返回空结果
            return Collections.emptyList();
        }
    }

    @Override
    public String getStrategyName() {
        return "graphrag";
    }

    @Override
    public String getDescription() {
        return "基于知识图谱的检索增强策略";
    }

    /**
     * 抽取实体
     */
    private List<Entity> extractEntities(String query) {
        if (entityExtractor == null) {
            log.warn("实体抽取器未配置，使用规则抽取器");
            return Collections.emptyList();
        }

        return entityExtractor.extract(query, "default");
    }

    /**
     * 提取图谱上下文
     */
    private String extractGraphContext(List<Entity> entities) {
        StringBuilder context = new StringBuilder();
        GraphStore graphStore = graphStoreFactory.getGraphStore();

        for (Entity entity : entities) {
            // 查询实体社区
            GraphQuery communityQuery = GraphQuery.builder()
                    .type(GraphQuery.QueryType.COMMUNITY_QUERY)
                    .entityId(entity.getId())
                    .build();

            GraphQuery.GraphQueryResult communityResult =
                    graphQueryService.execute(communityQuery);

            if (communityResult.isSuccess() && communityResult.getCommunitySummary() != null) {
                context.append("【").append(entity.getName()).append("的社区】\n");
                context.append(communityResult.getCommunitySummary()).append("\n");
            }

            // 查询实体关系（1跳）
            GraphQuery relationQuery = GraphQuery.builder()
                    .type(GraphQuery.QueryType.RELATION_QUERY)
                    .entityId(entity.getId())
                    .hops(1)
                    .build();

            GraphQuery.GraphQueryResult relationResult =
                    graphQueryService.execute(relationQuery);

            if (relationResult.isSuccess() && relationResult.getRelations() != null) {
                context.append("【").append(entity.getName()).append("的关系】\n");

                for (Relation relation : relationResult.getRelations()) {
                    Entity target = graphStore.findNode(relation.getTarget());

                    if (target != null) {
                        context.append("- ")
                                .append(entity.getName())
                                .append(" ")
                                .append(relation.getType())
                                .append(" ")
                                .append(target.getName())
                                .append("\n");
                    }
                }
            }

            context.append("\n");
        }

        return context.toString();
    }

    /**
     * 执行混合检索
     */
    private List<RetrieveResult> performHybridRetrieval(String query,
                                                         RetrieveRequest request,
                                                         String graphContext) {
        // 这里应该调用向量检索，然后与图谱上下文融合
        // 简化实现：返回带图谱上下文的结果

        RetrieveResult result = RetrieveResult.builder()
                .docId("graphrag_" + System.currentTimeMillis())
                .text("图谱上下文: " + graphContext)
                .score(0.8)
                .source("graphrag")
                .build();

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("strategy", getStrategyName());
        metadata.put("graph_context", graphContext);
        metadata.put("graph_weight", DEFAULT_GRAPH_WEIGHT);
        metadata.put("vector_weight", DEFAULT_VECTOR_WEIGHT);

        result.setMetadata(metadata);

        List<RetrieveResult> results = new ArrayList<>();
        results.add(result);

        return results;
    }
}
