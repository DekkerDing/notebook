package io.github.dekkerding.examples.factory.impl;

import io.github.dekkerding.examples.factory.Factory;
import io.github.dekkerding.examples.service.Car;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ComplexFactory implements Factory {

    @Autowired
    private Car carComplex;

    /**
     * 产生汽车
     *
     * @return
     */
    @Override
    public Car createCar() {
        return carComplex;
    }
}
