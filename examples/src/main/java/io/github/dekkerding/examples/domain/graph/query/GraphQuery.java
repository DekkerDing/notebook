package io.github.dekkerding.examples.domain.graph.query;

import io.github.dekkerding.examples.domain.graph.model.Entity;
import io.github.dekkerding.examples.domain.graph.model.Relation;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 图查询模型
 *
 * @author Notebook RAG Team
 * @since 1.0.0
 */
@Data
@Builder
public class GraphQuery {

    /**
     * 查询类型
     */
    private QueryType type;

    /**
     * 查询参数
     */
    private Map<String, Object> parameters;

    /**
     * 实体ID（用于节点查询）
     */
    private String entityId;

    /**
     * 源实体ID（用于路径查询）
     */
    private String sourceId;

    /**
     * 目标实体ID（用于路径查询）
     */
    private String targetId;

    /**
     * 跳数限制（默认3跳）
     */
    private Integer hops;

    /**
     * 关系类型过滤
     */
    private List<String> relationTypes;

    /**
     * 实体类型过滤
     */
    private List<String> entityTypes;

    /**
     * 结果限制
     */
    private Integer limit;

    /**
     * 查询结果
     */
    private GraphQueryResult result;

    /**
     * 查询类型枚举
     */
    public enum QueryType {
        /**
         * 节点查询
         */
        NODE_QUERY,

        /**
         * 关系查询
         */
        RELATION_QUERY,

        /**
         * 路径查询
         */
        PATH_QUERY,

        /**
         * 社区查询
         */
        COMMUNITY_QUERY,

        /**
         * 模式查询
         */
        PATTERN_QUERY
    }

    /**
     * 查询结果
     */
    @Data
    @Builder
    public static class GraphQueryResult {
        /**
         * 查询是否成功
         */
        private boolean success;

        /**
         * 查询到的实体列表
         */
        private List<Entity> entities;

        /**
         * 查询到的关系列表
         */
        private List<Relation> relations;

        /**
         * 查询到的路径
         */
        private List<Path> paths;

        /**
         * 社区摘要
         */
        private String communitySummary;

        /**
         * 错误信息
         */
        private String errorMessage;
    }

    /**
     * 路径信息
     */
    @Data
    @Builder
    public static class Path {
        /**
         * 路径上的节点
         */
        private List<Entity> nodes;

        /**
         * 路径上的关系
         */
        private List<Relation> relations;

        /**
         * 路径长度
         */
        private Integer length;
    }
}
