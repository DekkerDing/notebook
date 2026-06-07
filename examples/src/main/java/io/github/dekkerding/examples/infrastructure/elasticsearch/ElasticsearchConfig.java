package io.github.dekkerding.examples.infrastructure.elasticsearch;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Elasticsearch配置类
 *
 * @author Notebook RAG Team
 * @since 1.0.0
 */
@Slf4j
@Data
@Configuration
@ConfigurationProperties(prefix = "elasticsearch")
public class ElasticsearchConfig {

    /**
     * Elasticsearch主机地址
     */
    private String host = "192.168.10.107";

    /**
     * Elasticsearch端口
     */
    private int port = 9200;

    /**
     * 协议（http/https）
     */
    private String scheme = "http";

    /**
     * 用户名
     */
    private String username;

    /**
     * 密码
     */
    private String password;

    /**
     * 连接超时（毫秒）
     */
    private int connectTimeout = 5000;

    /**
     * Socket超时（毫秒）
     */
    private int socketTimeout = 30000;

    /**
     * 向量索引名称
     */
    private String vectorIndexName = "kb_vector_index";

    /**
     * 向量维度
     */
    private int vectorDimension = 1536;

    /**
     * 相似度算法（cosine/dot_product/l2_norm）
     */
    private String similarity = "cosine";

    /**
     * HNSW参数
     */
    private HnswConfig hnsw = new HnswConfig();

    /**
     * 索引优化配置
     */
    private IndexOptimizationConfig indexOptimization = new IndexOptimizationConfig();

    /**
     * 获取完整URL
     */
    public String getHttpUrl() {
        return scheme + "://" + host + ":" + port;
    }

    /**
     * HNSW配置
     */
    @Data
    public static class HnswConfig {
        /**
         * M参数：每个节点的最大连接数
         */
        private int m = 16;

        /**
         * ef_construction参数：构建时的候选数
         */
        private int efConstruction = 100;

        /**
         * ef_search参数：搜索时的候选数
         */
        private int efSearch = 50;
    }

    /**
     * 索引优化配置
     */
    @Data
    public static class IndexOptimizationConfig {
        /**
         * 索引刷新间隔（秒）
         */
        private int refreshInterval = 30;

        /**
         * 最小批量大小
         */
        private int bulkMinSize = 500;

        /**
         * 最大批量大小
         */
        private int bulkMaxSize = 2000;

        /**
         * 索引线程池大小
         */
        private int threadPoolSize = 4;

        /**
         * 索引队列容量
         */
        private int queueCapacity = 1000;

        /**
         * 索引超时（秒）
         */
        private int indexTimeout = 60;
    }

    /**
     * 验证配置
     */
    public boolean isValid() {
        if (host == null || host.isEmpty()) {
            log.error("Elasticsearch host不能为空");
            return false;
        }

        if (port < 1 || port > 65535) {
            log.error("Elasticsearch port无效: {}", port);
            return false;
        }

        if (vectorDimension < 1) {
            log.error("向量维度无效: {}", vectorDimension);
            return false;
        }

        return true;
    }

    /**
     * 获取KNN索引配置
     */
    public String getKnnIndexMapping() {
        return "{\n" +
                "  \"properties\": {\n" +
                "    \"content\": {\n" +
                "      \"type\": \"text\",\n" +
                "      \"fields\": {\n" +
                "        \"keyword\": {\n" +
                "          \"type\": \"keyword\"\n" +
                "        }\n" +
                "      }\n" +
                "    },\n" +
                "    \"content_vector\": {\n" +
                "      \"type\": \"dense_vector\",\n" +
                "      \"dims\": " + vectorDimension + ",\n" +
                "      \"index\": true,\n" +
                "      \"similarity\": \"" + similarity + "\"\n" +
                "    },\n" +
                "    \"doc_id\": {\n" +
                "      \"type\": \"keyword\"\n" +
                "    },\n" +
                "    \"kb_id\": {\n" +
                "      \"type\": \"keyword\"\n" +
                "    },\n" +
                "    \"metadata\": {\n" +
                "      \"type\": \"object\",\n" +
                "      \"dynamic\": true\n" +
                "    },\n" +
                "    \"chunk_type\": {\n" +
                "      \"type\": \"keyword\"\n" +
                "    },\n" +
                "    \"created_at\": {\n" +
                "      \"type\": \"date\"\n" +
                "    }\n" +
                "  }\n" +
                "}";
    }
}
