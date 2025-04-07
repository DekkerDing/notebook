package io.github.dekkerding.examples.command;


import java.util.ArrayList;
import java.util.List;

public class output {

    private final List<Command> commands;

    public output() {
        this.commands = new ArrayList<>();
    }

    public void addCommand(Command command) {
        this.commands.add(command);
    }

    public void run(){
        commands.forEach(Command::excute);
    }

    public static void main(String[] args) {
        Receiver receiver = new ReceiverA();
        output output = new output();
        output.addCommand(new Command1(receiver));
        output.run();
    }

}