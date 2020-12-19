package dslab.client.commands;

import dslab.client.INBOXManager;
import dslab.client.connection.Connection;

public class VerifyCommand implements Command{
    private Connection con;
    private INBOXManager manager;

    private int verify_id;

    public VerifyCommand(int verify_id){
        this.verify_id = verify_id;
    }

    @Override
    public void execute(Connection con, INBOXManager manager) {
        this.con = con;
        this.manager = manager;
        this.run();
    }

    @Override
    public void run() {
        this.con.console().println("NOT Implemented!");
    }
}
