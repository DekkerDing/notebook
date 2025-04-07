package io.github.dekkerding.examples.command;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ReceiverA implements Receiver{
    /**
     *
     */
    @Override
    public void operationA() {
        log.info("ReceiverA.operationA()");
    }

    /**
     *
     */
    @Override
    public void operationB() {
        log.info("ReceiverB.operationB()");
    }

    /**
     *
     */
    @Override
    public void operationC() {
        log.info("ReceiverC.operationC()");
    }
}