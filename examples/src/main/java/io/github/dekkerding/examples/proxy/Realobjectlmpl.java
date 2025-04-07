package io.github.dekkerding.examples.proxy;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Realobjectlmpl implements Realobject{
    /**
     *
     */
    @Override
    public void doSomething() {
        log.info("Realobjectlmpl.doSomething()");
    }
}