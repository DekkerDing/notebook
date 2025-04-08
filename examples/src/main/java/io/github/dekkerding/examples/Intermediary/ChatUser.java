package io.github.dekkerding.examples.Intermediary;

public class ChatUser extends User{

    public ChatUser(String id, String name, ChatRoom mediator) {
        super(id, name, mediator);
    }

    /**
     * @param msg
     * @param userld
     */
    @Override
    public void send(String msg, String userld) {

    }

    /**
     * @param msg
     */
    @Override
    public void receive(String msg) {

    }
}