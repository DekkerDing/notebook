package io.github.dekkerding.examples.infrastructure.elasticsearch;

import io.github.dekkerding.examples.domain.retrieval.model.RetrieveRequest;
import io.github.dekkerding.examples.domain.retrieval.model.RetrieveResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

/**
 * Elasticsearch向量检索服务
 *
 * <p>功能：</p>
 * <ul>
 *   <li>KNN向量检索</li>
 *   <li>混合检索（向量+关键词）</li>
 *   <li>多条件过滤检索</li>
 *   <li>结果高亮</li>
 * </ul>
 *
 * @author Notebook RAG Team
 * @since 1.0.0
 */
@Slf4j
@Service
public class ElasticsearchVectorRetrieveService {

    @Autowired
    private ElasticsearchConfig config;

    @Autowired
    private RestTemplate restTemplate;

    /**
     * KNN向量检索
     *
     * @param queryVector 查询向量
     * @param topK 返回数量
     * @param kbIds 知识库ID过滤（可选）
     * @return 检索结果
     */
    public List<RetrieveResult> knnSearch(float[] queryVector, int topK, String... kbIds) {
        log.debug("KNN检索: topK={}, kbIds={}", topK, kbIds);

        long startTime = System.currentTimeMillis();

        try {
            // 构建KNN查询
            Map<String, Object> knnQuery = buildKnnQuery(queryVector, topK, kbIds);

            // 发送请求
            String url = config.getHttpUrl() + "/" + config.getVectorIndexName() + "/_search";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(knnQuery, headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    request,
                    String.class
            );

            if (response.getStatusCode() == HttpStatus.OK) {
                List<RetrieveResult> results = parseKnnResponse(response.getBody());
                long duration = System.currentTimeMillis() - startTime;

                // 设置延迟
                for (RetrieveResult result : results) {
                    result.setLatency(duration);
                    result.setSource("es_knn");
                }

                log.debug("KNN检索完成: results={}, duration={}ms",
                        results.size(), duration);

                return results;
            } else {
                log.warn("KNN检索失败: status={}", response.getStatusCode());
                return Collections.emptyList();
            }

        } catch (Exception e) {
            log.error("KNN检索异常", e);
            return Collections.emptyList();
        }
    }

    /**
     * 混合检索（向量+BM25）
     */
    public List<RetrieveResult> hybridSearch(String queryText, float[] queryVector,
                                           int topK, String... kbIds) {
        log.debug("混合检索: topK={}, kbIds={}", topK, kbIds);

        long startTime = System.currentTimeMillis();

        try {
            // 构建混合查询
            Map<String, Object> hybridQuery = buildHybridQuery(queryText, queryVector, topK, kbIds);

            // 发送请求
            String url = config.getHttpUrl() + "/" + config.getVectorIndexName() + "/_search";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(hybridQuery, headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    request,
                    String.class
            );

            if (response.getStatusCode() == HttpStatus.OK) {
                List<RetrieveResult> results = parseHybridResponse(response.getBody());
                long duration = System.currentTimeMillis() - startTime;

                for (RetrieveResult result : results) {
                    result.setLatency(duration);
                    result.setSource("es_hybrid");
                }

                log.debug("混合检索完成: results={}, duration={}ms",
                        results.size(), duration);

                return results;
            } else {
                log.warn("混合检索失败: status={}", response.getStatusCode());
                return Collections.emptyList();
            }

        } catch (Exception e) {
            log.error("混合检索异常", e);
            return Collections.emptyList();
        }
    }

    /**
     * 构建KNN查询
     */
    private Map<String, Object> buildKnnQuery(float[] queryVector, int topK, String[] kbIds) {
        Map<String, Object> query = new HashMap<>();

        // KNN查询部分
        Map<String, Object> knn = new HashMap<>();
        knn.put("field", "content_vector");
        knn.put("query_vector", vectorToList(queryVector));
        knn.put("k", topK);
        knn.put("num_candidates", Math.max(topK * 2, 50)); // num_candidates应该>=k

        query.put("knn", knn);

        // 过滤条件
        if (kbIds != null && kbIds.length > 0) {
            Map<String, Object> filter = new HashMap<>();
            Map<String, Object> termsFilter = new HashMap<>();
            termsFilter.put("kb_id", Arrays.asList(kbIds));
            filter.put("terms", termsFilter);
            query.put("post_filter", filter);
        }

        // 返回字段
        query.put("_source", Arrays.asList("content", "doc_id", "kb_id", "metadata", "chunk_type"));

        return query;
    }

    /**
     * 构建混合查询
     */
    private Map<String, Object> buildHybridQuery(String queryText, float[] queryVector,
                                                   int topK, String[] kbIds) {
        Map<String, Object> query = new HashMap<>();

        // 布尔查询：should包含KNN和文本匹配
        Map<String, Object> boolQuery = new HashMap<>();

        List<Map<String, Object>> should = new ArrayList<>();

        // KNN查询
        Map<String, Object> knn = new HashMap<>();
        knn.put("field", "content_vector");
        knn.put("query_vector", vectorToList(queryVector));
        knn.put("k", topK);
        knn.put("num_candidates", Math.max(topK * 2, 50));
        Map<String, Object> knnQuery = new HashMap<>();
        knnQuery.put("knn", knn);
        should.add(knnQuery);

        // 文本匹配查询
        Map<String, Object> match = new HashMap<>();
        Map<String, Object> matchQuery = new HashMap<>();
        matchQuery.put("query", queryText);
        matchQuery.put("boost", 0.5); // 降低文本匹配权重
        match.put("content", matchQuery);
        Map<String, Object> matchQueryWrapper = new HashMap<>();
        matchQueryWrapper.put("match", match);
        should.add(matchQueryWrapper);

        boolQuery.put("should", should);
        Map<String, Object> queryWrapper = new HashMap<>();
        queryWrapper.put("bool", boolQuery);
        query.put("query", queryWrapper);

        // 过滤条件
        if (kbIds != null && kbIds.length > 0) {
            Map<String, Object> filter = new HashMap<>();
            Map<String, Object> termsFilter = new HashMap<>();
            termsFilter.put("kb_id", Arrays.asList(kbIds));
            filter.put("terms", termsFilter);
            query.put("post_filter", filter);
        }

        // 返回字段
        query.put("_source", Arrays.asList("content", "doc_id", "kb_id", "metadata", "chunk_type"));

        // 高亮
        Map<String, Object> highlight = new HashMap<>();
        Map<String, Object> highlightFields = new HashMap<>();
        highlightFields.put("fragment_size", 150);
        highlightFields.put("number_of_fragments", 1);
        highlight.put("content", highlightFields);
        Map<String, Object> highlightWrapper = new HashMap<>();
        highlightWrapper.put("fields", highlightFields);
        query.put("highlight", highlightWrapper);

        return query;
    }

    /**
     * 解析KNN响应
     */
    private List<RetrieveResult> parseKnnResponse(String responseBody) {
        List<RetrieveResult> results = new ArrayList<>();

        try {
            // 简化解析，实际应使用Jackson
            if (responseBody == null || responseBody.isEmpty()) {
                return results;
            }

            // 查找hits数组
            int hitsIdx = responseBody.indexOf("\"hits\":");
            if (hitsIdx < 0) return results;

            // 简化：这里假设响应格式正确，实际应完整解析JSON
            // 可以使用Jackson或Gson完整解析

            log.debug("KNN响应长度: {}", responseBody.length());

            // 临时返回空列表，实际需要完整JSON解析
            return results;

        } catch (Exception e) {
            log.error("解析KNN响应失败", e);
            return results;
        }
    }

    /**
     * 解析混合检索响应
     */
    private List<RetrieveResult> parseHybridResponse(String responseBody) {
        // 与KNN类似，需要完整JSON解析
        List<RetrieveResult> results = new ArrayList<>();

        try {
            // TODO: 完整实现JSON解析
            return results;
        } catch (Exception e) {
            log.error("解析混合响应失败", e);
            return results;
        }
    }

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
     * 获取检索统计
     */
    public Map<String, Object> getStats() {
        Map<String, Object> stats = new HashMap<>();

        try {
            // 获取集群健康状态
            String healthUrl = config.getHttpUrl() + "/_cluster/health/" + config.getVectorIndexName();
            ResponseEntity<String> healthResponse = restTemplate.getForEntity(healthUrl, String.class);

            // 获取索引统计
            String statsUrl = config.getHttpUrl() + "/" + config.getVectorIndexName() + "/_stats";
            ResponseEntity<String> statsResponse = restTemplate.getForEntity(statsUrl, String.class);

            stats.put("health", healthResponse.getStatusCode() == HttpStatus.OK ? "OK" : "ERROR");
            stats.put("index_stats", statsResponse.getBody());

        } catch (Exception e) {
            stats.put("health", "ERROR");
            stats.put("error", e.getMessage());
        }

        return stats;
    }
}
