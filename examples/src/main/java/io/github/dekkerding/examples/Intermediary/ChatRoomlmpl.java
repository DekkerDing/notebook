package io.github.dekkerding.examples.Intermediary;

import java.util.HashMap;
import java.util.Map;

public class ChatRoomlmpl implements ChatRoom{

    private Map<String,User> usersMap=new HashMap<>();

    /**
     * @param msg
     * @param userld
     */
    @Override
    public void sendMessage(String msg, String userld) {
        User user = usersMap.get(userld);
        user.receive(msg);
    }

    /**
     * @param user
     */
    @Override
    public void addUser(User user) {
        this.usersMap.put(user.getId(),user);
    }
}