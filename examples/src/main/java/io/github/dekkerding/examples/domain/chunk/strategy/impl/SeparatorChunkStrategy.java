package io.github.dekkerding.examples.domain.chunk.strategy.impl;

import io.github.dekkerding.examples.domain.chunk.model.ChunkConfig;
import io.github.dekkerding.examples.domain.chunk.model.ChunkNode;
import io.github.dekkerding.examples.domain.chunk.model.ChunkNode.ChunkMetadata;
import io.github.dekkerding.examples.domain.chunk.model.ChunkNode.ChunkType;
import io.github.dekkerding.examples.domain.chunk.strategy.ChunkStrategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 分隔符分块策略 - 按指定分隔符对文本进行分块
 *
 * <p>功能：</p>
 * <ul>
 *   <li>支持自定义分隔符（如 "\n\n", "\n", "。", "！" 等）</li>
 *   <li>支持多级分隔符（按优先级尝试）</li>
 *   <li>保留或移除分隔符</li>
 *   <li>处理中文和英文分隔符</li>
 * </ul>
 *
 * <p>使用场景：</p>
 * <ul>
 *   <li>普通文本文档</li>
 *   <li>Markdown文档</li>
 *   <li>结构化文本（如日志文件）</li>
 * </ul>
 *
 * <p>示例：</p>
 * <pre>
 * ChunkConfig config = ChunkConfig.builder()
 *     .chunkSize(500)
 *     .chunkOverlap(50)
 *     .customSeparators(new String[]{"\n\n", "\n", "。", "！", "？", "."})
 *     .build();
 *
 * ChunkStrategy strategy = new SeparatorChunkStrategy();
 * List&lt;ChunkNode&gt; chunks = strategy.split(text, config);
 * </pre>
 *
 * @author Notebook RAG Team
 * @since 1.0.0
 */
@Slf4j
@Component
public class SeparatorChunkStrategy implements ChunkStrategy {

    /**
     * 默认分隔符列表（中文优先）
     */
    private static final String[] DEFAULT_SEPARATORS = {
            "\n\n",      // 段落分隔
            "\n",        // 行分隔
            "。",        // 中文句号
            "！",        // 中文感叹号
            "？",        // 中文问号
            "；",        // 中文分号
            ".",         // 英文句号
            "!",         // 英文感叹号
            "?",         // 英文问号
            ";",         // 英文分号
            "\t",        // 制表符
            " ",         // 空格
            ""           // 最后手段：按字符分割
    };

    /**
     * 中文序号模式（用于识别章节）
     */
    private static final Pattern CHINESE_NUMBER_PATTERN = Pattern.compile(
            "^[一二三四五六七八九十百千]+、[\\s\\S]*"
    );

    @Override
    public List<ChunkNode> split(String text, ChunkConfig config) {
        log.debug("开始分隔符分块: textLength={}, config={}", text != null ? text.length() : 0, config);

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
        String[] separators = config.getCustomSeparators();
        if (separators == null || separators.length == 0) {
            separators = DEFAULT_SEPARATORS;
        }

        // 执行分块
        List<ChunkNode> chunks = performSplit(processedText, separators, config);

        // 后处理
        chunks = postProcessChunks(chunks, config);

        // 添加元数据
        enrichMetadata(chunks, config);

        log.debug("分隔符分块完成: chunks={}", chunks.size());
        return chunks;
    }

    /**
     * 执行分块逻辑
     */
    private List<ChunkNode> performSplit(String text, String[] separators, ChunkConfig config) {
        List<ChunkNode> chunks = new ArrayList<>();

        // 尝试每个分隔符，直到找到合适的
        for (String separator : separators) {
            List<String> splits = splitBySeparator(text, separator, config);

            if (splits.size() == 1) {
                // 只有一个分块，说明分隔符不合适，继续尝试下一个
                continue;
            }

            // 检查分块大小是否合理
            boolean allReasonableSize = splits.stream()
                    .allMatch(s -> s.length() <= config.getHardMaxSize());

            if (allReasonableSize) {
                // 找到合适的分隔符，创建分块节点
                for (String split : splits) {
                    if (!split.trim().isEmpty()) {
                        chunks.add(createChunkNode(split, chunks.size()));
                    }
                }
                break;
            }
        }

        // 如果所有分隔符都不合适，按硬限制强制分割
        if (chunks.isEmpty()) {
            chunks = forceSplit(text, config);
        }

        return chunks;
    }

    /**
     * 按分隔符分割文本
     */
    private List<String> splitBySeparator(String text, String separator, ChunkConfig config) {
        if (separator == null || separator.isEmpty()) {
            // 空分隔符：按字符分割
            return splitByCharacter(text, config);
        }

        List<String> splits = new ArrayList<>();

        if (separator.equals("\n\n")) {
            // 段落分隔：保留段落结构
            String[] paragraphs = text.split("\n\n");
            splits.addAll(Arrays.asList(paragraphs));
        } else {
            // 普通分隔符
            String[] parts = text.split(Pattern.quote(separator));
            splits.addAll(Arrays.asList(parts));
        }

        // 处理分隔符保留
        if (config.isKeepSeparator()) {
            return splits.stream()
                    .map(s -> s + separator)
                    .collect(Collectors.toList());
        }

        // 清理空白
        return splits.stream()
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }

    /**
     * 按字符分割
     */
    private List<String> splitByCharacter(String text, ChunkConfig config) {
        List<String> chunks = new ArrayList<>();
        int chunkSize = config.getChunkSize();

        for (int i = 0; i < text.length(); i += chunkSize) {
            int end = Math.min(i + chunkSize, text.length());
            String chunk = text.substring(i, end);
            chunks.add(chunk.trim());
        }

        return chunks;
    }

    /**
     * 强制分割（当所有分隔符都不合适时）
     */
    private List<ChunkNode> forceSplit(String text, ChunkConfig config) {
        List<ChunkNode> chunks = new ArrayList<>();
        int maxSize = config.getHardMaxSize();

        for (int i = 0; i < text.length(); i += maxSize) {
            int end = Math.min(i + maxSize, text.length());
            String chunk = text.substring(i, end);
            chunks.add(createChunkNode(chunk, chunks.size()));
        }

        return chunks;
    }

    /**
     * 创建分块节点
     */
    private ChunkNode createChunkNode(String content, int position) {
        return ChunkNode.builder()
                .content(content)
                .position(position)
                .metadata(ChunkMetadata.builder()
                        .chunkType(detectChunkType(content))
                        .charCount(content.length())
                        .build())
                .build();
    }

    /**
     * 检测分块类型
     */
    private ChunkType detectChunkType(String content) {
        if (CHINESE_NUMBER_PATTERN.matcher(content.trim()).matches()) {
            return ChunkType.HEADER;
        }

        // 检测列表
        if (content.trim().matches("^[\\d\\-\\*•]+\\..*")) {
            return ChunkType.LIST;
        }

        // 检测表格
        if (content.contains("|") && content.split("\\|").length >= 3) {
            return ChunkType.TABLE;
        }

        // 默认为文本
        return ChunkType.TEXT;
    }

    /**
     * 丰富元数据
     */
    private void enrichMetadata(List<ChunkNode> chunks, ChunkConfig config) {
        for (int i = 0; i < chunks.size(); i++) {
            ChunkNode chunk = chunks.get(i);
            ChunkMetadata metadata = chunk.getMetadata();

            // 设置策略名称
            metadata.setStrategyName(getStrategyName());

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
        }
    }

    @Override
    public String getStrategyName() {
        return "separator";
    }

    @Override
    public String getDescription() {
        return "按分隔符进行文本分块，支持自定义分隔符列表";
    }

    @Override
    public String[] getSupportedTypes() {
        return new String[]{
                "text/plain",
                "text/markdown",
                "text/html",
                "text/csv"
        };
    }

    /**
     * 获取默认分隔符列表
     */
    public static String[] getDefaultSeparators() {
        return DEFAULT_SEPARATORS.clone();
    }
}
