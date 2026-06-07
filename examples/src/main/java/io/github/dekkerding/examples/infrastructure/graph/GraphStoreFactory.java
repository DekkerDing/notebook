package io.github.dekkerding.examples.infrastructure.graph;

import io.github.dekkerding.examples.domain.graph.store.GraphStore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 图存储工厂
 *
 * @author Notebook RAG Team
 * @since 1.0.0
 */
@Slf4j
@Component
public class GraphStoreFactory {

    @Autowired
    private GraphProperties graphProperties;

    private GraphStore graphStore;

    /**
     * 获取图存储实例
     *
     * @return 图存储实例
     */
    public synchronized GraphStore getGraphStore() {
        if (graphStore == null) {
            graphStore = createGraphStore();
            graphStore.initialize();
        }

        return graphStore;
    }

    /**
     * 创建图存储实例
     *
     * @return 图存储实例
     */
    private GraphStore createGraphStore() {
        switch (graphProperties.getType()) {
            case NEO4J:
                log.info("使用Neo4j图存储");
                // TODO: 创建Neo4j图存储实例
                // return new Neo4jGraphStore(graphProperties);
                throw new UnsupportedOperationException("Neo4j图存储暂未实现");

            case MEMORY:
            default:
                log.info("使用内存图存储");
                return new InMemoryGraphStore();
        }
    }

    /**
     * 重置图存储（用于测试）
     */
    public void reset() {
        if (graphStore != null) {
            graphStore.clear();
            graphStore = null;
        }
    }
}
