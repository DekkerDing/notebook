package io.github.dekkerding.examples.domain.knowledgebase.repository;

import io.github.dekkerding.examples.domain.knowledgebase.entity.KbKnowledgeBase;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 知识库Repository
 *
 * @author Notebook RAG Team
 * @since 1.0.0
 */
@Repository
public interface KbKnowledgeBaseRepository
        extends JpaRepository<KbKnowledgeBase, String>,
                JpaSpecificationExecutor<KbKnowledgeBase> {

    /**
     * 根据状态查找知识库
     * @param status 状态
     * @return 知识库列表
     */
    List<KbKnowledgeBase> findByStatus(String status);

    /**
     * 根据状态查找知识库（按排序顺序）
     * @param status 状态
     * @return 知识库列表
     */
    List<KbKnowledgeBase> findByStatusOrderBySortOrderAsc(String status);

    /**
     * 根据所有者查找知识库
     * @param owner 所有者
     * @return 知识库列表
     */
    List<KbKnowledgeBase> findByOwner(String owner);

    /**
     * 根据分类路径前缀查找知识库
     * @param categoryPath 分类路径前缀
     * @return 知识库列表
     */
    List<KbKnowledgeBase> findByCategoryPathStartingWith(String categoryPath);

    /**
     * 根据名称查找知识库
     * @param name 名称
     * @return 知识库
     */
    Optional<KbKnowledgeBase> findByName(String name);

    /**
     * 检查名称是否存在
     * @param name 名称
     * @return 是否存在
     */
    boolean existsByName(String name);
}
