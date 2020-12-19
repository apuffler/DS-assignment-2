package dslab.client.commands;

import dslab.client.INBOXManager;
import dslab.client.connection.Connection;

public interface Command extends Runnable{

    public void execute(Connection con, INBOXManager manager);
}
