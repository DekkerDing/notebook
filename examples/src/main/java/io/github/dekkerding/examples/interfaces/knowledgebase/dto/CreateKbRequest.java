package io.github.dekkerding.examples.interfaces.knowledgebase.dto;

import io.github.dekkerding.examples.domain.knowledgebase.entity.KbKnowledgeBase;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/**
 * 创建知识库请求
 *
 * @author Notebook RAG Team
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateKbRequest {

    /**
     * 知识库名称
     */
    @NotBlank(message = "知识库名称不能为空")
    @Size(min = 1, max = 255, message = "知识库名称长度必须在1-255之间")
    private String name;

    /**
     * 知识库描述
     */
    private String description;

    /**
     * 知识库图标
     */
    private String icon;

    /**
     * 分类路径
     */
    private String categoryPath;

    /**
     * 语言
     */
    private String language;

    /**
     * 所有者
     */
    private String owner;

    /**
     * 排序顺序
     */
    private Integer sortOrder;
}
