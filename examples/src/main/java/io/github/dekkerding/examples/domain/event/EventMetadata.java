package io.github.dekkerding.examples.domain.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.UUID;

/**
 * 事件元数据值对象
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventMetadata implements Serializable {

    private static final long serialVersionUID = 1L;

    private String eventId;
    private long timestamp;
    private String eventType;
    private String sourceApplication;

    public static EventMetadata create(Class<?> eventClass, String sourceApplication) {
        return EventMetadata.builder()
                .eventId(UUID.randomUUID().toString())
                .timestamp(System.currentTimeMillis())
                .eventType(eventClass.getSimpleName())
                .sourceApplication(sourceApplication)
                .build();
    }

    public static EventMetadata create(Class<?> eventClass) {
        return create(eventClass, "unknown");
    }
}
