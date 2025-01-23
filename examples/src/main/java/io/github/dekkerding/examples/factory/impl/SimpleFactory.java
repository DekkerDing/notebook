package io.github.dekkerding.examples.factory.impl;

import io.github.dekkerding.examples.factory.Factory;
import io.github.dekkerding.examples.service.Car;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class SimpleFactory implements Factory {

    @Autowired
    private Car carSimple;

    /**
     * 产生汽车
     *
     * @return
     */
    @Override
    public Car createCar() {
        return carSimple;
    }
}
