package io.github.dekkerding.examples.domain.retrieval.strategy;

import io.github.dekkerding.examples.domain.retrieval.coordinator.HybridRetrieveCoordinator;
import io.github.dekkerding.examples.domain.retrieval.model.RetrieveRequest;
import io.github.dekkerding.examples.domain.retrieval.model.RetrieveResult;
import io.github.dekkerding.examples.domain.retrieval.strategy.impl.BM25RetrieveStrategy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 检索策略测试
 *
 * @author Notebook RAG Team
 * @since 1.0.0
 */
@SpringBootTest
class RetrieveStrategyTest {

    @Autowired(required = false)
    private HybridRetrieveCoordinator coordinator;

    @Autowired
    private BM25RetrieveStrategy bm25Strategy;

    @BeforeEach
    void setUp() {
        // 索引测试文档
        Map<String, String> testDocs = new HashMap<>();
        testDocs.put("doc1", "Java是一种广泛使用的编程语言，特别适合企业级应用开发");
        testDocs.put("doc2", "Python是一种简洁的编程语言，在数据科学和人工智能领域非常流行");
        testDocs.put("doc3", "Spring Boot是基于Spring框架的快速开发工具，简化了Spring应用的配置");
        testDocs.put("doc4", "Elasticsearch是一个基于Lucene的搜索引擎，提供RESTful接口和分布式特性");
        testDocs.put("doc5", "Redis是一个高性能的键值存储系统，常用于缓存和消息队列");

        bm25Strategy.indexBatch(testDocs);
    }

    @Test
    void testBM25Strategy() {
        RetrieveRequest request = RetrieveRequest.builder()
                .topK(3)
                .build();

        List<RetrieveResult> results = bm25Strategy.retrieve("Java编程语言", request, null);

        assertNotNull(results);
        assertFalse(results.isEmpty());
        assertEquals(3, results.size());

        // 验证结果排序（分数从高到低）
        for (int i = 0; i < results.size() - 1; i++) {
            assertTrue(results.get(i).getScore() >= results.get(i + 1).getScore());
        }

        // 验证结果来源
        results.forEach(r -> assertEquals("bm25", r.getSource()));

        System.out.println("BM25检索结果:");
        results.forEach(r -> System.out.println("  " + r));
    }

    @Test
    void testBM25KeywordQuery() {
        RetrieveRequest request = RetrieveRequest.builder()
                .topK(5)
                .build();

        // 关键词查询
        List<RetrieveResult> results = bm25Strategy.retrieve("Spring", request, null);

        assertNotNull(results);
        assertTrue(results.size() > 0);

        // Spring应该在doc3中排名最高
        boolean foundSpring = results.stream()
                .anyMatch(r -> r.getDocId().equals("doc3"));

        assertTrue(foundSpring, "应该找到包含Spring的文档");
    }

    @Test
    void testRetrieveRequestValidation() {
        // 有效请求
        RetrieveRequest validRequest = RetrieveRequest.builder()
                .query("test query")
                .topK(10)
                .build();

        assertTrue(validRequest.isValid());

        // 无效请求（空查询）
        RetrieveRequest invalidRequest = RetrieveRequest.builder()
                .query("")
                .topK(10)
                .build();

        assertFalse(invalidRequest.isValid());

        // 无效请求（topK <= 0）
        RetrieveRequest invalidRequest2 = RetrieveRequest.builder()
                .query("test")
                .topK(0)
                .build();

        assertFalse(invalidRequest2.isValid());
    }

    @Test
    void testRetrieveRequestPresets() {
        // 测试预设配置
        RetrieveRequest bm25Request = RetrieveRequest.bm25Config();
        assertEquals(RetrieveRequest.RetrieveType.BM25, bm25Request.getRetrieveType());

        RetrieveRequest vectorRequest = RetrieveRequest.vectorConfig();
        assertEquals(RetrieveRequest.RetrieveType.VECTOR, vectorRequest.getRetrieveType());

        RetrieveRequest hybridRequest = RetrieveRequest.hybridConfig();
        assertEquals(RetrieveRequest.RetrieveType.HYBRID, hybridRequest.getRetrieveType());
    }

    @Test
    void testBM25IndexStats() {
        BM25RetrieveStrategy.IndexStats stats = bm25Strategy.getIndexStats();

        assertNotNull(stats);
        assertEquals(5, stats.getTotalDocuments());
        assertTrue(stats.getAvgDocLength() > 0);
        assertTrue(stats.getTotalUniqueTerms() > 0);

        System.out.println("BM25索引统计: " + stats);
    }

    @Test
    void testBM25Parameters() {
        // 设置自定义BM25参数
        bm25Strategy.setBM25Params(1.5, 0.8);

        RetrieveRequest request = RetrieveRequest.builder()
                .topK(3)
                .build();

        List<RetrieveResult> results = bm25Strategy.retrieve("Java编程", request, null);

        assertNotNull(results);
    }

    @Test
    void testEmptyQuery() {
        RetrieveRequest request = RetrieveRequest.builder()
                .topK(3)
                .build();

        List<RetrieveResult> results = bm25Strategy.retrieve("", request, null);

        assertNotNull(results);
        assertTrue(results.isEmpty());
    }

    @Test
    void testNonExistentQuery() {
        RetrieveRequest request = RetrieveRequest.builder()
                .topK(3)
                .build();

        // 查询不存在的词
        List<RetrieveResult> results = bm25Strategy.retrieve("不存在的内容xyz123", request, null);

        assertNotNull(results);
        assertTrue(results.isEmpty() || results.get(0).getScore() == 0.0);
    }

    @Test
    void testMultiWordQuery() {
        RetrieveRequest request = RetrieveRequest.builder()
                .topK(5)
                .build();

        // 多词查询
        List<RetrieveResult> results = bm25Strategy.retrieve("编程语言", request, null);

        assertNotNull(results);
        assertTrue(results.size() > 0);

        System.out.println("多词查询结果:");
        results.forEach(r -> System.out.println("  " + r.getDocId() + ": " + r.getScore()));
    }

    @Test
    void testCoordinator() {
        if (coordinator == null) {
            System.out.println("HybridRetrieveCoordinator未配置，跳过测试");
            return;
        }

        List<String> availableStrategies = coordinator.getAvailableStrategies();

        assertNotNull(availableStrategies);
        assertFalse(availableStrategies.isEmpty());

        System.out.println("可用策略: " + availableStrategies);

        Map<String, String> descriptions = coordinator.getStrategyDescriptions();
        assertNotNull(descriptions);

        descriptions.forEach((name, desc) ->
            System.out.println("  " + name + ": " + desc));
    }

    @Test
    void testChineseTokenization() {
        // 测试中文分词
        String text = "Java是一种广泛使用的编程语言";
        Map<String, String> docs = new HashMap<>();
        docs.put("test", text);

        bm25Strategy.indexBatch(docs);

        BM25RetrieveStrategy.IndexStats stats = bm25Strategy.getIndexStats();

        assertNotNull(stats);
        assertTrue(stats.getTotalUniqueTerms() > 0);

        System.out.println("中文分词测试 - 文档数: " + stats.getTotalDocuments() +
                         ", 唯一词数: " + stats.getTotalUniqueTerms());
    }
}
