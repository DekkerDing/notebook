package io.github.dekkerding.examples.manager;

import io.github.dekkerding.examples.producer.BusinessProcessorFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.stream.StreamMessageListenerContainer;
import org.springframework.data.redis.stream.Subscription;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class DynamicStreamManager {

    // ✅ 正确的泛型声明
    @Autowired
    private StreamMessageListenerContainer<String, MapRecord<String, String, String>> container;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    // 存储当前活跃的订阅关系
    private final Map<String, Subscription> subscriptionHolder = new ConcurrentHashMap<>();

    @Autowired
    private BusinessProcessorFactory processorFactory;  // ✅ 注入工厂

    /**
     * 功能1：动态启动消费者
     */
    public void startConsumer(String streamKey, String group, String consumer) {
        String subscriptionId = buildSubscriptionId(streamKey, group, consumer);

        if (subscriptionHolder.containsKey(subscriptionId)) {
            log.warn("消费者 {} 已在运行", subscriptionId);
            return;
        }

        int maxRetries = 3;
        int retryCount = 0;

        while (retryCount < maxRetries) {
            try {
                ensureConsumerGroup(streamKey, group);

                // 使用菱形操作符简化代码
                // ✅ 创建 StreamOffset，指定 key 类型为 String
                StreamOffset<String> offset = StreamOffset.<String>create(streamKey, ReadOffset.lastConsumed());
                Subscription subscription = container.receive(offset, this::handleMessage);

                subscriptionHolder.put(subscriptionId, subscription);
                log.info("✅ 成功启动消费者: {}", subscriptionId);
                return; // 成功则退出

            } catch (Exception e) {
                retryCount++;
                log.error("启动消费者失败，重试 {}/{}: {}", retryCount, maxRetries, subscriptionId, e);

                if (retryCount >= maxRetries) {
                    log.error("消费者启动失败，已达到最大重试次数: {}", subscriptionId);
                    break;
                }

                try {
                    Thread.sleep(1000 * retryCount); // 指数退避
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
    }

    /**
     * 功能2：动态停止消费者
     */
    public void stopConsumer(String streamKey, String group, String consumer) {
        String subscriptionId = buildSubscriptionId(streamKey, group, consumer);
        Subscription subscription = subscriptionHolder.get(subscriptionId);

        if (subscription != null) {
            try {
                subscription.cancel(); // 停止订阅
                subscriptionHolder.remove(subscriptionId);
                log.info("🛑 成功停止消费者: {}", subscriptionId);
            } catch (Exception e) {
                log.error("停止消费者失败: {}", subscriptionId, e);
            }
        } else {
            log.warn("消费者 {} 不存在", subscriptionId);
        }
    }

    /**
     * 功能3：批量启动消费者（负载均衡）
     */
    public void startConsumerGroup(String streamKey, String group, int consumerCount) {
        for (int i = 1; i <= consumerCount; i++) {
            String consumer = "consumer-" + i;
            startConsumer(streamKey, group, consumer);
        }
        log.info("启动消费者组完成: {} 个消费者", consumerCount);
    }

    /**
     * 功能4：暂停/恢复消费者
     */
    public void pauseConsumer(String streamKey, String group, String consumer) {
        String subscriptionId = buildSubscriptionId(streamKey, group, consumer);
        Subscription subscription = subscriptionHolder.get(subscriptionId);

        if (subscription != null) {
            try {
                subscription.cancel(); // ✅ 暂停就是停止订阅
                subscriptionHolder.remove(subscriptionId);
                log.info("⏸️ 暂停消费者: {}", subscriptionId);
            } catch (Exception e) {
                log.error("暂停消费者失败: {}", subscriptionId, e);
            }
        } else {
            log.warn("消费者 {} 不存在，无法暂停", subscriptionId);
        }
    }

    public void resumeConsumer(String streamKey, String group, String consumer) {
        startConsumer(streamKey, group, consumer);
        log.info("▶️ 恢复消费者: {}", buildSubscriptionId(streamKey, group, consumer));
    }

    /**
     * 功能5：获取所有活跃消费者
     */
    public List<String> getActiveConsumers() {
        return new ArrayList<>(subscriptionHolder.keySet());
    }

    /**
     * 功能6：确保消费者组存在
     */
    private void ensureConsumerGroup(String streamKey, String group) {
        try {
            redisTemplate.opsForStream().createGroup(streamKey, group);
            log.info("创建消费者组: {} -> {}", streamKey, group);
        } catch (Exception e) {
            if (e.getMessage().contains("BUSYGROUP")) {
                log.info("消费者组已存在: {} -> {}", streamKey, group);
            } else {
                log.error("创建消费者组失败", e);
            }
        }
    }

    /**
     * 功能7：消息处理核心逻辑
     */
    private void handleMessage(MapRecord<String, String, String> record) {
        String streamKey = record.getStream();
        RecordId recordId = record.getId();
        Map<String, String> value = record.getValue();

        log.info("📨 收到消息 - Stream: {}, ID: {}, 内容: {}", streamKey, recordId, value);

        try {
            // 调用业务处理器
            boolean processed = processorFactory.processMessage(value);

            if (processed) {
                // 业务处理成功，确认消息
                // 注意：这里需要知道 group 名称
                String group = extractGroupFromStreamKey(streamKey);
                acknowledgeMessage(streamKey, group, recordId);
                log.info("✅ 消息处理成功并已确认: {}", recordId);
            } else {
                // 业务处理失败，进入重试逻辑
                handleRetry(record);
            }

        } catch (Exception e) {
            log.error("❌ 消息处理异常: {}", recordId, e);
            handleRetry(record);
        }
    }

    /**
     * 从 streamKey 提取 group 名称
     */
    private String extractGroupFromStreamKey(String streamKey) {
        // 假设 streamKey 格式为 "stream:group" 或直接使用默认 group
        // 这里需要根据你的实际设计来调整
        if (streamKey.contains(":")) {
            return streamKey.split(":")[1];
        }
        return "default-group";  // 默认 group
    }

    /**
     * 功能8：消息确认
     */
    private void acknowledgeMessage(String streamKey, String group, RecordId recordId) {
        redisTemplate.opsForStream().acknowledge(streamKey, group, recordId);
    }

    /**
     * 功能9：重试机制
     */
    private void handleRetry(MapRecord<String, String, String> record) {
        String retryCountKey = "retry:" + record.getId();
        String currentCount = redisTemplate.opsForValue().get(retryCountKey);
        int retryCount = currentCount == null ? 0 : Integer.parseInt(currentCount);

        if (retryCount < 3) {
            // 重试次数+1
            redisTemplate.opsForValue().set(retryCountKey, String.valueOf(retryCount + 1));
            redisTemplate.expire(retryCountKey, 1, TimeUnit.HOURS);

            // 重新投递到队列（简单实现：直接重新处理）
            log.warn("🔄 消息重试第 {} 次: {}", retryCount + 1, record.getId());
            handleMessage(record);
        } else {
            // 超过重试次数，发送到死信队列
            sendToDeadLetterQueue(record);
        }
    }

    /**
     * 功能10：死信队列
     */
    private void sendToDeadLetterQueue(MapRecord<String, String, String> record) {
        String dlqKey = "dlq:" + record.getStream();
        redisTemplate.opsForList().rightPush(dlqKey, record.getValue().toString());
        log.error("💀 消息进入死信队列: {}", record.getId());
    }

    private String buildSubscriptionId(String streamKey, String group, String consumer) {
        return streamKey + ":" + group + ":" + consumer;
    }

    /**
     * 功能11：增加健康检查
     */
    public boolean isConsumerActive(String streamKey, String group, String consumer) {
        String subscriptionId = buildSubscriptionId(streamKey, group, consumer);
        return subscriptionHolder.containsKey(subscriptionId);
    }

}