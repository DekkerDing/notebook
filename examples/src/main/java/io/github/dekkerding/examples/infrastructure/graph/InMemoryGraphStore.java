package io.github.dekkerding.examples.infrastructure.graph;

import io.github.dekkerding.examples.domain.graph.model.Entity;
import io.github.dekkerding.examples.domain.graph.model.Relation;
import io.github.dekkerding.examples.domain.graph.store.GraphStore;
import lombok.extern.slf4j.Slf4j;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 内存图存储实现（基于JGraphT）
 *
 * @author Notebook RAG Team
 * @since 1.0.0
 */
@Slf4j
public class InMemoryGraphStore implements GraphStore {

    private final Map<String, Entity> nodes = new HashMap<>();
    private final Map<String, Relation> edges = new HashMap<>();
    private final DefaultDirectedGraph<String, DefaultEdge> graph;

    public InMemoryGraphStore() {
        this.graph = new DefaultDirectedGraph<>(DefaultEdge.class);
    }

    @Override
    public boolean addNode(Entity entity) {
        if (entity == null || entity.getId() == null) {
            return false;
        }

        nodes.put(entity.getId(), entity);
        graph.addVertex(entity.getId());

        log.debug("添加节点: id={}, type={}", entity.getId(), entity.getType());
        return true;
    }

    @Override
    public boolean addNodes(List<Entity> entities) {
        if (entities == null || entities.isEmpty()) {
            return false;
        }

        for (Entity entity : entities) {
            addNode(entity);
        }

        return true;
    }

    @Override
    public boolean addEdge(Relation relation) {
        if (relation == null || relation.getId() == null) {
            return false;
        }

        if (!nodes.containsKey(relation.getSource()) ||
            !nodes.containsKey(relation.getTarget())) {
            log.warn("关系源或目标节点不存在: source={}, target={}",
                    relation.getSource(), relation.getTarget());
            return false;
        }

        edges.put(relation.getId(), relation);
        graph.addEdge(relation.getSource(), relation.getTarget());

        log.debug("添加关系: id={}, type={}, source={}, target={}",
                relation.getId(), relation.getType(), relation.getSource(), relation.getTarget());
        return true;
    }

    @Override
    public boolean addEdges(List<Relation> relations) {
        if (relations == null || relations.isEmpty()) {
            return false;
        }

        for (Relation relation : relations) {
            addEdge(relation);
        }

        return true;
    }

    @Override
    public Entity findNode(String id) {
        return nodes.get(id);
    }

    @Override
    public List<Entity> findNodeByName(String name) {
        return nodes.values().stream()
                .filter(node -> name.equals(node.getName()))
                .collect(Collectors.toList());
    }

    @Override
    public List<Entity> findNodeByType(String type) {
        return nodes.values().stream()
                .filter(node -> type.equals(node.getType()))
                .collect(Collectors.toList());
    }

    @Override
    public List<Relation> findRelations(String nodeId, int hops) {
        if (hops < 1 || hops > 3) {
            hops = 1;
        }

        Set<Relation> result = new HashSet<>();
        Set<String> visited = new HashSet<>();
        Queue<String> queue = new LinkedList<>();

        queue.add(nodeId);
        visited.add(nodeId);

        for (int hop = 0; hop < hops && !queue.isEmpty(); hop++) {
            int levelSize = queue.size();

            for (int i = 0; i < levelSize; i++) {
                String current = queue.poll();

                // 查找所有相关的关系
                for (Relation relation : edges.values()) {
                    if (relation.getSource().equals(current)) {
                        result.add(relation);

                        if (!visited.contains(relation.getTarget())) {
                            visited.add(relation.getTarget());
                            queue.add(relation.getTarget());
                        }
                    }
                }
            }
        }

        return new ArrayList<>(result);
    }

    @Override
    public List<Entity> findShortestPath(String sourceId, String targetId) {
        if (!nodes.containsKey(sourceId) || !nodes.containsKey(targetId)) {
            return Collections.emptyList();
        }

        try {
            // 使用JGraphT的最短路径算法
            org.jgrapht.GraphPath<String, DefaultEdge> path =
                    org.jgrapht.alg.shortestpath.DijkstraShortestPath.findPathBetween(
                            graph, sourceId, targetId);

            if (path == null) {
                return Collections.emptyList();
            }

            List<String> vertexList = path.getVertexList();
            List<Entity> result = new ArrayList<>();

            for (String vertexId : vertexList) {
                Entity entity = nodes.get(vertexId);
                if (entity != null) {
                    result.add(entity);
                }
            }

            return result;

        } catch (Exception e) {
            log.error("查找最短路径失败: source={}, target={}", sourceId, targetId, e);
            return Collections.emptyList();
        }
    }

    @Override
    public List<Entity> findCommunity(String nodeId) {
        if (!nodes.containsKey(nodeId)) {
            return Collections.emptyList();
        }

        // 简化的社区发现：返回1跳范围内的所有节点
        List<Relation> relations = findRelations(nodeId, 1);
        Set<String> communityIds = new HashSet<>();
        communityIds.add(nodeId);

        for (Relation relation : relations) {
            communityIds.add(relation.getTarget());
        }

        List<Entity> community = new ArrayList<>();
        for (String id : communityIds) {
            Entity entity = nodes.get(id);
            if (entity != null) {
                community.add(entity);
            }
        }

        return community;
    }

    @Override
    public void clear() {
        nodes.clear();
        edges.clear();
        graph.vertexSet().clear();
        graph.edgeSet().clear();

        log.info("清空内存图数据");
    }

    @Override
    public void initialize() {
        log.info("初始化内存图存储");
    }
}
