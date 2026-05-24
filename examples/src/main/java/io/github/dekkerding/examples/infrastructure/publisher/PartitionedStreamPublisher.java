package io.github.dekkerding.examples.infrastructure.publisher;

import io.github.dekkerding.examples.domain.event.EventEnvelope;
import io.github.dekkerding.examples.infrastructure.partition.PartitionManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.connection.stream.StreamRecords;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.Map;

/**
 * 分区Stream发布器
 * 支持按策略将事件发布到不同分区的Stream
 */
@Slf4j
public class PartitionedStreamPublisher {

    private final RedisTemplate<String, String> redisTemplate;
    private final PartitionManager partitionManager;

    public PartitionedStreamPublisher(
            RedisTemplate<String, String> redisTemplate,
            PartitionManager partitionManager) {
        this.redisTemplate = redisTemplate;
        this.partitionManager = partitionManager;
    }

    /**
     * 发布事件到分区Stream
     *
     * @param envelope 事件信封
     * @return 分区号和消息ID
     */
    public PartitionedMessageId publish(EventEnvelope envelope) {
        String eventType = envelope.getMetadata().getEventType();
        String eventKey = envelope.getMetadata().getEventId();

        partitionManager.ensurePartitions(eventType);

        int partition = partitionManager.getPartition(eventKey, eventType);
        String streamKey = partitionManager.getPartitionStreamKey(eventType, partition);

        Map<String, String> record = envelope.toStreamRecord();
        RecordId recordId = redisTemplate.opsForStream()
                .add(StreamRecords.newRecord()
                        .in(streamKey)
                        .ofMap(record));

        log.info("📤 事件已发布到分区Stream - type: {}, partition: {}, stream: {}, recordId: {}",
                eventType, partition, streamKey, recordId);

        return new PartitionedMessageId(partition, streamKey, recordId.getValue());
    }

    /**
     * 分区消息ID，包含分区信息
     */
    public static class PartitionedMessageId {
        private final int partition;
        private final String streamKey;
        private final String recordId;

        public PartitionedMessageId(int partition, String streamKey, String recordId) {
            this.partition = partition;
            this.streamKey = streamKey;
            this.recordId = recordId;
        }

        public int getPartition() {
            return partition;
        }

        public String getStreamKey() {
            return streamKey;
        }

        public String getRecordId() {
            return recordId;
        }

        @Override
        public String toString() {
            return String.format("PartitionedMessageId{partition=%d, stream='%s', recordId='%s'}",
                    partition, streamKey, recordId);
        }
    }
}
