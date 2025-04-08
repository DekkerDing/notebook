package io.github.dekkerding.examples.Intermediary;

public interface ChatRoom {
    void sendMessage(String msg,String userld);
    void addUser(User user);
}