package dslab.protocols.dmap;


public interface MessageAccessProtocol {

    void login(String username, String password);
    void list();
    void show(int msgid);
    void delete(int msgid);
    void logout();
    void quit();
    void startsecure();
}
