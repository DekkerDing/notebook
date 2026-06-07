package io.github.dekkerding.examples.infrastructure.graph;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 图存储配置类
 *
 * @author Notebook RAG Team
 * @since 1.0.0
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "graph")
public class GraphProperties {

    /**
     * 图存储类型（neo4j、memory）
     */
    private GraphType type = GraphType.MEMORY;

    /**
     * Neo4j配置
     */
    private Neo4jConfig neo4j = new Neo4jConfig();

    /**
     * 实体抽取配置
     */
    private EntityExtractionConfig entityExtraction = new EntityExtractionConfig();

    /**
     * 关系抽取配置
     */
    private RelationExtractionConfig relationExtraction = new RelationExtractionConfig();

    /**
     * 图存储类型枚举
     */
    public enum GraphType {
        /**
         * Neo4j图数据库
         */
        NEO4J,

        /**
         * 内存图（JGraphT）
         */
        MEMORY
    }

    /**
     * Neo4j配置
     */
    @Data
    public static class Neo4jConfig {
        /**
         * Neo4j主机地址
         */
        private String host = "localhost";

        /**
         * Neo4j Bolt端口
         */
        private int port = 7687;

        /**
         * 协议（bolt://或bolt+routing://）
         */
        private String scheme = "bolt://";

        /**
         * 用户名
         */
        private String username = "neo4j";

        /**
         * 密码
         */
        private String password = "password";

        /**
         * 数据库名称
         */
        private String database = "neo4j";

        /**
         * 连接超时（毫秒）
         */
        private int connectionTimeout = 5000;

        /**
         * 获取完整Bolt URL
         */
        public String getBoltUrl() {
            return scheme + host + ":" + port;
        }
    }

    /**
     * 实体抽取配置
     */
    @Data
    public static class EntityExtractionConfig {
        /**
         * 是否启用实体抽取
         */
        private boolean enabled = true;

        /**
         * 实体置信度阈值（0-1）
         */
        private double confidenceThreshold = 0.7;

        /**
         * 最大实体数量
         */
        private int maxEntities = 1000;

        /**
         * 支持的实体类型（逗号分隔）
         */
        private String supportedTypes = "PERSON,ORG,LOC,DATE,NUMBER";

        /**
         * 使用NLP工具（false则使用规则）
         */
        private boolean useNlp = false;
    }

    /**
     * 关系抽取配置
     */
    @Data
    public static class RelationExtractionConfig {
        /**
         * 是否启用关系抽取
         */
        private boolean enabled = true;

        /**
         * 关系置信度阈值（0-1）
         */
        private double confidenceThreshold = 0.6;

        /**
         * 最大关系数量
         */
        private int maxRelations = 2000;

        /**
         * 最大跳数限制
         */
        private int maxHops = 3;
    }
}
