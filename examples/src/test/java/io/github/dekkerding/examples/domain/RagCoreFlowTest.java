package io.github.dekkerding.examples.domain;

import io.github.dekkerding.examples.domain.chunk.model.ChunkConfig;
import io.github.dekkerding.examples.domain.chunk.model.ChunkNode;
import io.github.dekkerding.examples.domain.chunk.service.ChunkSplitService;
import io.github.dekkerding.examples.domain.chunk.strategy.impl.RecursiveChunkStrategy;
import io.github.dekkerding.examples.domain.chunk.strategy.impl.SeparatorChunkStrategy;
import io.github.dekkerding.examples.domain.retrieval.model.RetrieveContext;
import io.github.dekkerding.examples.domain.retrieval.model.RetrieveRequest;
import io.github.dekkerding.examples.domain.retrieval.model.RetrieveResult;
import io.github.dekkerding.examples.domain.retrieval.service.RetrieveService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * RAG核心流程测试 - 展示分块、向量化和检索的完整流程
 *
 * <p>测试场景：</p>
 * <ul>
 *   <li>文本分块</li>
 *   <li>向量化（模拟）</li>
 *   <li>检索（模拟）</li>
 *   <li>完整RAG流程</li>
 * </ul>
 *
 * @author Notebook RAG Team
 * @since 1.0.0
 */
@SpringBootTest
public class RagCoreFlowTest {

    private ChunkSplitService chunkSplitService;
    private RetrieveService retrieveService;

    private static final String SAMPLE_TEXT =
            "第一章：人工智能概述\n\n" +
            "人工智能（Artificial Intelligence，简称AI）是计算机科学的一个分支，" +
            "它企图了解智能的实质，并生产出一种新的能以人类智能相似的方式做出反应的智能机器。" +
            "该领域的研究包括机器人、语言识别、图像识别、自然语言处理和专家系统等。\n\n" +
            "人工智能的发展可以分为几个阶段：符号主义AI、连接主义AI和行为主义AI。" +
            "每个阶段都有其独特的特点和贡献。\n\n" +
            "第二章：机器学习基础\n\n" +
            "机器学习是人工智能的核心，它使计算机能够从数据中学习，" +
            "而无需明确编程。机器学习算法可以自动改进，通过经验积累变得更加准确。\n\n" +
            "主要的机器学习类型包括：监督学习、无监督学习和强化学习。" +
            "监督学习使用标记数据进行训练，无监督学习发现数据中的模式，" +
            "强化学习通过奖励和惩罚机制学习最优策略。";

    @BeforeEach
    public void setUp() {
        // 初始化分块服务
        chunkSplitService = new ChunkSplitService();
        chunkSplitService.init();

        // 注册策略
        chunkSplitService.registerStrategy(new SeparatorChunkStrategy());
        chunkSplitService.registerStrategy(new RecursiveChunkStrategy());
    }

    @Test
    public void testChunkSplit() {
        System.out.println("\n=== 测试文本分块 ===");

        // 使用默认配置进行分块
        List<ChunkNode> chunks = chunkSplitService.split(SAMPLE_TEXT);

        assertNotNull(chunks);
        assertTrue(chunks.size() > 0, "应该产生至少一个分块");

        System.out.println("分块数量: " + chunks.size());

        // 打印前3个分块
        for (int i = 0; i < Math.min(3, chunks.size()); i++) {
            ChunkNode chunk = chunks.get(i);
            System.out.println("\n分块 #" + (i + 1) + ":");
            System.out.println("  长度: " + chunk.getLength());
            System.out.println("  估算Token: " + chunk.estimateTokens());
            System.out.println("  类型: " + chunk.getMetadata().getChunkType());
            System.out.println("  摘要: " + chunk.getSummary());
        }

        // 获取统计信息
        ChunkSplitService.ChunkStatistics stats = chunkSplitService.getStatistics(chunks);
        System.out.println("\n分块统计:");
        System.out.println("  " + stats);
    }

    @Test
    public void testChunkWithCustomConfig() {
        System.out.println("\n=== 测试自定义配置分块 ===");

        // 创建自定义配置
        ChunkConfig config = ChunkConfig.builder()
                .chunkSize(200)
                .chunkOverlap(20)
                .hardMaxSize(400)
                .softMinSize(10)
                .keepSeparator(false)
                .build();

        List<ChunkNode> chunks = chunkSplitService.split(SAMPLE_TEXT, "recursive", config);

        assertNotNull(chunks);
        System.out.println("使用自定义配置分块数量: " + chunks.size());

        // 验证分块大小
        for (ChunkNode chunk : chunks) {
            assertTrue(chunk.getLength() <= config.getHardMaxSize(),
                    "分块大小不应超过硬限制");
        }
    }

    @Test
    public void testChunkWithSeparatorStrategy() {
        System.out.println("\n=== 测试分隔符分块策略 ===");

        List<ChunkNode> chunks = chunkSplitService.split(SAMPLE_TEXT, "separator");

        assertNotNull(chunks);
        System.out.println("分隔符策略分块数量: " + chunks.size());

        // 打印每个分块的类型
        for (int i = 0; i < chunks.size(); i++) {
            System.out.println("分块 #" + (i + 1) + ": 类型=" +
                    chunks.get(i).getMetadata().getChunkType());
        }
    }

    @Test
    public void testRetrieveFlow() {
        System.out.println("\n=== 测试检索流程 ===");

        // 1. 先分块
        List<ChunkNode> chunks = chunkSplitService.split(SAMPLE_TEXT);

        // 2. 模拟检索（实际应该使用真正的检索服务）
        List<RetrieveResult> results = simulateRetrieve("机器学习", chunks);

        assertNotNull(results);
        assertTrue(results.size() > 0, "应该检索到结果");

        System.out.println("检索结果数量: " + results.size());

        // 打印前3个结果
        for (int i = 0; i < Math.min(3, results.size()); i++) {
            RetrieveResult result = results.get(i);
            System.out.println("\n结果 #" + (i + 1) + ":");
            System.out.println("  文档ID: " + result.getDocId());
            System.out.println("  分数: " + String.format("%.4f", result.getScore()));
            System.out.println("  来源: " + result.getSource());
            System.out.println("  摘要: " + result.getSummary());
        }
    }

    @Test
    public void testCompleteRagFlow() {
        System.out.println("\n=== 测试完整RAG流程 ===");

        String query = "什么是机器学习？";

        // 步骤1: 分块
        System.out.println("\n步骤1: 文本分块");
        List<ChunkNode> chunks = chunkSplitService.split(SAMPLE_TEXT);
        System.out.println("  产生 " + chunks.size() + " 个分块");

        // 步骤2: 向量化（模拟）
        System.out.println("\n步骤2: 向量化（模拟）");
        List<float[]> embeddings = simulateEmbedding(chunks);
        System.out.println("  完成 " + embeddings.size() + " 个向量化");

        // 步骤3: 检索
        System.out.println("\n步骤3: 检索");
        List<RetrieveResult> results = simulateRetrieve(query, chunks);
        System.out.println("  检索到 " + results.size() + " 个结果");

        // 步骤4: 创建检索上下文
        System.out.println("\n步骤4: 检索上下文");
        RetrieveContext context = RetrieveContext.of(query, RetrieveRequest.defaultConfig());
        context.addResults(results);
        context.markEnd();
        context.sortByScore();
        context.truncate(3);

        System.out.println("  上下文统计:");
        System.out.println("    " + context.getStatistics());

        // 步骤5: 获取Top结果
        System.out.println("\n步骤5: Top 3 检索结果");
        for (int i = 0; i < Math.min(3, context.getResults().size()); i++) {
            RetrieveResult result = context.getResults().get(i);
            System.out.println("\n  排名 #" + (i + 1) + ":");
            System.out.println("    分数: " + String.format("%.4f", result.getScore()));
            System.out.println("    内容: " + result.getSummary());
        }
    }

    @Test
    public void testMultiWayRetrieve() {
        System.out.println("\n=== 测试多路检索融合 ===");

        // 1. 分块
        List<ChunkNode> chunks = chunkSplitService.split(SAMPLE_TEXT);

        // 2. 模拟多路检索
        List<RetrieveResult> bm25Results = simulateBM25Retrieve("机器学习", chunks);
        List<RetrieveResult> vectorResults = simulateVectorRetrieve("机器学习", chunks);

        System.out.println("BM25检索结果: " + bm25Results.size());
        System.out.println("向量检索结果: " + vectorResults.size());

        // 3. 融合结果
        RetrieveContext context = RetrieveContext.of("机器学习");
        context.addResults(bm25Results);
        context.mergeResults(vectorResults);
        context.sortByScore();

        System.out.println("\n融合后结果: " + context.getResults().size());

        // 打印融合后的Top结果
        for (int i = 0; i < Math.min(3, context.getResults().size()); i++) {
            RetrieveResult result = context.getResults().get(i);
            System.out.println("\n  #" + (i + 1) + ": " + result.getSource() +
                    " - 分数: " + String.format("%.4f", result.getScore()) +
                    " - " + result.getSummary());
        }
    }

    // ===== 模拟方法 =====

    /**
     * 模拟向量化
     */
    private List<float[]> simulateEmbedding(List<ChunkNode> chunks) {
        List<float[]> embeddings = new ArrayList<>();
        for (ChunkNode chunk : chunks) {
            // 模拟生成1536维向量
            float[] vector = new float[1536];
            for (int i = 0; i < vector.length; i++) {
                vector[i] = (float) Math.random();
            }
            embeddings.add(vector);
        }
        return embeddings;
    }

    /**
     * 模拟检索
     */
    private List<RetrieveResult> simulateRetrieve(String query, List<ChunkNode> chunks) {
        List<RetrieveResult> results = new ArrayList<>();

        for (int i = 0; i < chunks.size(); i++) {
            ChunkNode chunk = chunks.get(i);

            // 简单的匹配度计算
            double score = 0.0;
            if (chunk.getContent().contains(query)) {
                score = 0.9;
            } else {
                // 计算词重叠度
                String[] queryWords = query.split("\\s+");
                String[] chunkWords = chunk.getContent().split("\\s+");
                int overlap = 0;
                for (String qw : queryWords) {
                    for (String cw : chunkWords) {
                        if (cw.contains(qw)) {
                            overlap++;
                            break;
                        }
                    }
                }
                score = (double) overlap / queryWords.length;
            }

            if (score > 0.3) {
                results.add(RetrieveResult.builder()
                        .docId("chunk_" + i)
                        .text(chunk.getContent())
                        .score(score)
                        .source("hybrid")
                        .chunkIndex(i)
                        .build());
            }
        }

        // 按分数排序
        results.sort((a, b) -> Double.compare(b.getScore(), a.getScore()));

        return results;
    }

    /**
     * 模拟BM25检索
     */
    private List<RetrieveResult> simulateBM25Retrieve(String query, List<ChunkNode> chunks) {
        List<RetrieveResult> results = new ArrayList<>();

        for (int i = 0; i < chunks.size(); i++) {
            ChunkNode chunk = chunks.get(i);
            double score = 0.0;

            // 简化的BM25计算
            String[] queryWords = query.split("");
            String[] chunkWords = chunk.getContent().split("");

            for (String qw : queryWords) {
                for (String cw : chunkWords) {
                    if (qw.equals(cw)) {
                        score += 0.1;
                    }
                }
            }

            if (score > 0.2) {
                results.add(RetrieveResult.builder()
                        .docId("chunk_" + i)
                        .text(chunk.getContent())
                        .score(score)
                        .source("bm25")
                        .chunkIndex(i)
                        .build());
            }
        }

        return results;
    }

    /**
     * 模拟向量检索
     */
    private List<RetrieveResult> simulateVectorRetrieve(String query, List<ChunkNode> chunks) {
        List<RetrieveResult> results = new ArrayList<>();

        for (int i = 0; i < chunks.size(); i++) {
            ChunkNode chunk = chunks.get(i);

            // 模拟向量相似度
            double score = 0.0;
            if (chunk.getContent().contains(query)) {
                score = 0.85 + Math.random() * 0.1; // 0.85-0.95
            } else if (chunk.getContent().contains("学习") || chunk.getContent().contains("智能")) {
                score = 0.6 + Math.random() * 0.2; // 0.6-0.8
            }

            if (score > 0.5) {
                results.add(RetrieveResult.builder()
                        .docId("chunk_" + i)
                        .text(chunk.getContent())
                        .score(score)
                        .source("vector")
                        .chunkIndex(i)
                        .build());
            }
        }

        return results;
    }
}
