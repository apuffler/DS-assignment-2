package dslab.client.commands;

import dslab.client.INBOXManager;
import dslab.client.connection.Connection;

public class DeleteCommand implements Command{
    private Connection con;
    private INBOXManager manager;

    private int del_id;

    public DeleteCommand(int del_id){
        this.del_id = del_id;
    }

    @Override
    public void execute(Connection con, INBOXManager manager) {
        this.con = con;
        this.manager = manager;
        this.run();
    }

    @Override
    public void run() {
        this.con.writeCon("delete " + this.del_id);

        String line = this.con.readCon();

        if("ok".equals(line)){
            this.manager.removeMsg(this.del_id);
            this.con.console().println("ok");
        }else{
            this.con.console().printf("%s: %s%n","DeleteCommand",line);
        }
    }
}
