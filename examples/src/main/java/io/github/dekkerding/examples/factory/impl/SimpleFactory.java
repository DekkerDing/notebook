package io.github.dekkerding.examples.factory.impl;

import io.github.dekkerding.examples.factory.Factory;
import io.github.dekkerding.examples.service.Engine;
import io.github.dekkerding.examples.service.Tyre;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class SimpleFactory implements Factory {

    @Autowired
    private Engine gasolineEngine;

    @Autowired
    private Tyre rubberTyre;

    /**
     * 产生发动机
     *
     * @return
     */
    @Override
    public Engine createEngine() {
        return gasolineEngine;
    }

    /**
     * 产生轮胎
     *
     * @return
     */
    @Override
    public Tyre createTyre() {
        return rubberTyre;
    }
}
