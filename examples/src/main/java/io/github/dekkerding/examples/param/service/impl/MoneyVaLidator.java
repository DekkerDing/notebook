package io.github.dekkerding.examples.param.service.impl;

import io.github.dekkerding.examples.param.domain.request.TransferRequest;
import io.github.dekkerding.examples.param.service.ParamValidator;
import org.springframework.stereotype.Component;

/**
 * 金额校验
 */
@Component
public class MoneyVaLidator implements ParamValidator {
    @Override
    public void checkParams(TransferRequest request) {
        System.out.println("校验金额：" + request);
    }
}
