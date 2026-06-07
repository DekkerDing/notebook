package io.github.dekkerding.examples.domain.graph.query;

import io.github.dekkerding.examples.domain.graph.model.Entity;
import io.github.dekkerding.examples.domain.graph.model.Relation;
import io.github.dekkerding.examples.domain.graph.store.GraphStore;
import io.github.dekkerding.examples.infrastructure.graph.GraphStoreFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 图查询服务
 *
 * @author Notebook RAG Team
 * @since 1.0.0
 */
@Slf4j
@Service
public class GraphQueryService {

    @Autowired
    private GraphStoreFactory graphStoreFactory;

    /**
     * 执行图查询
     *
     * @param query 查询对象
     * @return 查询结果
     */
    public GraphQuery.GraphQueryResult execute(GraphQuery query) {
        GraphStore graphStore = graphStoreFactory.getGraphStore();

        try {
            GraphQuery.GraphQueryResult result = GraphQuery.GraphQueryResult.builder()
                    .success(true)
                    .build();

            switch (query.getType()) {
                case NODE_QUERY:
                    return handleNodeQuery(graphStore, query, result);

                case RELATION_QUERY:
                    return handleRelationQuery(graphStore, query, result);

                case PATH_QUERY:
                    return handlePathQuery(graphStore, query, result);

                case COMMUNITY_QUERY:
                    return handleCommunityQuery(graphStore, query, result);

                case PATTERN_QUERY:
                    return handlePatternQuery(graphStore, query, result);

                default:
                    result.setSuccess(false);
                    result.setErrorMessage("不支持的查询类型: " + query.getType());
                    return result;
            }

        } catch (Exception e) {
            log.error("图查询执行失败: type={}", query.getType(), e);

            return GraphQuery.GraphQueryResult.builder()
                    .success(false)
                    .errorMessage(e.getMessage())
                    .build();
        }
    }

    /**
     * 处理节点查询
     */
    private GraphQuery.GraphQueryResult handleNodeQuery(GraphStore graphStore,
                                                         GraphQuery query,
                                                         GraphQuery.GraphQueryResult result) {
        String entityId = query.getEntityId();

        if (entityId == null || entityId.isEmpty()) {
            result.setSuccess(false);
            result.setErrorMessage("节点ID不能为空");
            return result;
        }

        Entity entity = graphStore.findNode(entityId);

        if (entity != null) {
            List<Entity> entities = new ArrayList<>();
            entities.add(entity);
            result.setEntities(entities);
        }

        return result;
    }

    /**
     * 处理关系查询
     */
    private GraphQuery.GraphQueryResult handleRelationQuery(GraphStore graphStore,
                                                            GraphQuery query,
                                                            GraphQuery.GraphQueryResult result) {
        String entityId = query.getEntityId();
        int hops = query.getHops() != null ? query.getHops() : 1;

        if (entityId == null || entityId.isEmpty()) {
            result.setSuccess(false);
            result.setErrorMessage("节点ID不能为空");
            return result;
        }

        List<Relation> relations = graphStore.findRelations(entityId, hops);

        if (query.getRelationTypes() != null && !query.getRelationTypes().isEmpty()) {
            relations = filterByRelationTypes(relations, query.getRelationTypes());
        }

        result.setRelations(relations);

        return result;
    }

    /**
     * 处理路径查询
     */
    private GraphQuery.GraphQueryResult handlePathQuery(GraphStore graphStore,
                                                       GraphQuery query,
                                                       GraphQuery.GraphQueryResult result) {
        String sourceId = query.getSourceId();
        String targetId = query.getTargetId();

        if (sourceId == null || targetId == null) {
            result.setSuccess(false);
            result.setErrorMessage("源节点和目标节点ID不能为空");
            return result;
        }

        List<Entity> path = graphStore.findShortestPath(sourceId, targetId);

        if (path != null && !path.isEmpty()) {
            List<GraphQuery.Path> paths = new ArrayList<>();
            paths.add(GraphQuery.Path.builder()
                    .nodes(path)
                    .length(path.size() - 1)
                    .build());

            result.setPaths(paths);
        } else {
            result.setPaths(Collections.emptyList());
        }

        return result;
    }

    /**
     * 处理社区查询
     */
    private GraphQuery.GraphQueryResult handleCommunityQuery(GraphStore graphStore,
                                                              GraphQuery query,
                                                              GraphQuery.GraphQueryResult result) {
        String entityId = query.getEntityId();

        if (entityId == null || entityId.isEmpty()) {
            result.setSuccess(false);
            result.setErrorMessage("节点ID不能为空");
            return result;
        }

        List<Entity> community = graphStore.findCommunity(entityId);

        result.setEntities(community);
        result.setCommunitySummary(generateCommunitySummary(community));

        return result;
    }

    /**
     * 处理模式查询
     */
    private GraphQuery.GraphQueryResult handlePatternQuery(GraphStore graphStore,
                                                            GraphQuery query,
                                                            GraphQuery.GraphQueryResult result) {
        // 简化实现：返回空结果
        result.setEntities(Collections.emptyList());
        result.setRelations(Collections.emptyList());

        return result;
    }

    /**
     * 按关系类型过滤
     */
    private List<Relation> filterByRelationTypes(List<Relation> relations, List<String> types) {
        List<Relation> filtered = new ArrayList<>();

        for (Relation relation : relations) {
            if (types.contains(relation.getType())) {
                filtered.add(relation);
            }
        }

        return filtered;
    }

    /**
     * 生成社区摘要
     */
    private String generateCommunitySummary(List<Entity> community) {
        if (community == null || community.isEmpty()) {
            return "";
        }

        StringBuilder summary = new StringBuilder();
        summary.append("社区包含").append(community.size()).append("个实体：");

        Map<String, Integer> typeCount = new HashMap<>();

        for (Entity entity : community) {
            String type = entity.getType();
            typeCount.put(type, typeCount.getOrDefault(type, 0) + 1);
        }

        for (Map.Entry<String, Integer> entry : typeCount.entrySet()) {
            summary.append(entry.getKey()).append("(").append(entry.getValue()).append(") ");
        }

        return summary.toString();
    }
}
