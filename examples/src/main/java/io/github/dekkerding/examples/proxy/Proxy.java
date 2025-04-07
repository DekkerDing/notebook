package io.github.dekkerding.examples.proxy;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Proxy extends Realobjectlmpl{
    /**
     *
     */
    @Override
    public void doSomething() {
        log.info("Proxy doSomething");
        super.doSomething();
    }
}