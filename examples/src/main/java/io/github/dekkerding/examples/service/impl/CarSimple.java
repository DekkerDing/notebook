package io.github.dekkerding.examples.service.impl;

import io.github.dekkerding.examples.service.Car;
import org.springframework.stereotype.Service;

@Service
public class CarSimple implements Car {

    /**
     * 产品行为
     */
    @Override
    public void run() {
        System.out.println("CarSimple.run()");
    }
}
