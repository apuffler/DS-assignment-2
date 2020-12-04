package dslab.protocols.dmtp;

import dslab.protocols.Message;
import dslab.protocols.ProtocolException;

public interface MessageTransferProtocolServer {

    int checkAddresses(String addresses) throws ProtocolException;
    void send(Message m) throws ProtocolException;
}
