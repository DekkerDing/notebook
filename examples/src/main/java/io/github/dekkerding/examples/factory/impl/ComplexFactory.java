package io.github.dekkerding.examples.factory.impl;

import io.github.dekkerding.examples.factory.Factory;
import io.github.dekkerding.examples.service.Engine;
import io.github.dekkerding.examples.service.Tyre;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ComplexFactory implements Factory {

    @Autowired
    private Engine electricityEngine;

    @Autowired
    private Tyre otherTyre;

    /**
     * 产生发动机
     *
     * @return
     */
    @Override
    public Engine createEngine() {
        return electricityEngine;
    }

    /**
     * 产生轮胎
     *
     * @return
     */
    @Override
    public Tyre createTyre() {
        return otherTyre;
    }
}
