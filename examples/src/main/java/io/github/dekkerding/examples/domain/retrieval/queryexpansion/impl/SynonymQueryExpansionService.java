package io.github.dekkerding.examples.domain.retrieval.queryexpansion.impl;

import io.github.dekkerding.examples.domain.retrieval.queryexpansion.QueryExpansionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 同义词查询扩展服务
 *
 * <p>基于同义词词典进行查询扩展，简单高效。</p>
 *
 * @author Notebook RAG Team
 * @since 1.0.0
 */
@Slf4j
@Component("synonymQueryExpansionService")
public class SynonymQueryExpansionService implements QueryExpansionService {

    /**
     * 同义词词典
     */
    private static final Map<String, Set<String>> SYNONYM_DICT = new HashMap<>();

    static {
        // 编程相关同义词
        addSynonyms("配置", Arrays.asList("设置", "参数", "配置项", "设定"));
        addSynonyms("使用", Arrays.asList("应用", "调用", "采用", "运用"));
        addSynonyms("创建", Arrays.asList("建立", "生成", "新建", "构造"));
        addSynonyms("获取", Arrays.asList("得到", "取得", "获得", "读取"));
        addSynonyms("发送", Arrays.asList("传送", "传输", "推送", "提交"));
        addSynonyms("删除", Arrays.asList("移除", "清除", "销毁", "取消"));

        // 框架相关
        addSynonyms("Spring Boot", Arrays.asList("SpringBoot", "springboot"));
        addSynonyms("数据库", Arrays.asList("DB", "DataBase", "存储"));
        addSynonyms("API", Arrays.asList("接口", "应用程序接口", "application interface"));
        addSynonyms("服务", Arrays.asList("service", "服务器", "server"));

        // 通用同义词
        addSynonyms("方法", Arrays.asList("函数", "function", "方式", "途径"));
        addSynonyms("问题", Arrays.asList("错误", "bug", "故障", "异常", "exception"));
    }

    private static void addSynonyms(String word, List<String> synonyms) {
        Set<String> set = new HashSet<>(synonyms);
        SYNONYM_DICT.put(word.toLowerCase(), set);

        // 反向映射
        for (String synonym : synonyms) {
            Set<String> existing = SYNONYM_DICT.computeIfAbsent(
                    synonym.toLowerCase(),
                    k -> new HashSet<>()
            );
            existing.add(word);
        }
    }

    @Override
    public QueryExpansionResult expand(String originalQuery) {
        long startTime = System.currentTimeMillis();

        QueryExpansionResult result = new QueryExpansionResult();
        result.setOriginalQuery(originalQuery);
        result.setStrategy(ExpansionStrategy.SYNONYM);

        List<String> expandedQueries = new ArrayList<>();
        expandedQueries.add(originalQuery); // 原始查询

        // 分词并替换同义词
        String[] words = originalQuery.split("\\s+");
        Set<String> expanded = new HashSet<>();
        expanded.add(originalQuery);

        // 生成查询变体
        for (int i = 0; i < words.length; i++) {
            String word = words[i].toLowerCase();
            Set<String> synonyms = SYNONYM_DICT.get(word);

            if (synonyms != null && !synonyms.isEmpty()) {
                for (String synonym : synonyms) {
                    StringBuilder sb = new StringBuilder();
                    for (int j = 0; j < words.length; j++) {
                        if (j == i) {
                            sb.append(synonym);
                        } else {
                            sb.append(words[j]);
                        }
                        if (j < words.length - 1) {
                            sb.append(" ");
                        }
                    }
                    expanded.add(sb.toString());
                }
            }
        }

        expandedQueries.addAll(expanded);
        result.setExpandedQueries(new ArrayList<>(expanded));
        result.setExpansionCount(expanded.size() - 1);
        result.setConfidence(0.7);
        result.setExpansionLatency(System.currentTimeMillis() - startTime);

        log.debug("查询扩展完成: original={}, expanded={}",
                originalQuery, expanded.size());

        return result;
    }

    @Override
    public List<QueryExpansionResult> expandBatch(List<String> queries) {
        List<QueryExpansionResult> results = new ArrayList<>();

        for (String query : queries) {
            results.add(expand(query));
        }

        return results;
    }

    /**
     * 添加自定义同义词
     *
     * @param word 原词
     * @param synonyms 同义词列表
     */
    public void addCustomSynonyms(String word, List<String> synonyms) {
        Set<String> set = new HashSet<>(synonyms);
        SYNONYM_DICT.put(word.toLowerCase(), set);

        log.info("添加自定义同义词: word={}, synonyms={}", word, synonyms);
    }
}
