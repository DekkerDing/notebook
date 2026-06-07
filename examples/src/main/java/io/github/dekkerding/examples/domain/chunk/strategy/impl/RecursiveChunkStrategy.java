package io.github.dekkerding.examples.domain.chunk.strategy.impl;

import io.github.dekkerding.examples.domain.chunk.model.ChunkConfig;
import io.github.dekkerding.examples.domain.chunk.model.ChunkNode;
import io.github.dekkerding.examples.domain.chunk.model.ChunkNode.ChunkMetadata;
import io.github.dekkerding.examples.domain.chunk.strategy.ChunkStrategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 递归字符分块策略 - 按优先级递归尝试分隔符
 *
 * <p>功能：</p>
 * <ul>
 *   <li>按优先级递归尝试分隔符</li>
 *   <li>自动选择最佳分隔符</li>
 *   <li>保持分块大小均匀</li>
 *   <li>支持重叠分块</li>
 * </ul>
 *
 * <p>使用场景：</p>
 * <ul>
 *   <li>复杂文本文档</li>
 *   <li>需要保持语义完整性的场景</li>
 *   <li>不明确分隔符的文档</li>
 * </ul>
 *
 * <p>工作原理：</p>
 * <pre>
 * 1. 尝试第一个分隔符（如 "\n\n"）分割
 * 2. 如果分割后的块太大，尝试下一个分隔符（如 "\n"）
 * 3. 递归直到所有块大小合适或用完所有分隔符
 * 4. 最后按字符强制分割
 * </pre>
 *
 * @author Notebook RAG Team
 * @since 1.0.0
 */
@Slf4j
@Component
public class RecursiveChunkStrategy implements ChunkStrategy {

    /**
     * 默认分隔符优先级列表
     */
    private static final String[] DEFAULT_SEPARATORS = {
            "\n\n",      // 段落分隔
            "\n",        // 行分隔
            "。",        // 中文句号
            "！",        // 中文感叹号
            "？",        // 中文问号
            "；",        // 中文分号
            "，",        // 中文逗号
            ".",         // 英文句号
            "!",         // 英文感叹号
            "?",         // 英文问号
            ";",         // 英文分号
            ",",         // 英文逗号
            " ",         // 空格
            ""           // 按字符分割
    };

    @Override
    public List<ChunkNode> split(String text, ChunkConfig config) {
        log.debug("开始递归分块: textLength={}, config={}", text != null ? text.length() : 0, config);

        // 验证输入
        if (!isValidInput(text)) {
            log.warn("无效输入文本");
            return Collections.emptyList();
        }

        if (!isValidConfig(config)) {
            throw new IllegalArgumentException("Invalid chunk config");
        }

        // 预处理文本
        String processedText = preprocessText(text, config);

        // 获取分隔符列表
        String[] separators = config.getRecursiveSeparators();
        if (separators == null || separators.length == 0) {
            separators = DEFAULT_SEPARATORS;
        }

        // 执行递归分块
        List<ChunkNode> chunks = performRecursiveSplit(
                processedText,
                separators,
                0,
                config
        );

        // 处理重叠
        if (config.getChunkOverlap() > 0) {
            chunks = addOverlap(chunks, config);
        }

        // 后处理
        chunks = postProcessChunks(chunks, config);

        // 添加元数据
        enrichMetadata(chunks, config);

        log.debug("递归分块完成: chunks={}", chunks.size());
        return chunks;
    }

    /**
     * 递归分块核心逻辑
     *
     * @param text 待分块文本
     * @param separators 分隔符列表
     * @param separatorIndex 当前分隔符索引
     * @param config 分块配置
     * @return 分块列表
     */
    private List<ChunkNode> performRecursiveSplit(
            String text,
            String[] separators,
            int separatorIndex,
            ChunkConfig config) {

        // 如果用完所有分隔符，强制按字符分割
        if (separatorIndex >= separators.length) {
            return splitBySize(text, config);
        }

        String separator = separators[separatorIndex];

        // 空字符串表示按字符分割
        if (separator.isEmpty()) {
            return splitBySize(text, config);
        }

        // 按当前分隔符分割
        List<String> splits = splitBySeparator(text, separator);

        // 检查是否需要继续递归
        boolean needsRecursion = splits.stream()
                .anyMatch(split -> split.length() > config.getHardMaxSize());

        if (!needsRecursion && splits.size() > 1) {
            // 分块大小合适，创建节点
            return createChunkNodes(splits, config);
        }

        // 需要继续递归
        List<ChunkNode> result = new ArrayList<>();
        for (String split : splits) {
            if (split.length() > config.getHardMaxSize()) {
                // 递归处理过大的块
                result.addAll(performRecursiveSplit(
                        split,
                        separators,
                        separatorIndex + 1,
                        config
                ));
            } else if (!split.trim().isEmpty()) {
                // 大小合适的块
                result.add(createChunkNode(split, config));
            }
        }

        return result;
    }

    /**
     * 按分隔符分割文本
     */
    private List<String> splitBySeparator(String text, String separator) {
        if (separator.isEmpty()) {
            return Collections.singletonList(text);
        }

        // 特殊处理段落分隔
        if ("\n\n".equals(separator)) {
            return Arrays.asList(text.split("\\n\\n"));
        }

        // 普通分隔符
        String[] parts = text.split(Pattern.quote(separator));
        return new ArrayList<>(Arrays.asList(parts));
    }

    /**
     * 按大小分割文本
     */
    private List<ChunkNode> splitBySize(String text, ChunkConfig config) {
        List<ChunkNode> chunks = new ArrayList<>();
        int chunkSize = config.getChunkSize();

        for (int i = 0; i < text.length(); i += chunkSize) {
            int end = Math.min(i + chunkSize, text.length());
            String chunk = text.substring(i, end);
            if (!chunk.trim().isEmpty()) {
                chunks.add(createChunkNode(chunk, config));
            }
        }

        return chunks;
    }

    /**
     * 创建分块节点列表
     */
    private List<ChunkNode> createChunkNodes(List<String> splits, ChunkConfig config) {
        List<ChunkNode> chunks = new ArrayList<>();
        for (String split : splits) {
            if (!split.trim().isEmpty()) {
                chunks.add(createChunkNode(split, config));
            }
        }
        return chunks;
    }

    /**
     * 创建单个分块节点
     */
    private ChunkNode createChunkNode(String content, ChunkConfig config) {
        return ChunkNode.builder()
                .content(content.trim())
                .metadata(ChunkMetadata.builder()
                        .chunkType(io.github.dekkerding.examples.domain.chunk.model.ChunkNode.ChunkType.TEXT)
                        .charCount(content.length())
                        .build())
                .build();
    }

    /**
     * 添加分块重叠
     */
    private List<ChunkNode> addOverlap(List<ChunkNode> chunks, ChunkConfig config) {
        if (chunks.isEmpty()) {
            return chunks;
        }

        int overlap = config.getChunkOverlap();
        if (overlap <= 0) {
            return chunks;
        }

        List<ChunkNode> overlappedChunks = new ArrayList<>();

        for (int i = 0; i < chunks.size(); i++) {
            ChunkNode current = chunks.get(i);
            String content = current.getContent();

            // 添加前一个分块的重叠内容
            if (i > 0) {
                String prevContent = chunks.get(i - 1).getContent();
                int overlapStart = Math.max(0, prevContent.length() - overlap);
                String overlapText = prevContent.substring(overlapStart);
                content = overlapText + "\n\n" + content;
            }

            ChunkNode overlapped = current.copy();
            overlapped.setContent(content);
            overlapped.getMetadata().setCharCount(content.length());
            overlappedChunks.add(overlapped);
        }

        return overlappedChunks;
    }

    /**
     * 丰富元数据
     */
    private void enrichMetadata(List<ChunkNode> chunks, ChunkConfig config) {
        for (int i = 0; i < chunks.size(); i++) {
            ChunkNode chunk = chunks.get(i);
            chunk.setPosition(i);

            ChunkMetadata metadata = chunk.getMetadata();
            metadata.setStrategyName(getStrategyName());
            metadata.setEstimatedTokens(chunk.estimateTokens());

            // 计算起始位置
            int startIndex = 0;
            for (int j = 0; j < i; j++) {
                // 减去重叠部分
                int overlap = (j > 0) ? config.getChunkOverlap() : 0;
                startIndex += chunks.get(j).getLength() - overlap;
            }
            metadata.setStartIndex(startIndex);
            metadata.setEndIndex(startIndex + chunk.getLength());

            // 添加额外信息
            metadata.getExtra().put("chunk_index", i);
            metadata.getExtra().put("total_chunks", chunks.size());
            metadata.getExtra().put("has_overlap", config.getChunkOverlap() > 0 && i > 0);
        }
    }

    @Override
    public String getStrategyName() {
        return "recursive";
    }

    @Override
    public String getDescription() {
        return "按优先级递归尝试分隔符，自动选择最佳分割点";
    }

    @Override
    public String[] getSupportedTypes() {
        return new String[]{
                "text/plain",
                "text/markdown",
                "text/html"
        };
    }

    /**
     * 获取默认分隔符优先级列表
     */
    public static String[] getDefaultSeparators() {
        return DEFAULT_SEPARATORS.clone();
    }
}
