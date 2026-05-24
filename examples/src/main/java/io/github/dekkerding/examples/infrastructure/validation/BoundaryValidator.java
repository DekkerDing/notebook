package io.github.dekkerding.examples.infrastructure.validation;

import io.github.dekkerding.examples.domain.event.EventEnvelope;
import io.github.dekkerding.examples.domain.exception.EventSerializationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * 边界条件和输入验证处理器
 * 确保系统在极端条件下的稳定性
 */
@Slf4j
@Component
public class BoundaryValidator {

    // 消息大小限制（10MB）
    private static final int MAX_MESSAGE_SIZE = 10 * 1024 * 1024;
    // 消息ID长度限制
    private static final int MAX_MESSAGE_ID_LENGTH = 256;
    // Stream Key长度限制
    private static final int MAX_STREAM_KEY_LENGTH = 512;
    // 特殊字符白名单
    private static final Pattern VALID_MESSAGE_ID_PATTERN = Pattern.compile("^[a-zA-Z0-9-_:.:@#$%]+$");

    /**
     * 验证事件信封
     */
    public ValidationResult validateEventEnvelope(EventEnvelope envelope) {
        if (envelope == null) {
            return ValidationResult.error("事件信封不能为空");
        }

        if (envelope.getMetadata() == null) {
            return ValidationResult.error("事件元数据不能为空");
        }

        // 验证消息ID
        String eventId = envelope.getMetadata().getEventId();
        ValidationResult idValidation = validateMessageId(eventId);
        if (!idValidation.isValid()) {
            return idValidation;
        }

        // 验证事件类型
        String eventType = envelope.getMetadata().getEventType();
        if (!StringUtils.hasText(eventType)) {
            return ValidationResult.error("事件类型不能为空");
        }

        // 验证payload大小
        if (envelope.getPayload() != null) {
            int payloadSize = envelope.getPayload().getBytes(StandardCharsets.UTF_8).length;
            if (payloadSize > MAX_MESSAGE_SIZE) {
                return ValidationResult.error(
                        String.format("消息大小超过限制: %d > %d", payloadSize, MAX_MESSAGE_SIZE));
            }
        }

        return ValidationResult.success();
    }

    /**
     * 验证消息ID
     */
    public ValidationResult validateMessageId(String messageId) {
        if (!StringUtils.hasText(messageId)) {
            return ValidationResult.error("消息ID不能为空");
        }

        if (messageId.length() > MAX_MESSAGE_ID_LENGTH) {
            return ValidationResult.error(
                    String.format("消息ID长度超过限制: %d > %d", messageId.length(), MAX_MESSAGE_ID_LENGTH));
        }

        // 检查特殊字符
        if (!VALID_MESSAGE_ID_PATTERN.matcher(messageId).matches()) {
            log.warn("消息ID包含特殊字符: {}", messageId);
            // 不阻止，但记录警告
        }

        return ValidationResult.success();
    }

    /**
     * 验证Stream Key
     */
    public ValidationResult validateStreamKey(String streamKey) {
        if (!StringUtils.hasText(streamKey)) {
            return ValidationResult.error("Stream Key不能为空");
        }

        if (streamKey.length() > MAX_STREAM_KEY_LENGTH) {
            return ValidationResult.error(
                    String.format("Stream Key长度超过限制: %d > %d", streamKey.length(), MAX_STREAM_KEY_LENGTH));
        }

        return ValidationResult.success();
    }

    /**
     * 验证分区配置
     */
    public ValidationResult validatePartitionConfig(int partitionCount) {
        if (partitionCount < 1) {
            return ValidationResult.error("分区数必须>=1");
        }

        if (partitionCount > 1000) {
            return ValidationResult.error("分区数不能超过1000");
        }

        return ValidationResult.success();
    }

    /**
     * 验证重试配置
     */
    public ValidationResult validateRetryConfig(int maxAttempts, long initialInterval, double multiplier) {
        if (maxAttempts < 0) {
            return ValidationResult.error("重试次数不能为负数");
        }

        if (maxAttempts > 100) {
            return ValidationResult.error("重试次数不能超过100");
        }

        if (initialInterval < 0) {
            return ValidationResult.error("初始间隔不能为负数");
        }

        if (multiplier < 1.0) {
            return ValidationResult.error("退避倍数必须>=1.0");
        }

        return ValidationResult.success();
    }

    /**
     * 验证TTL配置
     */
    public ValidationResult validateTtlConfig(long ttlSeconds) {
        if (ttlSeconds < 0) {
            return ValidationResult.error("TTL不能为负数");
        }

        if (ttlSeconds > 365 * 24 * 3600) {
            return ValidationResult.error("TTL不能超过1年");
        }

        return ValidationResult.success();
    }

    /**
     * 验证结果
     */
    public static class ValidationResult {
        private final boolean valid;
        private final String errorMessage;

        private ValidationResult(boolean valid, String errorMessage) {
            this.valid = valid;
            this.errorMessage = errorMessage;
        }

        public static ValidationResult success() {
            return new ValidationResult(true, null);
        }

        public static ValidationResult error(String message) {
            return new ValidationResult(false, message);
        }

        public boolean isValid() {
            return valid;
        }

        public String getErrorMessage() {
            return errorMessage;
        }
    }

    /**
     * 增强的异常处理
     */
    @Component
    public static class ExceptionHandler {

        /**
         * 处理序列化异常
         */
        public void handleSerializationException(Object event, Exception e) {
            log.error("序列化失败: eventClass={}, error={}",
                    event.getClass().getName(), e.getMessage());

            // 记录到监控系统
            recordErrorMetrics("serialization", event.getClass().getSimpleName());
        }

        /**
         * 处理网络异常
         */
        public void handleNetworkException(String operation, String target, Exception e) {
            log.warn("网络异常: operation={}, target={}, error={}",
                    operation, target, e.getMessage());

            // 记录到监控系统
            recordErrorMetrics("network", operation);

            // 根据操作类型决定是否重试
            if (isRetryableNetworkError(e)) {
                log.info("网络异常可重试: {}", e.getClass().getSimpleName());
            }
        }

        /**
         * 处理边界条件异常
         */
        public void handleBoundaryException(String condition, Object value, Exception e) {
            log.error("边界条件异常: condition={}, value={}, error={}",
                    condition, value, e.getMessage());

            // 记录到监控系统
            recordErrorMetrics("boundary", condition);
        }

        private boolean isRetryableNetworkError(Exception e) {
            String className = e.getClass().getName();
            return className.contains("Timeout") ||
                   className.contains("ConnectionRefused") ||
                   className.contains("ConnectionReset");
        }

        private void recordErrorMetrics(String type, String detail) {
            // 这里可以集成到监控系统（如Prometheus、Micrometer）
            // Metrics.counter("redis.stream.errors", "type", type, "detail", detail).increment();
        }
    }
}
