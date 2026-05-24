package io.github.dekkerding.examples.domain.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * 事件传输信封，封装序列化后的事件数据用于 Redis Stream 传输
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventEnvelope implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final String FIELD_EVENT_ID = "eventId";
    public static final String FIELD_TIMESTAMP = "timestamp";
    public static final String FIELD_EVENT_TYPE = "eventType";
    public static final String FIELD_SOURCE_APP = "sourceApplication";
    public static final String FIELD_EVENT_CLASS = "eventClassName";
    public static final String FIELD_PAYLOAD = "payload";

    private EventMetadata metadata;
    private String payload;
    private String eventClassName;

    public Map<String, String> toStreamRecord() {
        Map<String, String> record = new HashMap<>();
        record.put(FIELD_EVENT_ID, metadata.getEventId());
        record.put(FIELD_TIMESTAMP, String.valueOf(metadata.getTimestamp()));
        record.put(FIELD_EVENT_TYPE, metadata.getEventType());
        record.put(FIELD_SOURCE_APP, metadata.getSourceApplication());
        record.put(FIELD_EVENT_CLASS, eventClassName);
        record.put(FIELD_PAYLOAD, payload);
        return record;
    }

    public static EventEnvelope fromStreamRecord(Map<String, String> record) {
        EventMetadata metadata = EventMetadata.builder()
                .eventId(record.get(FIELD_EVENT_ID))
                .timestamp(Long.parseLong(record.getOrDefault(FIELD_TIMESTAMP, "0")))
                .eventType(record.get(FIELD_EVENT_TYPE))
                .sourceApplication(record.get(FIELD_SOURCE_APP))
                .build();

        return EventEnvelope.builder()
                .metadata(metadata)
                .eventClassName(record.get(FIELD_EVENT_CLASS))
                .payload(record.get(FIELD_PAYLOAD))
                .build();
    }

    /**
     * 重置对象状态（对象池复用时调用）
     */
    public void reset() {
        this.metadata = null;
        this.payload = null;
        this.eventClassName = null;
    }

    /**
     * 销毁对象（对象池移除时调用）
     */
    public void destroy() {
        this.metadata = null;
        this.payload = null;
        this.eventClassName = null;
    }
}
