package io.github.dekkerding.examples.infrastructure.partition;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * 轮询分区策略
 * 按顺序轮流分配到各个分区，实现负载均衡
 */
@Slf4j
public class RoundRobinPartitionStrategy implements PartitionStrategy {

    private final AtomicInteger counter = new AtomicInteger(0);

    @Override
    public int partition(String eventKey, int numPartitions) {
        if (numPartitions <= 0) {
            throw new IllegalArgumentException("分区数必须大于0");
        }
        int partition = Math.abs(counter.getAndIncrement()) % numPartitions;
        log.trace("轮询分区: key={}, partition={}/{}, count={}",
                eventKey, partition, numPartitions, counter.get());
        return partition;
    }

    @Override
    public String getName() {
        return "round_robin";
    }
}
