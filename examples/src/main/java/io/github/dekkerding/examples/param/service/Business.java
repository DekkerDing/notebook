package io.github.dekkerding.examples.param.service;

import io.github.dekkerding.examples.param.domain.request.TransferRequest;
import io.github.dekkerding.examples.param.managers.ValidatorManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class Business {

    @Autowired
    private ValidatorManager manager;

    public void transfer(TransferRequest transferRequest) {
        manager.checkParams(transferRequest);
    }

}
