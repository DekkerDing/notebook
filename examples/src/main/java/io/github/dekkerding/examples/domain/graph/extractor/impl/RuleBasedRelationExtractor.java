package io.github.dekkerding.examples.domain.graph.extractor.impl;

import io.github.dekkerding.examples.domain.graph.extractor.RelationExtractor;
import io.github.dekkerding.examples.domain.graph.model.Entity;
import io.github.dekkerding.examples.domain.graph.model.Relation;
import io.github.dekkerding.examples.infrastructure.graph.GraphProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 基于规则的关系抽取器
 *
 * @author Notebook RAG Team
 * @since 1.0.0
 */
@Slf4j
@Component
public class RuleBasedRelationExtractor implements RelationExtractor {

    @Autowired
    private GraphProperties graphProperties;

    private static final Map<String, Pattern> PATTERNS = new HashMap<>();

    static {
        // WORKS_FOR关系
        PATTERNS.put("WORKS_FOR",
                Pattern.compile("\\b([A-Z][a-z]+\\s+[A-Z][a-z]+)\\s+(?:works? for|is an? employee of|joined)\\s+([A-Z][A-Za-z&]+(?:\\s+Inc|Corp|LLC|Ltd|Company)\\.?)\\b"));

        // PART_OF关系
        PATTERNS.put("PART_OF",
                Pattern.compile("\\b([A-Z][a-z]+)\\s+(?:is part of|belongs to)\\s+([A-Z][a-z]+(?:\\s+[A-Z][a-z]+)*)\\b"));

        // LOCATED_IN关系
        PATTERNS.put("LOCATED_IN",
                Pattern.compile("\\b([A-Z][a-z]+(?:\\s+[A-Z][a-z]+)*)\\s+(?:is located in|is based in|in)\\s+([A-Z][a-z]+(?:\\s+[A-Z][a-z]+)*)\\b"));
    }

    @Override
    public List<Relation> extract(String text, List<Entity> entities, String kbId) {
        if (!graphProperties.getRelationExtraction().isEnabled() || entities.isEmpty()) {
            return Collections.emptyList();
        }

        List<Relation> relations = new ArrayList<>();
        Map<String, Entity> entityMap = buildEntityMap(entities);

        for (String relationType : getSupportedTypes()) {
            Pattern pattern = PATTERNS.get(relationType);

            if (pattern != null) {
                Matcher matcher = pattern.matcher(text);

                while (matcher.find()) {
                    String sourceName = matcher.group(1);
                    String targetName = matcher.group(2);

                    Entity source = findEntity(entityMap, sourceName);
                    Entity target = findEntity(entityMap, targetName);

                    if (source != null && target != null) {
                        String relationId = generateRelationId(source.getId(), target.getId(), relationType);

                        Relation relation = Relation.builder()
                                .id(relationId)
                                .source(source.getId())
                                .target(target.getId())
                                .type(relationType)
                                .confidence(0.7)
                                .kbId(kbId)
                                .createdAt(new Date())
                                .build();

                        if (relations.size() < graphProperties.getRelationExtraction().getMaxRelations()) {
                            relations.add(relation);
                        }
                    }
                }
            }
        }

        log.debug("抽取关系: count={}, text-length={}", relations.size(), text.length());
        return relations;
    }

    @Override
    public List<String> getSupportedTypes() {
        return new ArrayList<>(PATTERNS.keySet());
    }

    /**
     * 构建实体名称到实体的映射
     */
    private Map<String, Entity> buildEntityMap(List<Entity> entities) {
        Map<String, Entity> map = new HashMap<>();
        for (Entity entity : entities) {
            map.put(entity.getName().toLowerCase(), entity);
            map.put(entity.getId(), entity);
        }
        return map;
    }

    /**
     * 查找实体
     */
    private Entity findEntity(Map<String, Entity> entityMap, String name) {
        return entityMap.get(name.toLowerCase());
    }

    /**
     * 生成关系ID
     */
    private String generateRelationId(String sourceId, String targetId, String relationType) {
        return relationType.toLowerCase() + ":" + sourceId + "->" + targetId;
    }
}
