package dslab.client;

import dslab.protocols.Message;

import java.util.HashMap;

public interface INBOXManager {

    public void updateINBOX(HashMap<Integer, Message> inbox);
    public Message getMsg(int id);
    public void removeMsg(int id);
    public String getLogin();
}
