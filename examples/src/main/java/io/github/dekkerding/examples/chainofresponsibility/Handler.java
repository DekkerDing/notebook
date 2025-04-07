package io.github.dekkerding.examples.chainofresponsibility;

public interface Handler {
    void setNext(Handler handler);
    void handle(Request request);
}