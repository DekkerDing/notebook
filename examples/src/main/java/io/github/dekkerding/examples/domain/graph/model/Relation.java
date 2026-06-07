package io.github.dekkerding.examples.domain.graph.model;

import lombok.Builder;
import lombok.Data;

import java.util.Date;
import java.util.Map;

/**
 * 图关系边
 *
 * @author Notebook RAG Team
 * @since 1.0.0
 */
@Data
@Builder
public class Relation {

    /**
     * 关系唯一标识
     */
    private String id;

    /**
     * 源实体ID
     */
    private String source;

    /**
     * 目标实体ID
     */
    private String target;

    /**
     * 关系类型（PART_OF、WORKS_FOR等）
     */
    private String type;

    /**
     * 关系属性
     */
    private Map<String, Object> properties;

    /**
     * 关系置信度（0-1）
     */
    private Double confidence;

    /**
     * 创建时间
     */
    private Date createdAt;

    /**
     * 所属知识库ID
     */
    private String kbId;

    /**
     * 添加属性
     */
    public void addProperty(String key, Object value) {
        if (this.properties == null) {
            this.properties = new java.util.HashMap<>();
        }
        this.properties.put(key, value);
    }

    /**
     * 获取属性
     */
    public Object getProperty(String key) {
        if (this.properties == null) {
            return null;
        }
        return this.properties.get(key);
    }
}
