package io.github.dekkerding.examples.domain.graph.store;

import io.github.dekkerding.examples.domain.graph.model.Entity;
import io.github.dekkerding.examples.domain.graph.model.Relation;

import java.util.List;

/**
 * 图存储接口
 *
 * @author Notebook RAG Team
 * @since 1.0.0
 */
public interface GraphStore {

    /**
     * 添加实体节点
     *
     * @param entity 实体
     * @return 操作结果
     */
    boolean addNode(Entity entity);

    /**
     * 批量添加实体节点
     *
     * @param entities 实体列表
     * @return 操作结果
     */
    boolean addNodes(List<Entity> entities);

    /**
     * 添加关系边
     *
     * @param relation 关系
     * @return 操作结果
     */
    boolean addEdge(Relation relation);

    /**
     * 批量添加关系边
     *
     * @param relations 关系列表
     * @return 操作结果
     */
    boolean addEdges(List<Relation> relations);

    /**
     * 查询节点
     *
     * @param id 节点ID
     * @return 节点信息，不存在返回null
     */
    Entity findNode(String id);

    /**
     * 根据名称查找节点
     *
     * @param name 节点名称
     * @return 节点列表
     */
    List<Entity> findNodeByName(String name);

    /**
     * 根据类型查找节点
     *
     * @param type 节点类型
     * @return 节点列表
     */
    List<Entity> findNodeByType(String type);

    /**
     * 查询节点的关系
     *
     * @param nodeId 节点ID
     * @param hops 跳数（1、2、3）
     * @return 关系列表
     */
    List<Relation> findRelations(String nodeId, int hops);

    /**
     * 查询两个节点间的最短路径
     *
     * @param sourceId 源节点ID
     * @param targetId 目标节点ID
     * @return 路径上的节点列表
     */
    List<Entity> findShortestPath(String sourceId, String targetId);

    /**
     * 发现节点所属社区
     *
     * @param nodeId 节点ID
     * @return 同一社区的所有节点
     */
    List<Entity> findCommunity(String nodeId);

    /**
     * 删除所有数据
     */
    void clear();

    /**
     * 初始化图存储
     */
    void initialize();
}
