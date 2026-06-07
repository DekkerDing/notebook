package io.github.dekkerding.examples.domain.knowledgebase.repository;

import io.github.dekkerding.examples.domain.knowledgebase.entity.KbKnowledgePoint;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 知识点Repository
 *
 * @author Notebook RAG Team
 * @since 1.0.0
 */
@Repository
public interface KbKnowledgePointRepository
        extends JpaRepository<KbKnowledgePoint, String>,
                JpaSpecificationExecutor<KbKnowledgePoint> {

    /**
     * 根据知识库ID查找知识点
     * @param kbId 知识库ID
     * @return 知识点列表
     */
    List<KbKnowledgePoint> findByKbId(String kbId);

    /**
     * 根据知识库ID和状态查找知识点
     * @param kbId 知识库ID
     * @param status 状态
     * @return 知识点列表
     */
    List<KbKnowledgePoint> findByKbIdAndStatus(String kbId, String status);

    /**
     * 根据知识库ID分页查找知识点
     * @param kbId 知识库ID
     * @param pageable 分页参数
     * @return 知识点分页
     */
    Page<KbKnowledgePoint> findByKbId(String kbId, Pageable pageable);

    /**
     * 根据分类查找知识点
     * @param category 分类
     * @return 知识点列表
     */
    List<KbKnowledgePoint> findByCategory(String category);

    /**
     * 根据知识库ID和分类查找知识点
     * @param kbId 知识库ID
     * @param category 分类
     * @return 知识点列表
     */
    List<KbKnowledgePoint> findByKbIdAndCategory(String kbId, String category);

    /**
     * 根据难度查找知识点
     * @param difficulty 难度
     * @return 知识点列表
     */
    List<KbKnowledgePoint> findByDifficulty(String difficulty);

    /**
     * 根据标签查找知识点（模糊匹配）
     * @param tag 标签
     * @return 知识点列表
     */
    List<KbKnowledgePoint> findByTagsContaining(String tag);

    /**
     * 根据关键词查找知识点（模糊匹配）
     * @param keyword 关键词
     * @return 知识点列表
     */
    List<KbKnowledgePoint> findByKeywordsContaining(String keyword);

    /**
     * 根据标题或内容搜索知识点
     * @param kbId 知识库ID
     * @param keyword 关键词
     * @return 知识点列表
     */
    @Query("SELECT kp FROM KbKnowledgePoint kp WHERE kp.kbId = :kbId " +
           "AND (kp.title LIKE %:keyword% OR kp.content LIKE %:keyword%)")
    List<KbKnowledgePoint> searchByKbIdAndKeyword(
            @Param("kbId") String kbId,
            @Param("keyword") String keyword
    );

    /**
     * 根据创建者查找知识点
     * @param createdBy 创建者
     * @return 知识点列表
     */
    List<KbKnowledgePoint> findByCreatedBy(String createdBy);

    /**
     * 统计知识库的知识点数量
     * @param kbId 知识库ID
     * @return 数量
     */
    long countByKbId(String kbId);

    /**
     * 按查看次数排序查找知识点
     * @param kbId 知识库ID
     * @param pageable 分页参数
     * @return 知识点分页
     */
    Page<KbKnowledgePoint> findByKbIdOrderByViewCountDesc(String kbId, Pageable pageable);

    /**
     * 按点赞次数排序查找知识点
     * @param kbId 知识库ID
     * @param pageable 分页参数
     * @return 知识点分页
     */
    Page<KbKnowledgePoint> findByKbIdOrderByLikeCountDesc(String kbId, Pageable pageable);

    /**
     * 根据知识库ID删除所有知识点
     * @param kbId 知识库ID
     */
    void deleteByKbId(String kbId);
}
