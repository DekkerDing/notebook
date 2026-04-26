package io.github.dekkerding.examples.producer;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class BusinessProcessorFactory {

    /**
     * ✅ 关键：使用 @Autowired 自动注入所有 BusinessProcessor 实现
     * Spring 会自动将所有实现了 BusinessProcessor 接口的 Bean 注入到这里
     * 注意：这里使用 required=false，避免因为没有处理器而启动失败
     */
    private List<BusinessProcessor> processors;

    /**
     * 处理器映射表（类型 -> 处理器实例）
     */
    private final Map<String, BusinessProcessor> processorMap = new ConcurrentHashMap<>();

    @Autowired(required = false)
    public BusinessProcessorFactory(List<BusinessProcessor> processors) {
        this.processors = processors != null ? processors : new ArrayList<>();
    }

    /**
     * ✅ 使用 @PostConstruct 初始化处理器映射
     */
    @PostConstruct
    public void init() {
        if (processors == null || processors.isEmpty()) {
            log.warn("未发现任何 BusinessProcessor 实现");
            return;
        }

        for (BusinessProcessor processor : processors) {
            String messageType = processor.getSupportedMessageType();
            if (StringUtils.isBlank(messageType)) {
                log.error("处理器 {} 未指定支持的消息类型", processor.getClass().getName());
                continue;
            }

            if (processorMap.containsKey(messageType)) {
                log.error("发现重复的消息类型处理器: {}，处理器: {}",
                        messageType, processor.getClass().getName());
                continue;
            }

            processorMap.put(messageType, processor);
            log.info("注册业务处理器: {} -> {}", messageType, processor.getClass().getName());
        }

        log.info("业务处理器初始化完成，共加载 {} 个处理器", processorMap.size());
    }

    /**
     * 根据消息类型获取对应的处理器
     */
    public BusinessProcessor getProcessor(String messageType) {
        return processorMap.get(messageType);
    }

    /**
     * 处理消息的统一入口
     */
    public boolean processMessage(Map<String, String> message) {
        String messageType = message.get("type");
        if (StringUtils.isBlank(messageType)) {
            log.error("消息类型为空: {}", message);
            return false;
        }

        BusinessProcessor processor = getProcessor(messageType);
        if (processor == null) {
            log.error("未找到消息类型为 {} 的处理器", messageType);
            return false;
        }

        return processor.process(message);
    }

    /**
     * 获取所有支持的消息类型
     */
    public Set<String> getSupportedMessageTypes() {
        return processorMap.keySet();
    }
}