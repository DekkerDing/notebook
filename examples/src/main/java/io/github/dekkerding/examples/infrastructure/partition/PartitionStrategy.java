package io.github.dekkerding.examples.infrastructure.partition;

/**
 * 分区策略接口，用于决定事件发布到哪个分区
 * 类似Kafka的分区器功能
 */
public interface PartitionStrategy {

    /**
     * 根据事件Key计算分区号
     *
     * @param eventKey      事件Key（通常是事件ID或业务Key）
     * @param numPartitions 分区总数
     * @return 分区号，范围 [0, numPartitions-1]
     */
    int partition(String eventKey, int numPartitions);

    /**
     * 获取策略名称
     */
    String getName();
}
