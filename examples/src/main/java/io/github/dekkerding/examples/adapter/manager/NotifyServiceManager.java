package io.github.dekkerding.examples.adapter.manager;

import io.github.dekkerding.examples.adapter.service.NotifyService;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class NotifyServiceManager implements InitializingBean {

    @Autowired
    private NotifyService  mailNotifyServiceImpl;
    @Autowired
    private NotifyService  smsNotifyServiceImpl;

    private final Map<Integer, NotifyService> notifyServiceMap = new LinkedHashMap<>();

    /**
     * Invoked by the containing {@code BeanFactory} after it has set all bean properties
     * and satisfied {@link BeanFactoryAware}, {@code ApplicationContextAware} etc.
     * <p>This method allows the bean instance to perform validation of its overall
     * configuration and final initialization when all bean properties have been set.
     *
     * @throws Exception in the event of misconfiguration (such as failure to set an
     *                   essential property) or if initialization fails for any other reason
     */
    @Override
    public void afterPropertiesSet() throws Exception {
        notifyServiceMap.put(1, mailNotifyServiceImpl);
        notifyServiceMap.put(2, smsNotifyServiceImpl);
    }

    public void notify(Integer i, String userId, String content){
        NotifyService notifyService = notifyServiceMap.get(i);
        notifyService.notifyMessage(userId, content);
    }
}
