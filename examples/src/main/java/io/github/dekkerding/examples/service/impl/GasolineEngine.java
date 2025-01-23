package io.github.dekkerding.examples.service.impl;

import io.github.dekkerding.examples.service.Engine;
import org.springframework.stereotype.Service;

@Service
public class GasolineEngine implements Engine {

    /**
     * 引擎调节
     */
    @Override
    public void adjust() {
        System.out.println("该引擎烧汽油");
    }
}
