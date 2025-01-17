package io.github.dekkerding.examples.param.managers;

import io.github.dekkerding.examples.param.domain.request.TransferRequest;
import io.github.dekkerding.examples.param.service.ParamValidator;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

@Service
public class ValidatorManager implements InitializingBean {

    @Autowired
    private ApplicationContext applicationContext;

    private List<ParamValidator> list  = new LinkedList<>();


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
        /**
         * 获取校验实例对象
         */
        Map<String, ParamValidator> param = applicationContext.getBeansOfType(ParamValidator.class);
        list= new LinkedList<>(param.values());
    }

    /**
     * 循环 校验参数
     * @param request
     */
    public void checkParams(TransferRequest request) {
        list.forEach(validator -> {
            validator.checkParams(request);
        });
    }
}
