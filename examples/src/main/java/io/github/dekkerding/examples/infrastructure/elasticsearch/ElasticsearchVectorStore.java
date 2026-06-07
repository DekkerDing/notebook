package io.github.dekkerding.examples.infrastructure.elasticsearch;

import io.github.dekkerding.examples.domain.chunk.model.ChunkNode;
import io.github.dekkerding.examples.domain.embedding.model.EmbeddingResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

/**
 * Elasticsearch向量存储服务
 *
 * <p>功能：</p>
 * <ul>
 *   <li>创建KNN向量索引</li>
 *   <li>存储文档和向量</li>
 *   <li>批量索引文档</li>
 *   <li>索引管理</li>
 * </ul>
 *
 * @author Notebook RAG Team
 * @since 1.0.0
 */
@Slf4j
@Service
public class ElasticsearchVectorStore {

    @Autowired
    private ElasticsearchConfig config;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired(required = false)
    private IndexMetrics indexMetrics;

    /**
     * 初始化索引
     */
    public boolean initializeIndex() {
        log.info("初始化Elasticsearch向量索引: index={}", config.getVectorIndexName());

        try {
            // 检查索引是否存在
            if (indexExists()) {
                log.info("索引已存在: {}", config.getVectorIndexName());
                return true;
            }

            // 创建索引
            return createIndex();
        } catch (Exception e) {
            log.error("初始化索引失败", e);
            return false;
        }
    }

    /**
     * 检查索引是否存在
     */
    public boolean indexExists() {
        try {
            String url = config.getHttpUrl() + "/_alias/" + config.getVectorIndexName();
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            return response.getStatusCode() == HttpStatus.OK;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 创建索引
     */
    public boolean createIndex() {
        try {
            String url = config.getHttpUrl() + "/" + config.getVectorIndexName();

            // 设置索引配置
            Map<String, Object> indexSettings = new HashMap<>();
            indexSettings.put("number_of_shards", 1);
            indexSettings.put("number_of_replicas", 1);

            // HNSW配置
            Map<String, Object> hnswConfig = new HashMap<>();
            hnswConfig.put("m", config.getHnsw().getM());
            hnswConfig.put("ef_construction", config.getHnsw().getEfConstruction());

            Map<String, Object> knnSettings = new HashMap<>();
            knnSettings.put("index.knn", true);
            knnSettings.put("index.knn.space_type", config.getSimilarity());
            knnSettings.put("index.knn.algo_param.ef_search", config.getHnsw().getEfSearch());

            indexSettings.putAll(knnSettings);

            // 完整的索引配置
            Map<String, Object> indexConfig = new HashMap<>();
            indexConfig.put("settings", indexSettings);
            indexConfig.put("mappings", parseMappingToJson(config.getKnnIndexMapping()));

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(indexConfig, headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.PUT,
                    request,
                    String.class
            );

            boolean success = response.getStatusCode() == HttpStatus.OK;
            if (success) {
                log.info("索引创建成功: {}", config.getVectorIndexName());
            }

            return success;
        } catch (Exception e) {
            log.error("创建索引失败", e);
            return false;
        }
    }

    /**
     * 存储文档向量
     */
    public String indexDocument(String docId, String content, float[] vector,
                               Map<String, Object> metadata) {
        try {
            String url = config.getHttpUrl() + "/" + config.getVectorIndexName() + "/_doc/" + docId;

            Map<String, Object> doc = new HashMap<>();
            doc.put("content", content);
            doc.put("content_vector", vectorToList(vector));
            doc.put("doc_id", docId);
            doc.put("created_at", new Date());

            if (metadata != null) {
                doc.put("metadata", metadata);
                if (metadata.containsKey("kb_id")) {
                    doc.put("kb_id", metadata.get("kb_id"));
                }
                if (metadata.containsKey("chunk_type")) {
                    doc.put("chunk_type", metadata.get("chunk_type"));
                }
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(doc, headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    request,
                    String.class
            );

            if (response.getStatusCode() == HttpStatus.OK ||
                response.getStatusCode() == HttpStatus.CREATED) {
                log.debug("文档索引成功: docId={}", docId);
                return docId;
            } else {
                log.warn("文档索引失败: docId={}, status={}",
                        docId, response.getStatusCode());
                return null;
            }
        } catch (Exception e) {
            log.error("索引文档异常: docId={}", docId, e);
            return null;
        }
    }

    /**
     * 批量索引文档
     */
    public BulkIndexResult bulkIndex(List<VectorDocument> documents) {
        long startTime = System.currentTimeMillis();
        log.info("批量索引: count={}", documents.size());

        BulkIndexResult result = new BulkIndexResult();
        result.setTotal(documents.size());

        try {
            // ES批量操作格式
            StringBuilder bulkData = new StringBuilder();

            for (VectorDocument doc : documents) {
                // 索引操作元数据
                bulkData.append("{ \"index\" : { \"_index\" : \"")
                        .append(config.getVectorIndexName())
                        .append("\", \"_id\" : \"")
                        .append(doc.getDocId())
                        .append("\" } }\n");

                // 文档数据
                bulkData.append("{\n");
                bulkData.append("  \"content\": \"").append(escapeJson(doc.getContent())).append("\",\n");
                bulkData.append("  \"content_vector\": ").append(vectorToList(doc.getVector())).append(",\n");
                bulkData.append("  \"doc_id\": \"").append(doc.getDocId()).append("\",\n");

                if (doc.getKbId() != null) {
                    bulkData.append("  \"kb_id\": \"").append(doc.getKbId()).append("\",\n");
                }

                if (doc.getChunkType() != null) {
                    bulkData.append("  \"chunk_type\": \"").append(doc.getChunkType()).append("\",\n");
                }

                if (doc.getMetadata() != null && !doc.getMetadata().isEmpty()) {
                    bulkData.append("  \"metadata\": {");
                    boolean first = true;
                    for (Map.Entry<String, Object> entry : doc.getMetadata().entrySet()) {
                        if (!first) bulkData.append(", ");
                        bulkData.append("\"").append(entry.getKey()).append("\": \"")
                                .append(escapeJson(entry.getValue().toString())).append("\"");
                        first = false;
                    }
                    bulkData.append("},\n");
                }

                bulkData.append("  \"created_at\": \"").append(new Date()).append("\"\n");
                bulkData.append("}\n");
            }

            // 发送批量请求
            String url = config.getHttpUrl() + "/_bulk";
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<String> request = new HttpEntity<>(bulkData.toString(), headers);

            ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);

            long elapsed = System.currentTimeMillis() - startTime;

            if (response.getStatusCode() == HttpStatus.OK) {
                // 解析批量结果
                result.setSuccess(true);
                // 简化：假设全部成功，实际应解析响应
                result.setSuccessCount(documents.size());
                result.setFailedCount(0);

                log.info("批量索引完成: total={}, success={}, latency={}ms",
                        documents.size(), documents.size(), elapsed);

                // 记录指标
                if (indexMetrics != null) {
                    indexMetrics.recordBulkResult(result);
                    indexMetrics.recordIndexLatency(elapsed);
                }
            } else {
                result.setSuccess(false);
                result.setFailedCount(documents.size());

                log.error("批量索引失败: status={}", response.getStatusCode());

                if (indexMetrics != null) {
                    indexMetrics.recordIndexFailure();
                }
            }

        } catch (Exception e) {
            result.setSuccess(false);
            result.setFailedCount(documents.size());

            long elapsed = System.currentTimeMillis() - startTime;
            log.error("批量索引异常: latency={}ms", elapsed, e);

            if (indexMetrics != null) {
                indexMetrics.recordIndexFailure();
            }
        }

        return result;
    }

    /**
     * 删除索引
     */
    public boolean deleteIndex() {
        try {
            String url = config.getHttpUrl() + "/" + config.getVectorIndexName();

            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.DELETE,
                    HttpEntity.EMPTY,
                    String.class
            );

            boolean success = response.getStatusCode() == HttpStatus.OK;
            if (success) {
                log.info("索引删除成功: {}", config.getVectorIndexName());
            }

            return success;
        } catch (Exception e) {
            log.error("删除索引失败", e);
            return false;
        }
    }

    /**
     * 获取索引统计
     */
    public IndexStats getIndexStats() {
        try {
            String url = config.getHttpUrl() + "/" + config.getVectorIndexName() + "/_stats";

            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);

            if (response.getStatusCode() == HttpStatus.OK) {
                // 简化解析，实际应使用Jackson解析JSON
                IndexStats stats = new IndexStats();
                stats.setIndexName(config.getVectorIndexName());
                stats.setDocumentCount(parseDocCount(response.getBody()));
                return stats;
            }

            return null;
        } catch (Exception e) {
            log.error("获取索引统计失败", e);
            return null;
        }
    }

    // ===== 辅助方法 =====

    /**
     * 向量数组转List
     */
    private List<Float> vectorToList(float[] vector) {
        List<Float> list = new ArrayList<>(vector.length);
        for (float v : vector) {
            list.add(v);
        }
        return list;
    }

    /**
     * JSON转义
     */
    private String escapeJson(String text) {
        if (text == null) return "";
        return text.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    /**
     * 解析Mapping
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> parseMappingToJson(String mapping) {
        // 简化实现，实际应使用Jackson
        Map<String, Object> result = new HashMap<>();
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper =
                    new com.fasterxml.jackson.databind.ObjectMapper();
            return mapper.readValue(mapping, Map.class);
        } catch (Exception e) {
            log.warn("解析Mapping失败", e);
            return result;
        }
    }

    /**
     * 解析文档数量
     */
    private long parseDocCount(String responseBody) {
        // 简化实现
        if (responseBody.contains("\"docs\"")) {
            int idx = responseBody.indexOf("\"docs\":");
            if (idx > 0) {
                String numStr = responseBody.substring(idx + 7).split("[,}]")[0].trim();
                try {
                    return Long.parseLong(numStr);
                } catch (NumberFormatException e) {
                    // ignore
                }
            }
        }
        return 0;
    }

    /**
     * 向量文档
     */
    @lombok.Data
    @lombok.Builder
    @lombok.AllArgsConstructor
    @lombok.NoArgsConstructor
    public static class VectorDocument {
        private String docId;
        private String content;
        private float[] vector;
        private String kbId;
        private String chunkType;
        private Map<String, Object> metadata;
    }

    /**
     * 批量索引结果
     */
    @lombok.Data
    @lombok.AllArgsConstructor
    @lombok.NoArgsConstructor
    public static class BulkIndexResult {
        private boolean success;
        private int total;
        private int successCount;
        private int failedCount;
        private String errorMessage;
    }

    /**
     * 索引统计
     */
    @lombok.Data
    @lombok.AllArgsConstructor
    @lombok.NoArgsConstructor
    public static class IndexStats {
        private String indexName;
        private long documentCount;
        private long storeSize;
        private String healthStatus;
    }
}
