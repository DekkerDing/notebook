package io.github.dekkerding.examples.adapter.service.impl;

import io.github.dekkerding.examples.adapter.client.MailClient;
import io.github.dekkerding.examples.adapter.service.NotifyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class SmsNotifyServiceImpl implements NotifyService {

    @Autowired
    private MailClient mailClient;
    /**
     * 发通知
     *
     * @param userId
     * @param content
     */
    @Override
    public void notifyMessage(String userId, String content) {
        mailClient.send(userId,content);
    }
}
