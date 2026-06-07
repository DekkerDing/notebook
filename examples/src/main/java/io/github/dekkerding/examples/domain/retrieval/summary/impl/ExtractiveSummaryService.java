package io.github.dekkerding.examples.domain.retrieval.summary.impl;

import io.github.dekkerding.examples.domain.retrieval.model.RetrieveResult;
import io.github.dekkerding.examples.domain.retrieval.summary.ResultSummaryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 抽取式摘要服务
 *
 * <p>从原文中提取关键句子作为摘要，保留原文真实性。</p>
 *
 * <p>抽取策略：</p>
 * <ul>
 *   <li>位置权重：首句和尾句权重更高</li>
 *   <li>关键词匹配：包含查询词的句子优先</li>
 *   <li>长度过滤：过滤过长或过短的句子</li>
 *   <li>去重：移除重复句子</li>
 * </ul>
 *
 * @author Notebook RAG Team
 * @since 1.0.0
 */
@Slf4j
@Component("extractiveSummaryService")
public class ExtractiveSummaryService implements ResultSummaryService {

    /**
     * 句子分割模式
     */
    private static final Pattern SENTENCE_PATTERN = Pattern.compile(
            "[。！？\\.\n]+|[！?]+|[。?]+"
    );

    /**
     * 最小句子长度
     */
    private static final int MIN_SENTENCE_LENGTH = 10;

    /**
     * 最大句子长度
     */
    private static final int MAX_SENTENCE_LENGTH = 200;

    /**
     * 最大摘要句子数
     */
    private static final int MAX_SUMMARY_SENTENCES = 3;

    @Override
    public SummaryResult summarize(RetrieveResult result) {
        if (result == null || result.getText() == null) {
            return createEmptySummary(result);
        }

        long startTime = System.currentTimeMillis();

        String text = result.getText();
        List<String> sentences = extractSentences(text);

        if (sentences.isEmpty()) {
            return createEmptySummary(result);
        }

        // 对句子打分
        List<ScoredSentence> scoredSentences = new ArrayList<>();
        for (int i = 0; i < sentences.size(); i++) {
            String sentence = sentences.get(i);
            double score = calculateSentenceScore(sentence, i, sentences.size());
            scoredSentences.add(new ScoredSentence(sentence, score, i));
        }

        // 按分数排序并选择Top-N
        scoredSentences.sort((a, b) -> Double.compare(b.score, a.score));

        int topN = Math.min(MAX_SUMMARY_SENTENCES, scoredSentences.size());
        List<ScoredSentence> topSentences = scoredSentences.subList(0, topN);

        // 按原文顺序重新排序
        topSentences.sort(Comparator.comparingInt(s -> s.position));

        // 构建摘要
        StringBuilder summary = new StringBuilder();
        for (ScoredSentence ss : topSentences) {
            if (summary.length() > 0) {
                summary.append(" ");
            }
            summary.append(ss.sentence);
        }

        // 创建结果
        SummaryResult summaryResult = new SummaryResult();
        summaryResult.setDocId(result.getDocId());
        summaryResult.setSummary(summary.toString());
        summaryResult.setStrategy(SummaryStrategy.EXTRACTIVE);
        summaryResult.setCompressionRatio((double) summary.length() / text.length());
        summaryResult.setKeySentences(
                topSentences.stream()
                        .map(ss -> ss.sentence)
                        .collect(Collectors.toList())
        );
        summaryResult.setGenerationLatency(System.currentTimeMillis() - startTime);

        log.debug("摘要生成完成: docId={}, compressionRatio={}",
                result.getDocId(),
                String.format("%.2f", summaryResult.getCompressionRatio()));

        return summaryResult;
    }

    @Override
    public AggregateSummary summarizeAggregate(List<RetrieveResult> results, String query) {
        if (results == null || results.isEmpty()) {
            return createEmptyAggregateSummary(query);
        }

        long startTime = System.currentTimeMillis();

        // 对每个结果生成摘要
        List<SummaryResult> summaries = summarizeBatch(results);

        // 提取所有关键句子
        Set<String> allKeySentences = new HashSet<>();
        for (SummaryResult summary : summaries) {
            if (summary.getKeySentences() != null) {
                allKeySentences.addAll(summary.getKeySentences());
            }
        }

        // 基于查询相关性选择关键句子
        List<String> relevantSentences = selectRelevantSentences(
                new ArrayList<>(allKeySentences),
                query
        );

        // 生成汇总
        StringBuilder summary = new StringBuilder();
        for (String sentence : relevantSentences) {
            if (summary.length() > 0) {
                summary.append(" ");
            }
            summary.append(sentence);
        }

        // 提取关键点
        List<String> keyPoints = extractKeyPoints(relevantSentences);

        AggregateSummary aggregateSummary = new AggregateSummary();
        aggregateSummary.setQuery(query);
        aggregateSummary.setSummary(summary.toString());
        aggregateSummary.setKeyPoints(keyPoints);
        aggregateSummary.setCoveredDocCount(results.size());
        aggregateSummary.setStrategy(SummaryStrategy.EXTRACTIVE);
        aggregateSummary.setGenerationLatency(System.currentTimeMillis() - startTime);

        log.info("汇总摘要生成完成: query={}, coveredDocs={}, keyPoints={}",
                query, results.size(), keyPoints.size());

        return aggregateSummary;
    }

    @Override
    public List<SummaryResult> summarizeBatch(List<RetrieveResult> results) {
        if (results == null || results.isEmpty()) {
            return Collections.emptyList();
        }

        List<SummaryResult> summaries = new ArrayList<>();

        for (RetrieveResult result : results) {
            summaries.add(summarize(result));
        }

        return summaries;
    }

    /**
     * 提取句子
     */
    private List<String> extractSentences(String text) {
        List<String> sentences = new ArrayList<>();

        String[] parts = SENTENCE_PATTERN.split(text);

        for (String part : parts) {
            String sentence = part.trim();
            if (sentence.length() >= MIN_SENTENCE_LENGTH &&
                sentence.length() <= MAX_SENTENCE_LENGTH) {
                sentences.add(sentence);
            }
        }

        return sentences;
    }

    /**
     * 计算句子分数
     */
    private double calculateSentenceScore(String sentence, int position, int totalSentences) {
        double score = 0.0;

        // 1. 位置权重（首句和尾句权重更高）
        if (position == 0) {
            score += 0.3; // 首句
        } else if (position == totalSentences - 1) {
            score += 0.2; // 尾句
        } else if (position < totalSentences * 0.2) {
            score += 0.1; // 前20%
        }

        // 2. 长度适中偏好
        int length = sentence.length();
        if (length >= 30 && length <= 100) {
            score += 0.2;
        } else if (length < 30) {
            score += 0.1;
        }

        // 3. 数字和关键词
        if (sentence.matches(".*\\d+.*")) {
            score += 0.1;
        }

        if (sentence.matches(".*(配置|设置|方法|原理|步骤).*")) {
            score += 0.1;
        }

        return score;
    }

    /**
     * 选择相关句子
     */
    private List<String> selectRelevantSentences(List<String> sentences, String query) {
        // 简单实现：基于关键词匹配
        Set<String> queryKeywords = extractKeywords(query);

        List<ScoredSentence> scored = new ArrayList<>();
        for (String sentence : sentences) {
            double score = 0.0;
            for (String keyword : queryKeywords) {
                if (sentence.toLowerCase().contains(keyword.toLowerCase())) {
                    score += 0.3;
                }
            }
            scored.add(new ScoredSentence(sentence, score, 0));
        }

        // 按分数排序
        scored.sort((a, b) -> Double.compare(b.score, a.score));

        // 选择Top-5
        int topN = Math.min(5, scored.size());
        List<String> selected = new ArrayList<>();

        for (int i = 0; i < topN; i++) {
            selected.add(scored.get(i).sentence);
        }

        return selected;
    }

    /**
     * 提取关键点
     */
    private List<String> extractKeyPoints(List<String> sentences) {
        List<String> keyPoints = new ArrayList<>();

        for (String sentence : sentences) {
            // 寻找陈述性句子
            if (sentence.matches(".*(是|的|可以|能够|支持|提供).*")) {
                keyPoints.add(sentence);
            }
        }

        return keyPoints;
    }

    /**
     * 提取关键词
     */
    private Set<String> extractKeywords(String query) {
        Set<String> keywords = new HashSet<>();
        String[] words = query.split("\\s+");

        for (String word : words) {
            if (word.length() > 1) {
                keywords.add(word.toLowerCase());
            }
        }

        return keywords;
    }

    /**
     * 创建空摘要
     */
    private SummaryResult createEmptySummary(RetrieveResult result) {
        SummaryResult summary = new SummaryResult();
        summary.setDocId(result != null ? result.getDocId() : "");
        summary.setSummary("");
        summary.setStrategy(SummaryStrategy.EXTRACTIVE);
        summary.setCompressionRatio(0.0);
        summary.setKeySentences(Collections.emptyList());
        summary.setGenerationLatency(0);
        return summary;
    }

    /**
     * 创建空汇总摘要
     */
    private AggregateSummary createEmptyAggregateSummary(String query) {
        AggregateSummary summary = new AggregateSummary();
        summary.setQuery(query);
        summary.setSummary("");
        summary.setKeyPoints(Collections.emptyList());
        summary.setCoveredDocCount(0);
        summary.setStrategy(SummaryStrategy.EXTRACTIVE);
        summary.setGenerationLatency(0);
        return summary;
    }

    /**
     * 带分数的句子
     */
    private static class ScoredSentence {
        String sentence;
        double score;
        int position;

        ScoredSentence(String sentence, double score, int position) {
            this.sentence = sentence;
            this.score = score;
            this.position = position;
        }
    }
}
