package io.github.dekkerding.examples.domain.graph.extractor.impl;

import io.github.dekkerding.examples.domain.graph.extractor.EntityExtractor;
import io.github.dekkerding.examples.domain.graph.model.Entity;
import io.github.dekkerding.examples.infrastructure.graph.GraphProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 基于规则的实体抽取器
 *
 * @author Notebook RAG Team
 * @since 1.0.0
 */
@Slf4j
@Component
public class RuleBasedEntityExtractor implements EntityExtractor {

    @Autowired
    private GraphProperties graphProperties;

    private static final Map<String, List<Pattern>> PATTERNS = new HashMap<>();

    static {
        // 人物模式
        List<Pattern> personPatterns = new ArrayList<>();
        personPatterns.add(Pattern.compile("\\b([A-Z][a-z]+)\\s+([A-Z][a-z]+)\\b"));
        personPatterns.add(Pattern.compile("\\bMr\\.?\\s+([A-Z][a-z]+)\\b"));
        personPatterns.add(Pattern.compile("\\bDr\\.?\\s+([A-Z][a-z]+)\\b"));
        PATTERNS.put("PERSON", personPatterns);

        // 组织模式
        List<Pattern> orgPatterns = new ArrayList<>();
        orgPatterns.add(Pattern.compile("\\b([A-Z][a-zA-Z&]+)\\s+(Inc|Corp|LLC|Ltd|Company)\\.?\\b"));
        orgPatterns.add(Pattern.compile("\\b([A-Z]{2,})\\b"));
        PATTERNS.put("ORG", orgPatterns);

        // 位置模式
        List<Pattern> locPatterns = new ArrayList<>();
        locPatterns.add(Pattern.compile("\\b([A-Z][a-z]+(?:\\s+[A-Z][a-z]+)*)\\s+(City|State|Country|Province)\\b"));
        PATTERNS.put("LOC", locPatterns);

        // 数字模式
        List<Pattern> numberPatterns = new ArrayList<>();
        numberPatterns.add(Pattern.compile("\\b\\d{4}\\b")); // 年份
        numberPatterns.add(Pattern.compile("\\b\\$?\\d+(?:,\\d{3})*(?:\\.\\d{2})?\\b")); // 金额
        PATTERNS.put("NUMBER", numberPatterns);

        // 日期模式
        List<Pattern> datePatterns = new ArrayList<>();
        datePatterns.add(Pattern.compile("\\b\\d{4}-\\d{2}-\\d{2}\\b"));
        datePatterns.add(Pattern.compile("\\b\\d{2}/\\d{2}/\\d{4}\\b"));
        PATTERNS.put("DATE", datePatterns);
    }

    @Override
    public List<Entity> extract(String text, String kbId) {
        if (!graphProperties.getEntityExtraction().isEnabled()) {
            return Collections.emptyList();
        }

        List<Entity> entities = new ArrayList<>();
        Map<String, Entity> entityMap = new LinkedHashMap<>();

        for (String type : getSupportedTypes()) {
            List<Pattern> patterns = PATTERNS.get(type);

            for (Pattern pattern : patterns) {
                Matcher matcher = pattern.matcher(text);

                while (matcher.find()) {
                    String entityName = matcher.group(1);
                    String entityId = generateId(type, entityName);

                    if (!entityMap.containsKey(entityId)) {
                        Entity entity = Entity.builder()
                                .id(entityId)
                                .type(type)
                                .name(entityName)
                                .confidence(0.8)
                                .kbId(kbId)
                                .createdAt(new Date())
                                .build();

                        entityMap.put(entityId, entity);

                        if (entities.size() < graphProperties.getEntityExtraction().getMaxEntities()) {
                            entities.add(entity);
                        }
                    }
                }
            }
        }

        log.debug("抽取实体: count={}, text-length={}", entities.size(), text.length());
        return entities;
    }

    @Override
    public List<String> getSupportedTypes() {
        String config = graphProperties.getEntityExtraction().getSupportedTypes();
        String[] types = config.split(",");
        return Arrays.asList(types);
    }

    /**
     * 生成实体ID
     */
    private String generateId(String type, String name) {
        return type.toLowerCase() + ":" + name.toLowerCase().replaceAll("\\s+", "_");
    }
}
