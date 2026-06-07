package io.github.dekkerding.examples.interfaces.graph;

import io.github.dekkerding.examples.common.ApiResponse;
import io.github.dekkerding.examples.domain.graph.model.Entity;
import io.github.dekkerding.examples.domain.graph.model.Relation;
import io.github.dekkerding.examples.domain.graph.query.GraphQuery;
import io.github.dekkerding.examples.domain.graph.query.GraphQueryService;
import io.github.dekkerding.examples.domain.graph.store.GraphStore;
import io.github.dekkerding.examples.infrastructure.graph.GraphStoreFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 图查询接口
 *
 * @author Notebook RAG Team
 * @since 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/graph")
public class GraphController {

    @Autowired
    private GraphStoreFactory graphStoreFactory;

    @Autowired
    private GraphQueryService graphQueryService;

    /**
     * 获取实体的关系
     */
    @GetMapping("/entities/{id}/relations")
    public ApiResponse<Map<String, Object>> getEntityRelations(
            @PathVariable String id,
            @RequestParam(defaultValue = "1") int hops) {

        log.info("查询实体关系: id={}, hops={}", id, hops);

        try {
            GraphStore graphStore = graphStoreFactory.getGraphStore();

            // 验证跳数
            if (hops < 1 || hops > 3) {
                hops = 1;
            }

            // 查询关系
            List<Relation> relations = graphStore.findRelations(id, hops);

            // 获取关联实体
            Map<String, Entity> relatedEntities = new HashMap<>();

            for (Relation relation : relations) {
                Entity source = graphStore.findNode(relation.getSource());
                Entity target = graphStore.findNode(relation.getTarget());

                if (source != null) {
                    relatedEntities.put(source.getId(), source);
                }

                if (target != null) {
                    relatedEntities.put(target.getId(), target);
                }
            }

            Map<String, Object> result = new HashMap<>();
            result.put("relations", relations);
            result.put("entities", relatedEntities.values());
            result.put("total", relations.size());

            return ApiResponse.success(result);

        } catch (Exception e) {
            log.error("查询实体关系失败: id={}", id, e);
            return ApiResponse.error("QUERY_ERROR", "查询失败: " + e.getMessage());
        }
    }

    /**
     * 执行图查询
     */
    @PostMapping("/query")
    public ApiResponse<GraphQuery.GraphQueryResult> executeQuery(@RequestBody GraphQuery query) {
        log.info("执行图查询: type={}", query.getType());

        try {
            GraphQuery.GraphQueryResult result = graphQueryService.execute(query);

            if (result.isSuccess()) {
                return ApiResponse.success(result);
            } else {
                return ApiResponse.error("QUERY_ERROR", result.getErrorMessage());
            }

        } catch (Exception e) {
            log.error("图查询执行失败", e);
            return ApiResponse.error("QUERY_ERROR", "查询失败: " + e.getMessage());
        }
    }

    /**
     * 获取社区摘要
     */
    @GetMapping("/communities/{id}/summary")
    public ApiResponse<String> getCommunitySummary(@PathVariable String id) {
        log.info("获取社区摘要: id={}", id);

        try {
            GraphQuery query = GraphQuery.builder()
                    .type(GraphQuery.QueryType.COMMUNITY_QUERY)
                    .entityId(id)
                    .build();

            GraphQuery.GraphQueryResult result = graphQueryService.execute(query);

            if (result.isSuccess() && result.getCommunitySummary() != null) {
                return ApiResponse.success(result.getCommunitySummary());
            } else {
                return ApiResponse.error("QUERY_ERROR", "获取社区摘要失败");
            }

        } catch (Exception e) {
            log.error("获取社区摘要失败: id={}", id, e);
            return ApiResponse.error("QUERY_ERROR", "查询失败: " + e.getMessage());
        }
    }

    /**
     * 获取实体信息
     */
    @GetMapping("/entities/{id}")
    public ApiResponse<Entity> getEntity(@PathVariable String id) {
        log.info("获取实体信息: id={}", id);

        try {
            GraphStore graphStore = graphStoreFactory.getGraphStore();
            Entity entity = graphStore.findNode(id);

            if (entity != null) {
                return ApiResponse.success(entity);
            } else {
                return ApiResponse.error("NOT_FOUND", "实体不存在: " + id);
            }

        } catch (Exception e) {
            log.error("获取实体信息失败: id={}", id, e);
            return ApiResponse.error("QUERY_ERROR", "查询失败: " + e.getMessage());
        }
    }

    /**
     * 获取图谱统计信息
     */
    @GetMapping("/stats")
    public ApiResponse<Map<String, Object>> getStats() {
        try {
            GraphStore graphStore = graphStoreFactory.getGraphStore();

            Map<String, Object> stats = new HashMap<>();
            stats.put("nodeCount", getNodeCount(graphStore));
            stats.put("edgeCount", getEdgeCount(graphStore));

            return ApiResponse.success(stats);

        } catch (Exception e) {
            log.error("获取图谱统计失败", e);
            return ApiResponse.error("QUERY_ERROR", "查询失败: " + e.getMessage());
        }
    }

    /**
     * 获取节点数量（简化实现）
     */
    private long getNodeCount(GraphStore graphStore) {
        // 简化：返回所有类型PERSON节点的数量
        List<Entity> entities = graphStore.findNodeByType("PERSON");
        return entities.size();
    }

    /**
     * 获取边数量（简化实现）
     */
    private long getEdgeCount(GraphStore graphStore) {
        // 简化实现
        return 0;
    }
}
