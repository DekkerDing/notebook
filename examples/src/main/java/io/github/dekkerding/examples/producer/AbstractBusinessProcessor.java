package io.github.dekkerding.examples.producer;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.Map;

/**
 * 抽象业务处理器基类
 */
@Slf4j
public abstract class AbstractBusinessProcessor implements BusinessProcessor{
    @Override
    public boolean process(Map<String, String> message) {
        try {
            // 1. 参数校验
            if (!validate(message)) {
                log.error("消息参数校验失败: {}", message);
                return false;
            }

            // 2. 获取消息类型
            String messageType = message.get("type");
            if (StringUtils.isBlank(messageType)) {
                log.error("消息类型为空: {}", message);
                return false;
            }

            // 3. 执行业务逻辑
            return doProcess(message);

        } catch (Exception e) {
            log.error("业务处理异常: {}", message, e);
            return false;
        }
    }

    /**
     * 参数校验（子类可重写）
     */
    protected boolean validate(Map<String, String> message) {
        return message != null && !message.isEmpty();
    }

    /**
     * 具体业务处理逻辑（子类必须实现）
     */
    protected abstract boolean doProcess(Map<String, String> message);
}