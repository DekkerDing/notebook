package io.github.dekkerding.examples.param.service;

import io.github.dekkerding.examples.param.domain.request.TransferRequest;

/**
 * 参数校验器接口
 */
public interface ParamValidator {

    void checkParams(TransferRequest request);
}
