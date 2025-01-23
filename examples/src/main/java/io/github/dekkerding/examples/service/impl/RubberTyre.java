package io.github.dekkerding.examples.service.impl;

import io.github.dekkerding.examples.service.Tyre;
import org.springframework.stereotype.Service;

@Service
public class RubberTyre implements Tyre {

    /**
     * 轮胎旋转
     */
    @Override
    public void rotate() {
        System.out.println("橡胶轮胎");
    }
}
