package io.github.dekkerding.examples.factory;


import io.github.dekkerding.examples.service.Engine;
import io.github.dekkerding.examples.service.Tyre;

/**
 * 抽象工厂
 */
public interface Factory {

    /**
     *  产生发动机
     * @return
     */
    public Engine createEngine();

    /**
     * 产生轮胎
     * @return
     */
    public Tyre createTyre();
}
