package dslab.client.commands;

import dslab.client.INBOXManager;
import dslab.client.connection.Connection;

public class LogoutCommand implements Command{

    private Connection con;
    private INBOXManager manager;

    @Override
    public void execute(Connection con, INBOXManager manager) {
        this.con = con;
        this.manager = manager;
        this.run();
    }

    @Override
    public void run() {
        this.con.writeCon("logout");

        String line = this.con.readCon();
        if(!"ok".equals(line)){
            this.con.console().println("LogoutCommand: " + line);
        }
    }
}
