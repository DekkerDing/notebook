package io.github.dekkerding.examples.adapter.service;

/**
 *  通知服务接口
 */
public interface NotifyService {

    /**
     *  发通知
     * @param userId
     * @param content
     */
    void notifyMessage(String userId, String content);
}
