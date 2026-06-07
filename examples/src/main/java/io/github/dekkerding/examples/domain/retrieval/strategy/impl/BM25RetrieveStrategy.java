package io.github.dekkerding.examples.domain.retrieval.strategy.impl;

import io.github.dekkerding.examples.domain.retrieval.model.RetrieveContext;
import io.github.dekkerding.examples.domain.retrieval.model.RetrieveRequest;
import io.github.dekkerding.examples.domain.retrieval.model.RetrieveResult;
import io.github.dekkerding.examples.domain.retrieval.strategy.RetrieveStrategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * BM25检索策略 - 基于概率检索模型的稀疏检索
 *
 * <p>BM25是一种用于估计文档与查询相关性的排序函数，是搜索引擎中常用的算法。</p>
 *
 * <p>核心公式：</p>
 * <pre>
 * score(D,Q) = Σ IDF(qi) × (f(qi,D) × (k1 + 1)) / (f(qi,D) + k1 × (1 - b + b × |D| / avgdl))
 *
 * 其中：
 * - qi: 查询中的第i个词
 * - f(qi,D): 词qi在文档D中的频率
 * - |D|: 文档D的长度
 * - avgdl: 语料库中文档的平均长度
 * - k1: 词频饱和参数（默认1.2）
 * - b: 长度归一化参数（默认0.75）
 * - IDF(qi): 词qi的逆文档频率
 * </pre>
 *
 * <p>优势：</p>
 * <ul>
 *   <li>考虑了词频和文档长度</li>
 *   <li>对长文档有归一化处理</li>
 *   <li>计算效率高，适合大规模检索</li>
 *   <li>对中文文本有良好支持</li>
 * </ul>
 *
 * @author Notebook RAG Team
 * @since 1.0.0
 */
@Slf4j
@Component
public class BM25RetrieveStrategy implements RetrieveStrategy {

    /**
     * 默认k1参数（词频饱和参数）
     */
    private static final double DEFAULT_K1 = 1.2;

    /**
     * 默认b参数（长度归一化参数）
     */
    private static final double DEFAULT_B = 0.75;

    /**
     * 中文分词正则（简单实现）
     */
    private static final Pattern CHINESE_TOKEN_PATTERN = Pattern.compile("[\\u4e00-\\u9fa5]");
    private static final Pattern WORD_TOKEN_PATTERN = Pattern.compile("[a-zA-Z0-9]+");

    /**
     * 文档索引（文档ID → 文档内容）
     */
    private final Map<String, DocumentStats> documentIndex = new HashMap<>();

    /**
     * 倒排索引（词 → 文档列表）
     */
    private final Map<String, Set<String>> invertedIndex = new HashMap<>();

    /**
     * 逆文档频率缓存（词 → IDF值）
     */
    private final Map<String, Double> idfCache = new HashMap<>();

    /**
     * 语料库统计信息
     */
    private CorpusStats corpusStats = new CorpusStats();

    /**
     * BM25参数配置
     */
    private double k1 = DEFAULT_K1;
    private double b = DEFAULT_B;

    @Override
    public List<RetrieveResult> retrieve(String query, RetrieveRequest request, RetrieveContext context) {
        log.debug("BM25检索: query={}, k1={}, b={}", query, k1, b);

        long startTime = System.currentTimeMillis();

        // 1. 分词
        List<String> queryTokens = tokenize(query);
        if (queryTokens.isEmpty()) {
            return Collections.emptyList();
        }

        log.debug("查询分词: {}", queryTokens);

        // 2. 计算查询词的IDF
        Map<String, Double> queryIdfMap = new HashMap<>();
        for (String token : queryTokens) {
            double idf = calculateIDF(token);
            queryIdfMap.put(token, idf);
        }

        // 3. 计算每个文档的BM25分数
        List<RetrieveResult> results = new ArrayList<>();
        Set<String> candidateDocs = findCandidateDocuments(queryTokens);

        for (String docId : candidateDocs) {
            DocumentStats docStats = documentIndex.get(docId);
            if (docStats == null) continue;

            double score = calculateBM25Score(queryTokens, queryIdfMap, docStats);

            if (score > 0) {
                results.add(RetrieveResult.builder()
                        .docId(docId)
                        .text(docStats.content)
                        .score(score)
                        .source("bm25")
                        .metadata(buildMetadata(docStats, score))
                        .latency(System.currentTimeMillis() - startTime)
                        .build());
            }
        }

        // 4. 排序
        results.sort((a, b) -> Double.compare(b.getScore(), a.getScore()));

        // 5. 截取TopK
        int topK = request != null ? request.getTopK() : 10;
        if (results.size() > topK) {
            results = results.subList(0, topK);
        }

        log.debug("BM25检索完成: results={}, 耗时={}ms",
                results.size(), System.currentTimeMillis() - startTime);

        return results;
    }

    @Override
    public String getStrategyName() {
        return "bm25";
    }

    @Override
    public String getDescription() {
        return "BM25概率检索模型，适用于关键词匹配和稀疏检索场景";
    }

    /**
     * 文本分词
     */
    private List<String> tokenize(String text) {
        if (text == null || text.trim().isEmpty()) {
            return Collections.emptyList();
        }

        List<String> tokens = new ArrayList<>();

        // 中文分词（简单实现：按字符分割）
        Matcher chineseMatcher = CHINESE_TOKEN_PATTERN.matcher(text);
        while (chineseMatcher.find()) {
            String token = chineseMatcher.group();
            // 过滤标点符号
            if (!isPunctuation(token)) {
                tokens.add(token);
            }
        }

        // 英文单词提取
        Matcher wordMatcher = WORD_TOKEN_PATTERN.matcher(text);
        while (wordMatcher.find()) {
            String token = wordMatcher.group().toLowerCase();
            if (token.length() > 1) { // 过滤单字符
                tokens.add(token);
            }
        }

        return tokens;
    }

    /**
     * 判断是否为标点符号
     */
    private boolean isPunctuation(String charStr) {
        return "。，、；：？！「」『』（）【】《》".contains(charStr);
    }

    /**
     * 计算逆文档频率（IDF）
     * IDF(q) = log((N - df(q) + 0.5) / (df(q) + 0.5))
     *
     * @param term 词项
     * @return IDF值
     */
    private double calculateIDF(String term) {
        // 检查缓存
        if (idfCache.containsKey(term)) {
            return idfCache.get(term);
        }

        Set<String> docs = invertedIndex.get(term);
        int df = docs != null ? docs.size() : 0; // 文档频率

        // 计算IDF（使用平滑版本避免负值）
        int N = documentIndex.size();
        double idf;
        if (df == 0) {
            idf = 0;
        } else {
            idf = Math.log((N - df + 0.5) / (df + 0.5));
        }

        // 缓存结果
        idfCache.put(term, idf);

        return idf;
    }

    /**
     * 计算BM25分数
     */
    private double calculateBM25Score(List<String> queryTokens,
                                     Map<String, Double> queryIdfMap,
                                     DocumentStats docStats) {
        double score = 0.0;

        for (String token : queryTokens) {
            // 获取词在文档中的频率
            int tf = docStats.getTermFrequency(token);
            if (tf == 0) continue;

            // 获取IDF
            Double idf = queryIdfMap.get(token);
            if (idf == null || idf == 0) continue;

            // 计算BM25分量
            double numerator = tf * (k1 + 1);
            double denominator = tf + k1 * (1 - b + b * docStats.length / corpusStats.avgDocLength);
            double component = idf * (numerator / denominator);

            score += component;
        }

        return score;
    }

    /**
     * 查找候选文档（包含查询词的文档）
     */
    private Set<String> findCandidateDocuments(List<String> queryTokens) {
        Set<String> candidates = new HashSet<>();

        for (String token : queryTokens) {
            Set<String> docs = invertedIndex.get(token);
            if (docs != null) {
                candidates.addAll(docs);
            }
        }

        return candidates;
    }

    /**
     * 构建元数据
     */
    private Map<String, Object> buildMetadata(DocumentStats docStats, double score) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("doc_length", docStats.length);
        metadata.put("unique_terms", docStats.uniqueTerms);
        Map<String, Object> bm25Params = new HashMap<>();
        bm25Params.put("k1", k1);
        bm25Params.put("b", b);
        metadata.put("bm25_params", bm25Params);
        return metadata;
    }

    // ===== 索引管理方法 =====

    /**
     * 添加文档到索引
     * @param docId 文档ID
     * @param content 文档内容
     */
    public void indexDocument(String docId, String content) {
        if (docId == null || content == null) {
            return;
        }

        // 分词
        List<String> tokens = tokenize(content);
        Map<String, Integer> termFreqMap = new HashMap<>();

        for (String token : tokens) {
            termFreqMap.put(token, termFreqMap.getOrDefault(token, 0) + 1);

            // 更新倒排索引
            invertedIndex.computeIfAbsent(token, k -> new HashSet<>()).add(docId);
        }

        // 创建文档统计
        DocumentStats docStats = new DocumentStats();
        docStats.docId = docId;
        docStats.content = content;
        docStats.length = tokens.size();
        docStats.uniqueTerms = termFreqMap.size();
        docStats.termFrequencies = termFreqMap;

        documentIndex.put(docId, docStats);

        // 更新语料库统计
        updateCorpusStats();

        log.debug("索引文档: docId={}, length={}, uniqueTerms={}",
                docId, docStats.length, docStats.uniqueTerms);
    }

    /**
     * 批量索引文档
     */
    public void indexBatch(Map<String, String> documents) {
        for (Map.Entry<String, String> entry : documents.entrySet()) {
            indexDocument(entry.getKey(), entry.getValue());
        }
        log.info("批量索引完成: totalDocs={}", documentIndex.size());
    }

    /**
     * 更新语料库统计信息
     */
    private void updateCorpusStats() {
        int totalDocs = documentIndex.size();
        long totalLength = 0;

        for (DocumentStats docStats : documentIndex.values()) {
            totalLength += docStats.length;
        }

        corpusStats.totalDocs = totalDocs;
        corpusStats.totalLength = totalLength;
        corpusStats.avgDocLength = totalDocs > 0 ? (double) totalLength / totalDocs : 0;

        // 清空IDF缓存（因为文档数量变化了）
        idfCache.clear();
    }

    /**
     * 清空索引
     */
    public void clearIndex() {
        documentIndex.clear();
        invertedIndex.clear();
        idfCache.clear();
        corpusStats = new CorpusStats();
        log.info("索引已清空");
    }

    /**
     * 获取索引统计信息
     */
    public IndexStats getIndexStats() {
        IndexStats stats = new IndexStats();
        stats.totalDocuments = documentIndex.size();
        stats.totalUniqueTerms = invertedIndex.size();
        stats.avgDocLength = corpusStats.avgDocLength;
        stats.totalTermOccurrences = corpusStats.totalLength;
        return stats;
    }

    /**
     * 设置BM25参数
     */
    public void setBM25Params(double k1, double b) {
        if (k1 < 0 || k1 > 3) {
            throw new IllegalArgumentException("k1 must be between 0 and 3");
        }
        if (b < 0 || b > 1) {
            throw new IllegalArgumentException("b must be between 0 and 1");
        }

        this.k1 = k1;
        this.b = b;
        log.info("BM25参数已更新: k1={}, b={}", k1, b);
    }

    // ===== 内部数据结构 =====

    /**
     * 文档统计信息
     */
    private static class DocumentStats {
        String docId;
        String content;
        int length;
        int uniqueTerms;
        Map<String, Integer> termFrequencies;

        int getTermFrequency(String term) {
            return termFrequencies != null ? termFrequencies.getOrDefault(term, 0) : 0;
        }
    }

    /**
     * 语料库统计信息
     */
    private static class CorpusStats {
        int totalDocs = 0;
        long totalLength = 0;
        double avgDocLength = 0;
    }

    /**
     * 索引统计信息
     */
    @lombok.Data
    public static class IndexStats {
        private int totalDocuments;
        private int totalUniqueTerms;
        private double avgDocLength;
        private long totalTermOccurrences;

        @Override
        public String toString() {
            return "IndexStats{" +
                    "totalDocuments=" + totalDocuments +
                    ", totalUniqueTerms=" + totalUniqueTerms +
                    ", avgDocLength=" + String.format("%.2f", avgDocLength) +
                    ", totalTermOccurrences=" + totalTermOccurrences +
                    '}';
        }
    }
}
