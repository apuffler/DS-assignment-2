package dslab.basic;

import java.io.PrintStream;
import java.net.Socket;

public interface BasicServer extends Runnable{

    void addClient(String name,Socket socket);
    void removeClient(ThreadedCommunication socket);
    String getName();
    PrintStream console();
}
