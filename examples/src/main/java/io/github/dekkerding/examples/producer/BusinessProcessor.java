package io.github.dekkerding.examples.producer;

import java.util.Map;

/**
 * 业务处理器接口
 */
public interface BusinessProcessor {
    /**
     * 处理消息
     * @param message 消息内容
     * @return 处理结果（true=成功，false=失败）
     */
    boolean process(Map<String, String> message);

    /**
     * 获取支持的消息类型
     */
    String getSupportedMessageType();

}