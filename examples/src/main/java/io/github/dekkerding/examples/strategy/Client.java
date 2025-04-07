package io.github.dekkerding.examples.strategy;

public class Client {
    public static void main(String[] args) {
        Promotional promotional = new Promotional(new MSpikeStrategy());
        promotional.recommand("1");

        Promotional fullpromotional = new Promotional(new FullReduceStrategy());
        fullpromotional.recommand("2");
    }
}