package io.github.dekkerding.examples.domain.chunk.strategy.impl;

import io.github.dekkerding.examples.domain.chunk.model.ChunkConfig;
import io.github.dekkerding.examples.domain.chunk.model.ChunkNode;
import io.github.dekkerding.examples.domain.chunk.model.ChunkNode.ChunkMetadata;
import io.github.dekkerding.examples.domain.chunk.model.ChunkNode.ChunkType;
import io.github.dekkerding.examples.domain.chunk.strategy.ChunkStrategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 语义分块策略 - 基于语义相似度的智能分块
 *
 * <p>语义分块通过分析文本的语义连贯性，在语义边界处进行分割。</p>
 *
 * <p>核心思想：</p>
 * <ul>
 *   <li>将文本分割成候选句子</li>
 *   <li>计算相邻句子的语义相似度</li>
 *   <li>在相似度低于阈值处进行分割</li>
 *   <li>合并过小的分块</li>
 * </ul>
 *
 * <p>优势：</p>
 * <ul>
 *   <li>保持语义完整性</li>
 *   <li>避免在句子中间分割</li>
 *   <li>适合长文档处理</li>
 * </ul>
 *
 * <p>工作流程：</p>
 * <pre>
 * 1. 按句子分割文本
 * 2. 计算句子间的语义相似度
 * 3. 识别相似度低于阈值的边界点
 * 4. 在边界点处进行分组
 * 5. 合并过小的组
 * 6. 创建分块节点
 * </pre>
 *
 * @author Notebook RAG Team
 * @since 1.0.0
 */
@Slf4j
@Component
public class SemanticChunkStrategy implements ChunkStrategy {

    /**
     * 默认相似度阈值
     */
    private static final double DEFAULT_SIMILARITY_THRESHOLD = 0.6;

    /**
     * 默认句子分隔符
     */
    private static final String[] SENTENCE_DELIMITERS = {
            "。", "！", "？", ".", "!", "?", "\n", ";", "；"
    };

    @Override
    public List<ChunkNode> split(String text, ChunkConfig config) {
        log.debug("开始语义分块: textLength={}, config={}",
                text != null ? text.length() : 0, config);

        // 验证输入
        if (!isValidInput(text)) {
            return Collections.emptyList();
        }

        // 预处理文本
        String processedText = preprocessText(text, config);

        // 1. 分割成句子
        List<String> sentences = splitIntoSentences(processedText);

        if (sentences.isEmpty()) {
            return Collections.emptyList();
        }

        if (sentences.size() == 1) {
            // 只有一个句子，直接返回
            return Collections.singletonList(createChunkNode(
                    processedText, 0, config));
        }

        // 2. 计算句子相似度
        double[] similarities = calculateSentenceSimilarities(sentences);

        // 3. 识别分割点
        double threshold = config.getSimilarityThreshold() > 0
                ? config.getSimilarityThreshold()
                : DEFAULT_SIMILARITY_THRESHOLD;
        List<Integer> splitPoints = identifySplitPoints(similarities, threshold);

        // 4. 创建分块
        List<ChunkNode> chunks = createChunksFromSentences(sentences, splitPoints, config);

        // 5. 后处理
        chunks = postProcessChunks(chunks, config);

        // 6. 添加元数据
        enrichMetadata(chunks, config);

        log.debug("语义分块完成: chunks={}", chunks.size());
        return chunks;
    }

    @Override
    public String getStrategyName() {
        return "semantic";
    }

    @Override
    public String getDescription() {
        return "基于语义相似度的智能分块，保持语义完整性";
    }

    /**
     * 分割成句子
     */
    private List<String> splitIntoSentences(String text) {
        List<String> sentences = new ArrayList<>();

        // 使用正则表达式分割
        String[] parts = text.split(
                "(?<=[。！？\\.!?\\n；;])\\s*"
        );

        for (String part : parts) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty() && trimmed.length() > 2) {
                // 过滤过短的片段
                sentences.add(trimmed);
            }
        }

        return sentences;
    }

    /**
     * 计算句子间的语义相似度
     * 这里使用简化的相似度计算（实际应该使用embedding）
     */
    private double[] calculateSentenceSimilarities(List<String> sentences) {
        double[] similarities = new double[sentences.size() - 1];

        for (int i = 0; i < similarities.length; i++) {
            similarities[i] = calculateSimpleSimilarity(
                    sentences.get(i),
                    sentences.get(i + 1)
            );
        }

        return similarities;
    }

    /**
     * 简化的相似度计算（基于词重叠）
     * 实际应该使用embedding向量计算余弦相似度
     */
    private double calculateSimpleSimilarity(String sentence1, String sentence2) {
        // 分词（简单实现）
        Set<String> words1 = extractWords(sentence1);
        Set<String> words2 = extractWords(sentence2);

        if (words1.isEmpty() || words2.isEmpty()) {
            return 0.0;
        }

        // Jaccard相似度
        Set<String> intersection = new HashSet<>(words1);
        intersection.retainAll(words2);

        Set<String> union = new HashSet<>(words1);
        union.addAll(words2);

        return (double) intersection.size() / union.size();
    }

    /**
     * 提取词语（简单实现）
     */
    private Set<String> extractWords(String sentence) {
        Set<String> words = new HashSet<>();

        // 提取中文词（按字符）
        for (int i = 0; i < sentence.length(); i++) {
            char c = sentence.charAt(i);
            if (isChineseChar(c)) {
                words.add(String.valueOf(c));
            }
        }

        // 提取英文词
        String[] englishWords = sentence.split("[^a-zA-Z0-9]+");
        for (String word : englishWords) {
            if (word.length() > 2) {
                words.add(word.toLowerCase());
            }
        }

        return words;
    }

    /**
     * 判断是否为中文字符
     */
    private boolean isChineseChar(char c) {
        return c >= 0x4E00 && c <= 0x9FA5;
    }

    /**
     * 识别分割点
     */
    private List<Integer> identifySplitPoints(double[] similarities, double threshold) {
        List<Integer> splitPoints = new ArrayList<>();

        for (int i = 0; i < similarities.length; i++) {
            if (similarities[i] < threshold) {
                splitPoints.add(i + 1); // 在i和i+1之间分割
            }
        }

        return splitPoints;
    }

    /**
     * 从句子创建分块
     */
    private List<ChunkNode> createChunksFromSentences(
            List<String> sentences,
            List<Integer> splitPoints,
            ChunkConfig config) {

        List<ChunkNode> chunks = new ArrayList<>();

        int start = 0;
        for (int splitPoint : splitPoints) {
            List<String> chunkSentences = sentences.subList(start, splitPoint);
            String chunkText = String.join(" ", chunkSentences);

            chunks.add(createChunkNode(chunkText, chunks.size(), config));

            start = splitPoint;
        }

        // 添加最后一个分块
        if (start < sentences.size()) {
            List<String> chunkSentences = sentences.subList(start, sentences.size());
            String chunkText = String.join(" ", chunkSentences);

            chunks.add(createChunkNode(chunkText, chunks.size(), config));
        }

        return chunks;
    }

    /**
     * 创建分块节点
     */
    private ChunkNode createChunkNode(String content, int position, ChunkConfig config) {
        return ChunkNode.builder()
                .id(UUID.randomUUID().toString())
                .content(content)
                .position(position)
                .metadata(ChunkMetadata.builder()
                        .chunkType(ChunkType.TEXT)
                        .charCount(content.length())
                        .strategyName(getStrategyName())
                        .build())
                .build();
    }

    /**
     * 丰富元数据
     */
    private void enrichMetadata(List<ChunkNode> chunks, ChunkConfig config) {
        for (int i = 0; i < chunks.size(); i++) {
            ChunkNode chunk = chunks.get(i);
            ChunkMetadata metadata = chunk.getMetadata();

            // 估算token数
            metadata.setEstimatedTokens(chunk.estimateTokens());

            // 计算位置
            int startIndex = 0;
            for (int j = 0; j < i; j++) {
                startIndex += chunks.get(j).getLength();
            }
            metadata.setStartIndex(startIndex);
            metadata.setEndIndex(startIndex + chunk.getLength());

            // 添加额外信息
            metadata.getExtra().put("chunk_index", i);
            metadata.getExtra().put("total_chunks", chunks.size());
            metadata.getExtra().put("semantic_split", true);
        }
    }
}
