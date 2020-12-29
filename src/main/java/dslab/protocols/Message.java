package dslab.protocols;

import java.util.List;

public interface Message {

    Integer getID();
    void setID(int id);

    String getFrom();
    void setFrom(String from);

    String getSubject();
    void setSubject(String subject);

    String getTo();
    void setTo(String to);

    String getData();
    void setData(String data);

    String getHash();
    void setHash(String hash);

    Message clone();
    boolean validate();

    List<String> generateCommands();

}
