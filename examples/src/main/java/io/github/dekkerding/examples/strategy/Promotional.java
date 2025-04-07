package io.github.dekkerding.examples.strategy;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Promotional {

    private final PromotionStrategy strategy;

    public Promotional(PromotionStrategy strategy) {
        this.strategy = strategy;
    }

    public void recommand(String skuld) {
        strategy.recommand(skuld);
    }
}