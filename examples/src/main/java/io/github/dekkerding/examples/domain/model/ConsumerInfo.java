package io.github.dekkerding.examples.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 消费者身份值对象
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConsumerInfo {

    private String streamKey;
    private String groupName;
    private String consumerName;

    public String toSubscriptionId() {
        return streamKey + ":" + groupName + ":" + consumerName;
    }

    public static ConsumerInfo of(String streamKey, String groupName, String consumerName) {
        return ConsumerInfo.builder()
                .streamKey(streamKey)
                .groupName(groupName)
                .consumerName(consumerName)
                .build();
    }
}
