package dslab.protocols.dmtp;

import java.util.List;

public interface MessageTransferProtocol {

    void begin();
    void to(String address);
    void from(String address);
    void subject(String subject);
    void data(String data);
    void hash(String hash);
    void send();
    void quit();
}
