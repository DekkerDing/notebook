package io.github.dekkerding.examples.infrastructure.partition;

import lombok.extern.slf4j.Slf4j;

/**
 * 哈希分区策略
 * 根据Key的哈希值分配分区，相同Key总是路由到同一分区
 * 适用于需要保持相同事件顺序的场景
 */
@Slf4j
public class HashPartitionStrategy implements PartitionStrategy {

    @Override
    public int partition(String eventKey, int numPartitions) {
        if (numPartitions <= 0) {
            throw new IllegalArgumentException("分区数必须大于0");
        }
        if (eventKey == null || eventKey.isEmpty()) {
            log.warn("事件Key为空，使用默认分区0");
            return 0;
        }
        int partition = Math.abs(eventKey.hashCode()) % numPartitions;
        log.trace("哈希分区: key={}, hash={}, partition={}/{}",
                eventKey, eventKey.hashCode(), partition, numPartitions);
        return partition;
    }

    @Override
    public String getName() {
        return "hash";
    }
}
