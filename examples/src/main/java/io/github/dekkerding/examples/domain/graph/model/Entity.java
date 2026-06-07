package io.github.dekkerding.examples.domain.graph.model;

import lombok.Builder;
import lombok.Data;

import java.util.Date;
import java.util.Map;

/**
 * 图实体节点
 *
 * @author Notebook RAG Team
 * @since 1.0.0
 */
@Data
@Builder
public class Entity {

    /**
     * 实体唯一标识
     */
    private String id;

    /**
     * 实体类型（PERSON、ORG、LOC等）
     */
    private String type;

    /**
     * 实体名称
     */
    private String name;

    /**
     * 实体属性（键值对）
     */
    private Map<String, Object> properties;

    /**
     * 实体置信度（0-1）
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
