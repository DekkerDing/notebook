package io.github.dekkerding.examples.domain.retrieval.intent.impl;

import io.github.dekkerding.examples.domain.retrieval.intent.QueryIntentDetector;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Pattern;

/**
 * 基于规则的查询意图检测器
 *
 * <p>使用规则和模式匹配识别查询意图，简单高效，适合快速部署。</p>
 *
 * <p>检测规则：</p>
 * <ul>
 *   <li>关键词匹配：疑问词、动词、名词</li>
 *   <li>句式模式：疑问句、陈述句</li>
 *   <li>长度特征：短查询通常是事实查询</li>
 *   <li>标点符号：问号、冒号等</li>
 * </ul>
 *
 * @author Notebook RAG Team
 * @since 1.0.0
 */
@Slf4j
@Component("ruleBasedIntentDetector")
public class RuleBasedIntentDetector implements QueryIntentDetector {

    /**
     * 意图关键词映射
     */
    private static final Map<QueryIntent, Set<String>> INTENT_KEYWORDS = new HashMap<>();

    /**
     * 意图模式映射
     */
    private static final Map<QueryIntent, List<Pattern>> INTENT_PATTERNS = new HashMap<>();

    static {
        // 事实查询关键词
        INTENT_KEYWORDS.put(QueryIntent.FACT_QUERY, new HashSet<>(Arrays.asList(
                "什么", "哪个", "哪些", "多少", "几个", "什么时候", "在哪",
                "什么是", "哪个是", "谁", "姓名", "地址", "电话", "邮箱"
        )));

        // 概念查询关键词
        INTENT_KEYWORDS.put(QueryIntent.CONCEPT_QUERY, new HashSet<>(Arrays.asList(
                "什么是", "解释", "定义", "原理", "概念", "怎么理解", "含义",
                "工作原理", "机制", "架构", "设计"
        )));

        // 操作查询关键词
        INTENT_KEYWORDS.put(QueryIntent.OPERATION_QUERY, new HashSet<>(Arrays.asList(
                "如何", "怎么", "怎样", "方法", "步骤", "流程", "操作", "配置",
                "设置", "安装", "部署", "使用", "实现"
        )));

        // 关系查询关键词
        INTENT_KEYWORDS.put(QueryIntent.RELATION_QUERY, new HashSet<>(Arrays.asList(
                "和", "与", "关系", "属于", "包含", "连接", "关联", "依赖",
                "是", "的", "中", "在", "谁", "哪里"
        )));

        // 对比查询关键词
        INTENT_KEYWORDS.put(QueryIntent.COMPARISON_QUERY, new HashSet<>(Arrays.asList(
                "区别", "差异", "对比", "比较", "相同", "不同", "优缺点", "vs",
                "和", "或", "之间"
        )));

        // 意图模式
        INTENT_PATTERNS.put(QueryIntent.FACT_QUERY, Arrays.asList(
                Pattern.compile(".*是什么？$"),
                Pattern.compile(".*有哪些？$")
        ));

        INTENT_PATTERNS.put(QueryIntent.CONCEPT_QUERY, Arrays.asList(
                Pattern.compile("^什么是.*"),
                Pattern.compile(".*是什么意思")
        ));

        INTENT_PATTERNS.put(QueryIntent.OPERATION_QUERY, Arrays.asList(
                Pattern.compile("^如何.*"),
                Pattern.compile("^怎么.*"),
                Pattern.compile(".*的方法$")
        ));

        INTENT_PATTERNS.put(QueryIntent.COMPARISON_QUERY, Arrays.asList(
                Pattern.compile(".*和.*的区别"),
                Pattern.compile(".*vs.*"),
                Pattern.compile(".*对比.*")
        ));
    }

    @Override
    public IntentDetectionResult detect(String query) {
        long startTime = System.currentTimeMillis();

        IntentDetectionResult result = new IntentDetectionResult();
        result.setQuery(query);

        // 检测各种意图
        Map<QueryIntent, Double> intentScores = new HashMap<>();

        intentScores.put(QueryIntent.FACT_QUERY, detectFactQuery(query));
        intentScores.put(QueryIntent.CONCEPT_QUERY, detectConceptQuery(query));
        intentScores.put(QueryIntent.OPERATION_QUERY, detectOperationQuery(query));
        intentScores.put(QueryIntent.RELATION_QUERY, detectRelationQuery(query));
        intentScores.put(QueryIntent.COMPARISON_QUERY, detectComparisonQuery(query));

        // 找到最高分的意图
        QueryIntent detectedIntent = QueryIntent.UNKNOWN;
        double maxScore = 0.0;

        for (Map.Entry<QueryIntent, Double> entry : intentScores.entrySet()) {
            if (entry.getValue() > maxScore) {
                maxScore = entry.getValue();
                detectedIntent = entry.getKey();
            }
        }

        // 置信度归一化
        double confidence = maxScore;
        if (confidence < 0.3) {
            detectedIntent = QueryIntent.UNKNOWN;
            confidence = 0.0;
        }

        result.setIntent(detectedIntent);
        result.setConfidence(confidence);
        result.setRecommendedStrategy(getRecommendedStrategy(detectedIntent));

        // 提取特征
        Map<String, Object> features = new HashMap<>();
        features.put("intent_scores", intentScores);
        features.put("query_length", query.length());
        features.put("has_question_mark", query.contains("?") || query.contains("？"));
        result.setFeatures(features);

        result.setDetectionLatency(System.currentTimeMillis() - startTime);

        log.debug("意图检测完成: query={}, intent={}, confidence={}",
                query, detectedIntent, String.format("%.2f", confidence));

        return result;
    }

    @Override
    public List<IntentDetectionResult> detectBatch(List<String> queries) {
        List<IntentDetectionResult> results = new ArrayList<>();

        for (String query : queries) {
            results.add(detect(query));
        }

        return results;
    }

    /**
     * 检测事实查询
     */
    private double detectFactQuery(String query) {
        double score = 0.0;

        // 关键词匹配
        Set<String> keywords = INTENT_KEYWORDS.get(QueryIntent.FACT_QUERY);
        for (String keyword : keywords) {
            if (query.contains(keyword)) {
                score += 0.2;
            }
        }

        // 模式匹配
        List<Pattern> patterns = INTENT_PATTERNS.get(QueryIntent.FACT_QUERY);
        if (patterns != null) {
            for (Pattern pattern : patterns) {
                if (pattern.matcher(query).matches()) {
                    score += 0.3;
                }
            }
        }

        // 长度特征（短查询通常是事实查询）
        if (query.length() < 20) {
            score += 0.1;
        }

        return Math.min(1.0, score);
    }

    /**
     * 检测概念查询
     */
    private double detectConceptQuery(String query) {
        double score = 0.0;

        // 关键词匹配
        Set<String> keywords = INTENT_KEYWORDS.get(QueryIntent.CONCEPT_QUERY);
        for (String keyword : keywords) {
            if (query.contains(keyword)) {
                score += 0.25;
            }
        }

        // 模式匹配
        List<Pattern> patterns = INTENT_PATTERNS.get(QueryIntent.CONCEPT_QUERY);
        if (patterns != null) {
            for (Pattern pattern : patterns) {
                if (pattern.matcher(query).matches()) {
                    score += 0.3;
                }
            }
        }

        return Math.min(1.0, score);
    }

    /**
     * 检测操作查询
     */
    private double detectOperationQuery(String query) {
        double score = 0.0;

        // 关键词匹配
        Set<String> keywords = INTENT_KEYWORDS.get(QueryIntent.OPERATION_QUERY);
        for (String keyword : keywords) {
            if (query.contains(keyword)) {
                score += 0.2;
            }
        }

        // 模式匹配
        List<Pattern> patterns = INTENT_PATTERNS.get(QueryIntent.OPERATION_QUERY);
        if (patterns != null) {
            for (Pattern pattern : patterns) {
                if (pattern.matcher(query).matches()) {
                    score += 0.3;
                }
            }
        }

        return Math.min(1.0, score);
    }

    /**
     * 检测关系查询
     */
    private double detectRelationQuery(String query) {
        double score = 0.0;

        // 关键词匹配
        Set<String> keywords = INTENT_KEYWORDS.get(QueryIntent.RELATION_QUERY);
        for (String keyword : keywords) {
            if (query.contains(keyword)) {
                score += 0.15;
            }
        }

        // 包含实体（大写字母开头的词）
        String[] words = query.split("\\s+");
        int entityCount = 0;
        for (String word : words) {
            if (word.matches("^[A-Z].*")) {
                entityCount++;
            }
        }

        if (entityCount >= 2) {
            score += 0.3;
        }

        return Math.min(1.0, score);
    }

    /**
     * 检测对比查询
     */
    private double detectComparisonQuery(String query) {
        double score = 0.0;

        // 关键词匹配
        Set<String> keywords = INTENT_KEYWORDS.get(QueryIntent.COMPARISON_QUERY);
        for (String keyword : keywords) {
            if (query.contains(keyword)) {
                score += 0.3;
            }
        }

        // 模式匹配
        List<Pattern> patterns = INTENT_PATTERNS.get(QueryIntent.COMPARISON_QUERY);
        if (patterns != null) {
            for (Pattern pattern : patterns) {
                if (pattern.matcher(query).matches()) {
                    score += 0.4;
                }
            }
        }

        // "vs"模式
        if (query.toLowerCase().contains(" vs ")) {
            score += 0.3;
        }

        return Math.min(1.0, score);
    }
}
