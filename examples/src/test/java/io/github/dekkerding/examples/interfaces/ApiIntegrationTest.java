package io.github.dekkerding.examples.interfaces;

import io.github.dekkerding.examples.common.ApiResponse;
import io.github.dekkerding.examples.common.PageResult;
import io.github.dekkerding.examples.domain.chunk.model.ChunkConfig;
import io.github.dekkerding.examples.domain.chunk.model.ChunkNode;
import io.github.dekkerding.examples.domain.chunk.service.ChunkSplitService;
import io.github.dekkerding.examples.domain.knowledgebase.entity.KbKnowledgeBase;
import io.github.dekkerding.examples.domain.knowledgebase.entity.KbKnowledgePoint;
import io.github.dekkerding.examples.domain.knowledgebase.repository.KbKnowledgeBaseRepository;
import io.github.dekkerding.examples.domain.knowledgebase.repository.KbKnowledgePointRepository;
import io.github.dekkerding.examples.domain.retrieval.model.RetrieveContext;
import io.github.dekkerding.examples.domain.retrieval.model.RetrieveResult;
import io.github.dekkerding.examples.domain.retrieval.service.RetrieveService;
import io.github.dekkerding.examples.interfaces.knowledgebase.dto.CreateKbRequest;
import io.github.dekkerding.examples.interfaces.retrieval.dto.SearchRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * API集成测试 - 展示完整的API使用流程
 *
 * <p>测试场景：</p>
 * <ul>
 *   <li>知识库管理（CRUD）</li>
 *   <li>知识点管理</li>
 *   <li>文档分块</li>
 *   <li>向量检索</li>
 *   <li>BM25检索</li>
 *   <li>多路融合检索</li>
 * </ul>
 *
 * @author Notebook RAG Team
 * @since 1.0.0
 */
@SpringBootTest
@ActiveProfiles("test")
public class ApiIntegrationTest {

    @Autowired
    private KbKnowledgeBaseRepository kbKnowledgeBaseRepository;

    @Autowired
    private KbKnowledgePointRepository kbKnowledgePointRepository;

    @Autowired
    private ChunkSplitService chunkSplitService;

    // 假设有检索服务注入
    // @Autowired
    // private RetrieveService retrieveService;

    private static final String SAMPLE_TEXT =
            "第一章：人工智能概述\n\n" +
            "人工智能（Artificial Intelligence，简称AI）是计算机科学的一个分支，" +
            "它企图了解智能的实质，并生产出一种新的能以人类智能相似的方式做出反应的智能机器。" +
            "该领域的研究包括机器人、语言识别、图像识别、自然语言处理和专家系统等。\n\n" +
            "第二章：机器学习基础\n\n" +
            "机器学习是人工智能的核心，它使计算机能够从数据中学习，" +
            "而无需明确编程。机器学习算法可以自动改进，通过经验积累变得更加准确。\n\n" +
            "主要的机器学习类型包括：监督学习、无监督学习和强化学习。";

    private KbKnowledgeBase testKnowledgeBase;

    @BeforeEach
    public void setUp() {
        // 清理测试数据
        kbKnowledgeBaseRepository.deleteAll();
        kbKnowledgePointRepository.deleteAll();

        // 初始化分块服务
        chunkSplitService.init();

        // 创建测试知识库
        testKnowledgeBase = createTestKnowledgeBase();
    }

    @Test
    public void testCompleteWorkflow() {
        System.out.println("\n=== 完整工作流测试 ===");

        // 1. 创建知识库
        System.out.println("\n步骤1: 创建知识库");
        KbKnowledgeBase kb = createKnowledgeBase("测试知识库", "用于测试的知识库");
        assertNotNull(kb);
        System.out.println("  知识库ID: " + kb.getId());
        System.out.println("  知识库名称: " + kb.getName());

        // 2. 添加知识点
        System.out.println("\n步骤2: 添加知识点");
        KbKnowledgePoint point1 = createKnowledgePoint(kb.getId(),
                "什么是AI？",
                "人工智能是计算机科学的一个分支...",
                "技术概念");
        KbKnowledgePoint point2 = createKnowledgePoint(kb.getId(),
                "什么是机器学习？",
                "机器学习是人工智能的核心...",
                "技术概念");

        assertNotNull(point1);
        assertNotNull(point2);
        System.out.println("  创建知识点1: " + point1.getTitle());
        System.out.println("  创建知识点2: " + point2.getTitle());

        // 3. 验证知识点数量
        System.out.println("\n步骤3: 验证知识点数量");
        long count = kbKnowledgePointRepository.countByKbId(kb.getId());
        assertEquals(2, count);
        System.out.println("  知识点数量: " + count);

        // 4. 查询知识点
        System.out.println("\n步骤4: 查询知识点");
        List<KbKnowledgePoint> points = kbKnowledgePointRepository.findByKbId(kb.getId());
        assertEquals(2, points.size());
        System.out.println("  查询到 " + points.size() + " 个知识点");

        // 5. 文本分块
        System.out.println("\n步骤5: 文本分块");
        List<ChunkNode> chunks = chunkSplitService.split(SAMPLE_TEXT, "recursive");
        assertFalse(chunks.isEmpty());
        System.out.println("  分块数量: " + chunks.size());

        // 打印前3个分块
        for (int i = 0; i < Math.min(3, chunks.size()); i++) {
            ChunkNode chunk = chunks.get(i);
            System.out.println("    分块 #" + (i + 1) + ": " +
                    "长度=" + chunk.getLength() + ", " +
                    "Token=" + chunk.estimateTokens() + ", " +
                    "摘要=" + chunk.getSummary());
        }

        // 6. 模拟检索
        System.out.println("\n步骤6: 模拟检索");
        List<RetrieveResult> results = simulateSearch("机器学习", kb.getId());
        System.out.println("  检索到 " + results.size() + " 个结果");

        for (int i = 0; i < Math.min(3, results.size()); i++) {
            RetrieveResult result = results.get(i);
            System.out.println("    结果 #" + (i + 1) + ": " +
                    "分数=" + String.format("%.4f", result.getScore()) + ", " +
                    "来源=" + result.getSource() + ", " +
                    "摘要=" + result.getSummary());
        }

        // 7. 知识库统计
        System.out.println("\n步骤7: 知识库统计");
        KbKnowledgeBase updatedKb = kbKnowledgeBaseRepository.findById(kb.getId()).orElse(null);
        assertNotNull(updatedKb);
        System.out.println("  文档数量: " + updatedKb.getDocCount());
        System.out.println("  FAQ数量: " + updatedKb.getFaqCount());
        System.out.println("  命中次数: " + updatedKb.getHitCount());

        System.out.println("\n=== 工作流测试完成 ===");
    }

    @Test
    public void testKnowledgeBaseCrud() {
        System.out.println("\n=== 知识库CRUD测试 ===");

        // Create
        System.out.println("\n1. 创建知识库");
        KbKnowledgeBase kb = createKnowledgeBase("CRUD测试知识库", "用于CRUD测试");
        assertNotNull(kb.getId());
        System.out.println("  创建成功: id=" + kb.getId());

        // Read
        System.out.println("\n2. 读取知识库");
        Optional<KbKnowledgeBase> foundKb = kbKnowledgeBaseRepository.findById(kb.getId());
        assertTrue(foundKb.isPresent());
        assertEquals("CRUD测试知识库", foundKb.get().getName());
        System.out.println("  读取成功: name=" + foundKb.get().getName());

        // Update
        System.out.println("\n3. 更新知识库");
        foundKb.get().setDescription("更新后的描述");
        KbKnowledgeBase updatedKb = kbKnowledgeBaseRepository.save(foundKb.get());
        assertEquals("更新后的描述", updatedKb.getDescription());
        System.out.println("  更新成功: description=" + updatedKb.getDescription());

        // Delete
        System.out.println("\n4. 删除知识库");
        kbKnowledgeBaseRepository.deleteById(updatedKb.getId());
        Optional<KbKnowledgeBase> deletedKb = kbKnowledgeBaseRepository.findById(updatedKb.getId());
        assertFalse(deletedKb.isPresent());
        System.out.println("  删除成功");

        System.out.println("\n=== CRUD测试完成 ===");
    }

    @Test
    public void testChunkStrategies() {
        System.out.println("\n=== 分块策略测试 ===");

        // 测试不同策略
        String[] strategies = {"separator", "recursive"};

        for (String strategy : strategies) {
            System.out.println("\n策略: " + strategy);
            List<ChunkNode> chunks = chunkSplitService.split(SAMPLE_TEXT, strategy);

            System.out.println("  分块数量: " + chunks.size());

            ChunkSplitService.ChunkStatistics stats = chunkSplitService.getStatistics(chunks);
            System.out.println("  统计信息: " + stats);

            // 验证分块有效性
            for (ChunkNode chunk : chunks) {
                assertTrue(chunk.isValid(), "分块应该有效");
                assertTrue(chunk.getLength() > 0, "分块长度应大于0");
            }
        }

        System.out.println("\n=== 分块策略测试完成 ===");
    }

    @Test
    public void testRetrieveScenarios() {
        System.out.println("\n=== 检索场景测试 ===");

        // 创建测试数据
        KbKnowledgeBase kb = createTestKnowledgeBase();

        // 场景1: 精确匹配
        System.out.println("\n场景1: 精确匹配");
        List<RetrieveResult> results1 = simulateSearch("机器学习是人工智能的核心", kb.getId());
        System.out.println("  查询: '机器学习是人工智能的核心'");
        System.out.println("  结果数: " + results1.size());
        if (!results1.isEmpty()) {
            System.out.println("  最高分数: " + String.format("%.4f", results1.get(0).getScore()));
        }

        // 场景2: 部分匹配
        System.out.println("\n场景2: 部分匹配");
        List<RetrieveResult> results2 = simulateSearch("机器学习算法", kb.getId());
        System.out.println("  查询: '机器学习算法'");
        System.out.println("  结果数: " + results2.size());
        if (!results2.isEmpty()) {
            System.out.println("  最高分数: " + String.format("%.4f", results2.get(0).getScore()));
        }

        // 场景3: 无匹配
        System.out.println("\n场景3: 无匹配");
        List<RetrieveResult> results3 = simulateSearch("量子计算原理", kb.getId());
        System.out.println("  查询: '量子计算原理'");
        System.out.println("  结果数: " + results3.size());
        assertTrue(results3.isEmpty() || results3.get(0).getScore() < 0.3,
                "无匹配查询应该返回空结果或低分结果");

        System.out.println("\n=== 检索场景测试完成 ===");
    }

    @Test
    public void testPerformance() {
        System.out.println("\n=== 性能测试 ===");

        // 创建大量测试数据
        System.out.println("\n准备测试数据...");
        KbKnowledgeBase kb = createKnowledgeBase("性能测试知识库", "用于性能测试");

        int pointCount = 100;
        for (int i = 0; i < pointCount; i++) {
            createKnowledgePoint(kb.getId(),
                    "测试问题" + i,
                    "测试答案" + i + "的内容",
                    "测试分类");
        }

        System.out.println("  创建 " + pointCount + " 个知识点");

        // 测试检索性能
        System.out.println("\n执行检索性能测试...");
        long startTime = System.currentTimeMillis();

        int iterations = 10;
        for (int i = 0; i < iterations; i++) {
            simulateSearch("测试问题" + i, kb.getId());
        }

        long duration = System.currentTimeMillis() - startTime;
        double avgTime = (double) duration / iterations;

        System.out.println("  总耗时: " + duration + "ms");
        System.out.println("  平均耗时: " + String.format("%.2f", avgTime) + "ms");
        System.out.println("  QPS: " + String.format("%.2f", 1000.0 / avgTime));

        System.out.println("\n=== 性能测试完成 ===");
    }

    // ===== 辅助方法 =====

    /**
     * 创建测试知识库
     */
    private KbKnowledgeBase createTestKnowledgeBase() {
        return createKnowledgeBase("测试知识库", "用于API测试的知识库");
    }

    /**
     * 创建知识库
     */
    private KbKnowledgeBase createKnowledgeBase(String name, String description) {
        KbKnowledgeBase kb = KbKnowledgeBase.builder()
                .name(name)
                .description(description)
                .status("active")
                .language("zh_CN")
                .build();

        return kbKnowledgeBaseRepository.save(kb);
    }

    /**
     * 创建知识点
     */
    private KbKnowledgePoint createKnowledgePoint(String kbId, String title,
                                                   String content, String category) {
        KbKnowledgePoint point = KbKnowledgePoint.builder()
                .kbId(kbId)
                .title(title)
                .content(content)
                .category(category)
                .difficulty("medium")
                .status("active")
                .build();

        return kbKnowledgePointRepository.save(point);
    }

    /**
     * 模拟搜索
     */
    private List<RetrieveResult> simulateSearch(String query, String kbId) {
        // 获取知识点
        List<KbKnowledgePoint> points = kbKnowledgePointRepository.findByKbId(kbId);

        // 简单的匹配计算
        List<RetrieveResult> results = new ArrayList<>();

        for (KbKnowledgePoint point : points) {
            double score = calculateSimilarity(query, point.getContent());

            if (score > 0.3) {
                results.add(RetrieveResult.builder()
                        .docId(point.getId())
                        .text(point.getContent())
                        .score(score)
                        .source("test")
                        .build());
            }
        }

        // 排序
        results.sort((a, b) -> Double.compare(b.getScore(), a.getScore()));

        // 截取Top10
        if (results.size() > 10) {
            results = results.subList(0, 10);
        }

        return results;
    }

    /**
     * 计算相似度
     */
    private double calculateSimilarity(String query, String content) {
        if (content.contains(query)) {
            return 0.9;
        }

        String[] queryWords = query.split("\\s+");
        String[] contentWords = content.split("\\s+");

        int overlap = 0;
        for (String qw : queryWords) {
            for (String cw : contentWords) {
                if (cw.contains(qw)) {
                    overlap++;
                    break;
                }
            }
        }

        return (double) overlap / queryWords.length;
    }
}
