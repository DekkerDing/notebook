package io.github.dekkerding.examples.command;

public class Command1 implements Command{

    private final Receiver receiver;

    public Command1(Receiver receiver) {
        this.receiver = receiver;
    }

    @Override
    public void excute() {
        receiver.operationA();
    }
}