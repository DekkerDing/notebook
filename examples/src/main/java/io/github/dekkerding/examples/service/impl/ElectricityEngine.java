package io.github.dekkerding.examples.service.impl;

import io.github.dekkerding.examples.service.Engine;
import org.springframework.stereotype.Service;

@Service
public class ElectricityEngine implements Engine {

    /**
     * 引擎调节
     */
    @Override
    public void adjust() {
        System.out.println("该引擎使用电池");
    }
}
