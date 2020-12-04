package dslab.protocols.dmap;

import dslab.protocols.Message;
import dslab.protocols.ProtocolException;

import java.util.List;

public interface MessageAccessProtocolServer {

    void login(String username, String password) throws ProtocolException;
    Message[] list(String username) throws ProtocolException;
    Message show(String username, int msgid) throws ProtocolException;
    void delete(String username, int msgid) throws ProtocolException;
}
