package io.github.dekkerding.examples.domain.retrieval.fusion;

import io.github.dekkerding.examples.domain.retrieval.model.RetrieveResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 结果融合服务测试
 *
 * @author Notebook RAG Team
 * @since 1.0.0
 */
@SpringBootTest
class ResultFusionServiceTest {

    @Autowired
    private ResultFusionService fusionService;

    private List<StrategyResult> mockResults;

    @BeforeEach
    void setUp() {
        // 创建模拟的策略结果
        mockResults = new ArrayList<>();

        // 策略1: BM25结果
        StrategyResult bm25Result = new StrategyResult();
        bm25Result.setStrategyName("bm25");
        bm25Result.setSuccess(true);
        bm25Result.setResults(Arrays.asList(
                createResult("doc1", "Java content", 0.9),
                createResult("doc2", "Python content", 0.8),
                createResult("doc3", "Spring content", 0.7),
                createResult("doc4", "Elasticsearch content", 0.6)
        ));

        // 策略2: 向量检索结果（部分重复）
        StrategyResult vectorResult = new StrategyResult();
        vectorResult.setStrategyName("vector");
        vectorResult.setSuccess(true);
        vectorResult.setResults(Arrays.asList(
                createResult("doc1", "Java content", 0.85),
                createResult("doc5", "Redis content", 0.75),
                createResult("doc2", "Python content", 0.65),
                createResult("doc6", "Docker content", 0.55)
        ));

        // 策略3: GraphRAG结果
        StrategyResult graphResult = new StrategyResult();
        graphResult.setStrategyName("graphrag");
        graphResult.setSuccess(true);
        graphResult.setResults(Arrays.asList(
                createResult("doc3", "Spring content", 0.7),
                createResult("doc7", "Kubernetes content", 0.6)
        ));

        mockResults.add(bm25Result);
        mockResults.add(vectorResult);
        mockResults.add(graphResult);
    }

    @Test
    void testFuseWithDefaultWeights() {
        Map<String, Double> weights = new HashMap<>();
        weights.put("bm25", 1.0);
        weights.put("vector", 1.0);
        weights.put("graphrag", 1.0);

        List<RetrieveResult> fused = fusionService.fuse(mockResults, weights);

        assertNotNull(fused);
        assertFalse(fused.isEmpty());

        // 应该包含所有唯一文档
        Set<String> docIds = new HashSet<>();
        fused.forEach(r -> docIds.add(r.getDocId()));

        assertEquals(7, docIds.size()); // doc1-doc7
        assertTrue(docIds.contains("doc1"));
        assertTrue(docIds.contains("doc5"));
        assertTrue(docIds.contains("doc7"));

        // 验证融合分数
        fused.forEach(r -> {
            assertTrue(r.getScore() > 0, "融合分数应该大于0");
            assertEquals("multiway", r.getSource());
        });

        System.out.println("融合结果数量: " + fused.size());
        fused.forEach(r -> System.out.println("  " + r.getDocId() + ": " +
                String.format("%.4f", r.getScore())));
    }

    @Test
    void testFuseWithCustomWeights() {
        Map<String, Double> weights = new HashMap<>();
        weights.put("bm25", 2.0);  // BM25权重更高
        weights.put("vector", 0.5);
        weights.put("graphrag", 0.5);

        List<RetrieveResult> fused = fusionService.fuse(mockResults, weights);

        assertNotNull(fused);

        // doc1在BM25和向量中都有，应该排名较高
        if (!fused.isEmpty()) {
            String topDoc = fused.get(0).getDocId();
            System.out.println("最高分文档: " + topDoc + " (" +
                    String.format("%.4f", fused.get(0).getScore()) + ")");
        }
    }

    @Test
    void testDeduplicateAndFuse() {
        List<List<RetrieveResult>> resultsList = new ArrayList<>();

        resultsList.add(Arrays.asList(
                createResult("doc1", "content1", 0.9),
                createResult("doc2", "content2", 0.8)
        ));

        resultsList.add(Arrays.asList(
                createResult("doc1", "content1", 0.85),
                createResult("doc3", "content3", 0.7)
        ));

        List<RetrieveResult> deduplicated = fusionService.deduplicateAndFuse(resultsList);

        assertNotNull(deduplicated);
        assertEquals(3, deduplicated.size()); // doc1, doc2, doc3

        // doc1应该保留更高的分数
        Optional<RetrieveResult> doc1 = deduplicated.stream()
                .filter(r -> "doc1".equals(r.getDocId()))
                .findFirst();

        assertTrue(doc1.isPresent());
        assertEquals(0.9, doc1.get().getScore(), 0.01);
    }

    @Test
    void testFuseWithEmptyStrategy() {
        List<StrategyResult> resultsWithEmpty = new ArrayList<>(mockResults);

        StrategyResult emptyResult = new StrategyResult();
        emptyResult.setStrategyName("empty");
        emptyResult.setSuccess(true);
        emptyResult.setResults(Collections.emptyList());

        resultsWithEmpty.add(emptyResult);

        Map<String, Double> weights = new HashMap<>();
        weights.put("bm25", 1.0);
        weights.put("vector", 1.0);
        weights.put("graphrag", 1.0);
        weights.put("empty", 1.0);

        List<RetrieveResult> fused = fusionService.fuse(resultsWithEmpty, weights);

        assertNotNull(fused);
        assertFalse(fused.isEmpty());
    }

    @Test
    void testFuseWithFailedStrategy() {
        List<StrategyResult> resultsWithFailed = new ArrayList<>(mockResults);

        StrategyResult failedResult = new StrategyResult();
        failedResult.setStrategyName("failed");
        failedResult.setSuccess(false);
        failedResult.setErrorMessage("Strategy execution failed");
        failedResult.setResults(Collections.emptyList());

        resultsWithFailed.add(failedResult);

        Map<String, Double> weights = new HashMap<>();
        weights.put("bm25", 1.0);
        weights.put("vector", 1.0);
        weights.put("graphrag", 1.0);
        weights.put("failed", 1.0);

        List<RetrieveResult> fused = fusionService.fuse(resultsWithFailed, weights);

        assertNotNull(fused);
        assertFalse(fused.isEmpty());
    }

    @Test
    void testFuseResultOrdering() {
        Map<String, Double> weights = new HashMap<>();
        weights.put("bm25", 1.0);
        weights.put("vector", 1.0);
        weights.put("graphrag", 1.0);

        List<RetrieveResult> fused = fusionService.fuse(mockResults, weights);

        assertNotNull(fused);

        // 验证结果按分数降序排列
        for (int i = 0; i < fused.size() - 1; i++) {
            assertTrue(fused.get(i).getScore() >= fused.get(i + 1).getScore(),
                    "结果应该按分数降序排列");
        }
    }

    @Test
    void testFusionMetadata() {
        Map<String, Double> weights = new HashMap<>();
        weights.put("bm25", 1.0);
        weights.put("vector", 1.0);
        weights.put("graphrag", 1.0);

        List<RetrieveResult> fused = fusionService.fuse(mockResults, weights);

        assertNotNull(fused);

        // 检查融合元数据
        fused.forEach(r -> {
            Map<String, Object> metadata = r.getMetadata();
            assertNotNull(metadata);

            // 应该包含融合分数
            assertTrue(metadata.containsKey("fusion_score"));

            // 应该包含策略排名信息
            assertTrue(metadata.containsKey("strategy_ranks"));
        });
    }

    /**
     * 创建测试结果
     */
    private RetrieveResult createResult(String docId, String text, double score) {
        return RetrieveResult.builder()
                .docId(docId)
                .text(text)
                .score(score)
                .build();
    }
}
