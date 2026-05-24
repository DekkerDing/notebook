package io.github.dekkerding.examples.domain.exception;

/**
 * 事件分发异常
 */
public class EventDispatchException extends RuntimeException {

    public EventDispatchException(String message) {
        super(message);
    }

    public EventDispatchException(String message, Throwable cause) {
        super(message, cause);
    }
}
