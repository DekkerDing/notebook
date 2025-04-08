package io.github.dekkerding.examples.Intermediary;

import lombok.Data;

@Data
public abstract class User {

    private ChatRoom mediator;
    private String id;
    private String name;

    public User(String id, String name,ChatRoom mediator) {
        this.mediator = mediator;
        this.id = id;
        this.name = name;
    }

    public abstract void send(String msg, String userld);
    public abstract void receive(String msg);
}