package io.github.dekkerding.examples.factory;

import io.github.dekkerding.examples.service.Car;

/**
 * 抽象工厂
 */
public interface Factory {

    /**
     *  产生汽车
     * @return
     */
    public Car createCar();
}
