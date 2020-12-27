package dslab.client.connection;

import java.io.IOException;
import java.io.PrintStream;

public interface Connection{

    public void start() throws IOException;

    public String readCon();
    public void writeCon(String msg);

    public PrintStream console();

    public void stop();
}
