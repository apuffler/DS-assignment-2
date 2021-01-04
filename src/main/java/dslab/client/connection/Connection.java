package dslab.client.connection;

import dslab.client.util.EncryptionManager;

import java.io.IOException;
import java.io.PrintStream;

public interface Connection{

    public void start() throws IOException;

    public String readCon();
    public EncryptionManager getEncryptionManager();
    public void writeCon(String msg);

    public PrintStream console();

    public void stop();
}
